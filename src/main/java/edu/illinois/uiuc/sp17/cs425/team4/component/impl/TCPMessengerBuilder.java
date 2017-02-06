package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;

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
	/** Max size of the cache that keeps open outgoing connections. */
	private Integer outgoingSocketCacheSize;
	/** Thread pool that TCP messenger should use to submit new tasks. */
	private ExecutorService threadPool;
	
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
	 * Get max size of the cache that keeps open outgoing connections.
	 * @return max size of cache.
	 */
	public Integer getOutgoingSocketCacheSize() {
		return outgoingSocketCacheSize;
	}
	
	/**
	 * Set max size of the cache that keeps open outgoing connections.
	 * @param max size of cache.
	 */
	public TCPMessengerBuilder setOutgoingSocketCacheSize(Integer outgoingSocketCacheSize) {
		this.outgoingSocketCacheSize = outgoingSocketCacheSize;
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

	@Override
	public Messenger build() throws IOException {
		return new TCPMessenger(this);
	}
}
