package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

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

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageReceiptListener;
import net.jcip.annotations.NotThreadSafe;

/**
 * A TCP Server that listens for messages in a loop and notifies everyone who has registered for it.
 * All the notifications are submitted to a thread pool where they run on dedicated threads.
 * 
 * @author bbassi2
 */
@NotThreadSafe
final class TCPServer implements Callable<Void> {
	
	/** List of message listeners. */
	private final List<MessageReceiptListener> messageListeners;
	/** TCP server. */
	private final ServerSocket tcpServer;
	/** Thread pool. */
	private final ExecutorService threadPool;
	
	/**
	 * Create an instance.
	 * @param messageListeners list of message listeners.
	 * @param tcpServer Tcp Server socket.
	 */
	public TCPServer(List<MessageReceiptListener> messageListeners, ServerSocket tcpServer, 
						ExecutorService threadPool) {
		this.messageListeners = messageListeners;
		this.tcpServer = tcpServer;
		this.threadPool = threadPool;
	}


	@Override
	public Void call() {
		try {
			List<Future<Void>> pendingListeners =  new LinkedList<Future<Void>>();
			while(true) {
				/* 
				 * Ideally, here we should check for interrupted flag to know when to 
				 * stop looping. However, ServerSocket.accpet() doesn't respect interruption, 
				 * so there's no use. To make this loop stop, we will have to close tcpServer 
				 * from other thread. This will make accept() throw SocketException.
				*/
				Socket connectionSocket = this.tcpServer.accept();
				System.out.println("Incoming connection");
				// notify listeners starting each on different thread.
				for (MessageReceiptListener listener: this.messageListeners) {
					System.out.println("First listener.");
					Callable<Void> messageListener = 
							new MessageListenerWrapper(listener,connectionSocket);
					Future<Void> future = this.threadPool.submit(messageListener);
					System.out.println("Submitted");
					// Add to list of pending listeners.
					pendingListeners.add(future);
				}
				// remove listeners from list that are already done.
				removeFinishedListeners(pendingListeners);
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
	
	/**
	 * Remove listeners from the list that are already done.
	 * If there was an error during 
	 * @param pendingListeners
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void removeFinishedListeners(List<Future<Void>> pendingListeners) throws InterruptedException, ExecutionException {
		Iterator<Future<Void>> it = pendingListeners.iterator();
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
	 * A callable wrapper for MessageListener so that it could be submitted to a thread pool.
	 *  
	 * @author bbassi2
	 */
	private static class MessageListenerWrapper implements Callable<Void> {
		/** Message listener. */
		private final MessageReceiptListener listener;
		/** Incoming socket for the tcp connection. */
		private final Socket tcpIncomingSocket;
		
		/**
		 * Create an instance.
		 * @param listener Message listener.
		 * @param receivedFrom Remote address that sent the message.
		 * @param inputStream Input stream to read the message from.
		 * @param outputStream Output stream to write the response to.
		 */
		private MessageListenerWrapper(MessageReceiptListener listener, Socket tcpIncomingSocket) {
			this.listener = listener;
			this.tcpIncomingSocket = tcpIncomingSocket;
		}
		
		@Override
		public Void call() {
			this.listener.messageReceived(this.tcpIncomingSocket);
			return null;
		}
		
	}

}
