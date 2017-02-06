package edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageReceiptListener;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import net.jcip.annotations.NotThreadSafe;

/**
 * A TCP Server that listens for messages in a loop and notifies everyone who has registered for it.
 * All the notifications are submitted to a thread pool where they run on dedicated threads.
 * 
 * @author bbassi2
 */
@NotThreadSafe
final class TCPServer implements Callable<Void> {
	
	/** Message listener. */
	private MessageReceiptListener messageListener;
	/** TCP server. */
	private final ServerSocket tcpServerSocket;
	/** Message Adaptor. */
	private final MessageAdaptor messageAdaptor;
	/** Thread pool. */
	private final ExecutorService threadPool;
	/** Identity of this process. */
	private final Process myIdentity;
	
	/**
	 * Create an instance.
	 * @param messageListeners list of message listeners.
	 * @param tcpServer Tcp Server socket.
	 * @throws IOException 
	 */
	public TCPServer(TCPMessengerBuilder builder) throws IOException {
		this.threadPool = (ExecutorService) checkForNull(builder.getThreadPool(),
				"Thread pool cannot be null.");
		
		// Get port number
		Integer port = (Integer) checkForNull(builder.getPort(), "Port number must be provided.");
		this.tcpServerSocket = createTcpServerSocket(port, builder);
		
		// Get adaptor.
		this.messageAdaptor = (MessageAdaptor) checkForNull(builder.getMessageAdaptor(), "Message Adaptor cannot be null");
		// Get Identity.
		this.myIdentity = (Process) checkForNull(builder.getMyIdentity(), "Identity cannot be null");
	}


	@Override
	public Void call() {
		try {
			List<Future<Void>> pendingMessages = new LinkedList<Future<Void>>();
			
			while(true) {
				/* 
				 * Ideally, here we should check for interrupted flag to know when to 
				 * stop looping. However, ServerSocket.accpet() doesn't respect interruption, 
				 * so there's no use. To make this loop stop, we will have to close tcpServer 
				 * from other thread. This will make accept() throw SocketException.
				*/
				Socket connectionSocket = this.tcpServerSocket.accept();
				// notify listeners starting each on different thread.
				Callable<Void> messageListenerWrapper = 
						new MessageListenerWrapper(this.messageListener,
								connectionSocket, this.messageAdaptor, this.myIdentity);
				Future<Void> future = this.threadPool.submit(messageListenerWrapper);
				// Add to list of pending listeners.
				pendingMessages.add(future);
				// remove listeners from list that are already done.
				checkFailures(pendingMessages);
			} 
		} catch (IOException e) {
			throw new ContextedRuntimeException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // so that code up stack trace is aware of interrupt.
			throw new ContextedRuntimeException(e);
		} catch (ExecutionException e) {
			throw new ContextedRuntimeException(e);
		}

	}
	
	public boolean registerListener(MessageReceiptListener listener) {
		this.messageListener = listener;
		return true;
	}
	
	/**
	 * Respond to processed responses.
	 * @param pendingResponses Responses that were pending till last check.
	 * @throws InterruptedException if interrupted.
	 * @throws ExecutionException if future had failed.
	 */
	private void checkFailures(List<Future<Void>> pendingResponses) 
			throws InterruptedException, ExecutionException {
		Iterator<Future<Void>> it = pendingResponses.iterator();
		while (it.hasNext()) {
			Future<Void> listenerFut = it.next();
			if (listenerFut.isDone()) {
				it.remove();
				// get is called so that if there was any error, it could throw that exception.
				listenerFut.get();
			}
		}
		
	}

	/**
	 * Creates a TCP server.
	 * @param port bind to the given port.
	 * @param builder Builder.
	 * @return TCP server socket.
	 * @throws IOException if cannot bind a TCP server to the given port/address.
	 */
	private ServerSocket createTcpServerSocket(Integer port, TCPMessengerBuilder builder) throws IOException {
		if (builder.getBacklog() == null && builder.getBindAddr() == null) {
			return new ServerSocket(port);
		} else if (builder.getBindAddr() == null) {
			return new ServerSocket(port, builder.getBacklog());
		} else {
			return new ServerSocket(port, builder.getBacklog(), builder.getBindAddr());
		}
	}
	
	/**
	 * Check if the given object is null.
	 * @param obj Object that has to be checked for null.
	 * @param message exception message if object is null.
	 * @return the passed object as it is if not null.
	 */
	private Object checkForNull(Object obj, String message) {
		if (obj == null) {
			throw new IllegalArgumentException(message);
		}
		return obj;
	}
	
	/**
	 * A callable wrapper for MessageListener so that it could be submitted to a thread pool.
	 *  
	 * @author bbassi2
	 */
	private static class MessageListenerWrapper implements Callable<Void> {
		/** Message listener. */
		private final MessageReceiptListener listener;
		/** Incoming socket for the tcp connection. */
		private final Socket tcpIncomingSocket;
		private final MessageAdaptor messageAdaptor;
		private final Process myIdentity;
		
		/**
		 * Create an instance.
		 * @param listener Message listener.
		 * @param receivedFrom Remote address that sent the message.
		 * @param inputStream Input stream to read the message from.
		 * @param outputStream Output stream to write the response to.
		 */
		private MessageListenerWrapper(MessageReceiptListener listener, 
				Socket tcpIncomingSocket, MessageAdaptor messageAdaptor,
				Process myIdentity) {
			this.listener = listener;
			this.tcpIncomingSocket = tcpIncomingSocket;
			this.messageAdaptor = messageAdaptor;
			this.myIdentity = myIdentity;
		}
		
		@Override
		public Void call() {
			// Read the message.
			Pair<Process,Message> srcAndMsg = 
					this.messageAdaptor.read(this.tcpIncomingSocket);
			// Process the message.
			Message response = this.listener.messageReceived(srcAndMsg);
			// Respond.
			this.messageAdaptor.write(this.tcpIncomingSocket, 
					Pair.of(this.myIdentity, response));
			return null;
		}
		
	}

}
