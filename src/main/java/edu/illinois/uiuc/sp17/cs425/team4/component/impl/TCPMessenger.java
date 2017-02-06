package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.exception.ContextedRuntimeException;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageReceiptListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * A messenger that sends and receives messages using TCP protocol.
 * Upon receipt of messages, each registered listener is submitted to a thread pool 
 * where it is run on a dedicated thread.
 * The implementation maintains a bounded cache of open Sockets and oldest entries are 
 * removed to make space for the new ones. Default max size of cache is 10.
 * 
 * @author bbassi2
 */
@ThreadSafe
final class TCPMessenger implements Messenger {
	/** TCP server that listens for incoming messages. */
	private final ServerSocket tcpServerSocket;
	/** Cache to maintain open outgoing connections. */
	private final Map<InetAddress, Socket> cache;
	/** Executor service to submit new threads to. */
	private final ExecutorService threadPool;
	/** List containing the registered listeners that should be called on message receipt. */
	private final List<MessageReceiptListener> messageReceiptListeners;
	/** Initialized? */
	@GuardedBy("this")
	private boolean initialized = false;
	/** A result bearer for the execution of tcp server. */
	private Future<Void> tcpServerFut;
	
	/**
	 * Create an instance.
	 * @param builder Builder.
	 * @throws IOException if cannot bind a TCP server to the given port/address.
	 */
	TCPMessenger(TCPMessengerBuilder builder) throws IOException {
		// Initialize thread pool.
		this.threadPool = (ExecutorService) checkForNull(builder.getThreadPool(),
				"Thread pool cannot be null.");
		
		// Get port number
		Integer port = (Integer) checkForNull(builder.getPort(), "Port number must be provided.");
		this.tcpServerSocket = createTcpServer(port, builder);
		
		// Default to 10.
		int maxCacheSize = builder.getOutgoingSocketCacheSize() == null ? 10 : builder.getOutgoingSocketCacheSize();
		this.cache = createCache(maxCacheSize);
		
		// initialize listeners list.
		this.messageReceiptListeners = new CopyOnWriteArrayList<MessageReceiptListener>(); 
	}
	
	
	@Override
	public void initialize() {
		synchronized(this) {
			if (!initialized) {
				this.initialized = true;
			} else {
				throw new ContextedRuntimeException("Messenger already initialized. Ignoring this call.");
			}
		}
		
		TCPServer tcpServer = new TCPServer(this.messageReceiptListeners, 
				this.tcpServerSocket, this.threadPool);
		this.tcpServerFut = this.threadPool.submit(tcpServer);
	}
	
	@Override
	public byte[] send(InetAddress sendTo, byte[] msg, Configuration configuration) {
		checkForFailure();
		return null;
	}

	@Override
	public boolean registerListener(MessageReceiptListener listener) {
		checkForFailure();
		this.messageReceiptListeners.add(listener);
		return true;
	}
	
	private synchronized void checkForFailure() {
		if (this.tcpServerFut != null && this.tcpServerFut.isDone()) {
			try {
				// following line will throw exception if there was an error on server.
				// Won't do anything oherwise.
				tcpServerFut.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt(); // so that code up stack trace is aware of interrupt.
				throw new ContextedRuntimeException(e);
			} catch (ExecutionException e) {
				throw new ContextedRuntimeException(e);
			}
		}
	}
	/**
	 * Creates cache to store open connections.
	 * @param maxCacheSize Max cache size.
	 * @return cache.
	 */
	private Map<InetAddress, Socket> createCache(final int maxCacheSize) {
		// LRU cache.
		Map<InetAddress, Socket> cache = new LinkedHashMap<InetAddress, Socket>(maxCacheSize, 0.75f, true) {
			private static final long serialVersionUID = 1L;
			
			protected boolean removeEldestEntry(Map.Entry<InetAddress, Socket> eldest) {
				return size() > maxCacheSize;
			}
		};
		return Collections.synchronizedMap(cache);
	}

	/**
	 * Creates a TCP server.
	 * @param port bind to the given port.
	 * @param builder Builder.
	 * @return TCP server socket.
	 * @throws IOException if cannot bind a TCP server to the given port/address.
	 */
	private ServerSocket createTcpServer(Integer port, TCPMessengerBuilder builder) throws IOException {
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
	
	
}
