package edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import net.jcip.annotations.NotThreadSafe;

/**
 * Builder for TCPMessenger.
 * Returns an instance of TCPMessenger class, see it's documentation for more info.
 * 
 * @author bbassi2
 */
@NotThreadSafe
public class TCPMessengerBuilder implements Messenger.Builder {
	/** Port on which messenger should listen to messages. Passed to underlying SocketServer. */
	private Integer port;
	/** Requested maximum length of the queue of incoming connections.
	 * Passed to underlying SocketServer.
	 */
	private Integer backlog;
	/** The local InetAddress the server will bind to. */
	private InetAddress bindAddr;
	/** Thread pool that TCP messenger should use to submit new tasks. */
	private ExecutorService threadPool;
	/** Message Adaptor. */
	private TCPMessageAdaptor messageAdaptor;
	/** Identity of this process. This will be typically used to communicate consistently with other processes.*/
	private Process myIdentity;
	
	// TODO might want to consider taking ServerSocket object from outside.
	
	/**
	 * Get port on which messenger should listen to messages.
	 * @return port.
	 */
	public Integer getPort() {
		return port;
	}
	
	/**
	 * Set port on which messenger should listen to messages. Passed to underlying SocketServer.
	 * @param port port
	 */
	public TCPMessengerBuilder setPort(Integer port) {
		this.port = port;
		return this;
	}
	
	/** 
	 * Get maximum length of the queue of incoming connections.
	 * @return max queue length.
	 */
	public Integer getBacklog() {
		return backlog;
	}
	
	/**
	 * Set requested maximum length of the queue of incoming connections.
	 * Passed to underlying SocketServer.
	 * @param backlog max queue length for incoming connections.
	 */
	public TCPMessengerBuilder setBacklog(Integer backlog) {
		this.backlog = backlog;
		return this;
	}
	
	/**
	 * The local InetAddress the server will bind to.
	 * @return bindAddr
	 */
	public InetAddress getBindAddr() {
		return bindAddr;
	}
	
	/**
	 * The local InetAddress the server will bind to. Passed to underlying SocketServer.
	 * Argument can be used on a multi-homed host for a ServerSocket that will only accept connect requests to one of its addresses.
	 * If bindAddr is null, it will default accepting connections on any/all local addresses.
	 * @param bindAddr local InetAddress the server will bind to.
	 */
	public TCPMessengerBuilder setBindAddr(InetAddress bindAddr) {
		this.bindAddr = bindAddr;
		return this;
	}
	

	/**
	 * Get thread pool.
	 * @return thread pool.
	 */
	public ExecutorService getThreadPool() {
		return threadPool;
	}

	/**
	 * Sets thread pool that should be used by TCP messenger for executing threads.
	 * @param threadPool Thread pool.
	 */
	public TCPMessengerBuilder setThreadPool(ExecutorService threadPool) {
		this.threadPool = threadPool;
		return this;
	}
	
	/**
	 * Get message adaptor. 
	 * @return message adaptor.
	 */
	public TCPMessageAdaptor getMessageAdaptor() {
		return messageAdaptor;
	}

	/**
	 * Set message adaptor that should be used by TCP messenger to convert to and from TCP
	 * messages and high-level messages.
	 * @param messageAdaptor
	 */
	public TCPMessengerBuilder setMessageAdaptor(TCPMessageAdaptor messageAdaptor) {
		this.messageAdaptor = messageAdaptor;
		return this;
	}
	
	/**
	 * Get my identity.
	 * @return my identity.
	 */
	public Process getMyIdentity() {
		return myIdentity;
	}

	/**
	 * Set my identity. This will typically be used to consistently communicate with other processes.
	 * @param myIdentity my identity.
	 */
	public TCPMessengerBuilder setMyIdentity(Process myIdentity) {
		this.myIdentity = myIdentity;
		return this;
	}

	@Override
	public Messenger build() throws IOException {
		return new TCPMessenger(this);
	}

	
}
