package edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import net.jcip.annotations.NotThreadSafe;

/**
 * A TCP Server that waits for messages in a loop 
 * and notifies registered message listener 
 * Notifications are submitted to a thread pool where it is run on dedicated thread.
 * Only supports one listener as of now.
 * 
 * @author bbassi2
 */
@NotThreadSafe
final class TCPServer implements Callable<Void> {
	private final static Logger LOG = Logger.getLogger(TCPServer.class.getName());
	/** Message listeners. */
	private List<MessageListener> messageListeners;
	/** TCP server. */
	private final ServerSocket tcpServerSocket;
	/** Message Adaptor. */
	private final TCPMessageAdaptor messageAdaptor;
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
		this.messageAdaptor = (TCPMessageAdaptor) checkForNull(builder.getMessageAdaptor(), "Message Adaptor cannot be null");
		// Get Identity.
		this.myIdentity = (Process) checkForNull(builder.getMyIdentity(), "Identity cannot be null");
	}


	@Override
	public Void call() throws Exception {
		while(true) {
			/* 
			 * Ideally, here we should check for interrupted flag to know when to 
			 * stop looping. However, ServerSocket.accpet() doesn't respect interruption, 
			 * so there's no use. To make this loop stop, we will have to close tcpServer 
			 * from other thread. This will make accept() throw SocketException.
			*/
			Socket connectionSocket = this.tcpServerSocket.accept();
			Pair<Process,Message> srcAndMsg = readMessage(connectionSocket);
			if (srcAndMsg == null) continue;
			LOG.debug(String.format("Incoming message %s arrived from %s", srcAndMsg.getRight().getUUID(), srcAndMsg.getLeft().getDisplayName()));
			ResponseWriter responseWriter = createResponseWriter(connectionSocket);
			// Listener to whom this message should be routed to.
			MessageListener listener = getListener(srcAndMsg);
			// notify listener on different thread.
			Callable<Void> messageListenerWrapper = 
					new MessageListenerWrapper(listener, srcAndMsg, responseWriter);
			this.threadPool.submit(messageListenerWrapper);
		} 
	}
	
	
	public void setMessageListeners(List<MessageListener> listeners) {
		this.messageListeners = listeners;
	}
	

	/**
	 * Creates a TCP server.
	 * @param port bind to the given port.
	 * @param builder Builder.
	 * @return TCP server socket.
	 * @throws IOException if cannot bind a TCP server to the given port/address.
	 */
	private ServerSocket createTcpServerSocket(Integer port, 
			TCPMessengerBuilder builder) throws IOException {
		ServerSocket serverSocket = new ServerSocket();
		serverSocket.setReuseAddress(true);
		if (builder.getBacklog() == null && builder.getBindAddr() == null) {
			serverSocket.bind(new InetSocketAddress(port));
			return serverSocket;
		} else if (builder.getBindAddr() == null) {
			serverSocket.bind(new InetSocketAddress(port), builder.getBacklog());
			return serverSocket;
		} else {
			serverSocket.bind(new InetSocketAddress(builder.getBindAddr(), port), builder.getBacklog());
			return serverSocket;
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
	
	private Pair<Process,Message> readMessage(Socket tcpIncomingSocket) {
		try {
			return this.messageAdaptor.read(tcpIncomingSocket);
		} catch (IOException e) {
			return null;
		}
	}
	
	private MessageListener getListener(Pair<Process,Message> srcAndMsg) {
		Message message = srcAndMsg.getRight();
		for (MessageListener listener: this.messageListeners) {
			if (listener.getIdentifier().equals(message.getMessageListenerId())){
				return listener;
			}
		}
		return null;
	}
	
	private ResponseWriter createResponseWriter(Socket tcpIncomingSocket) {
		return new TCPResponseWriter(this.messageAdaptor, 
				tcpIncomingSocket, this.myIdentity);
	}
	
	
	/**
	 * A callable wrapper for MessageListener so that it could be submitted to a thread pool.
	 *  
	 * @author bbassi2
	 */
	private static class MessageListenerWrapper implements Callable<Void> {
		private final MessageListener listener;
		private final Pair<Process,Message> srcAndMsg;
		private final ResponseWriter responseWriter;
		
		
		
		public MessageListenerWrapper(MessageListener listener, Pair<Process, Message> srcAndMsg,
				ResponseWriter responseWriter) {
			this.listener = listener;
			this.srcAndMsg = srcAndMsg;
			this.responseWriter = responseWriter;
		}



		@Override
		public Void call() {
			try {
				// Process the message.(Notify listener)
				this.listener.messageReceived(this.srcAndMsg, this.responseWriter);
			} catch (Exception e1) {
				// ignore this exception. We cannot do anything about it.
				// It came from client code and should have been handled by the client.
			}
			
			return null;
		}
	}

}
