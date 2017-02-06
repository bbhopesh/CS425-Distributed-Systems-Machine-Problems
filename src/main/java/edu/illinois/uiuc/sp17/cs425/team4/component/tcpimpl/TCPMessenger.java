package edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageReceiptListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
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
	private final TCPServer tcpServer;
	/** Cache to maintain open outgoing connections. */
	private final Map<Process, Socket> cache;
	/** Executor service to submit new threads to. */
	private final ExecutorService threadPool;
	/** Message Adaptor. */
	private final MessageAdaptor messageAdaptor;
	/** Identity of this process. */
	private final Process myIdentity;
	/** Message Listener. */
	@GuardedBy("this")
	private MessageReceiptListener messageReceiptListener;
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
		// Save Builder.
		this.tcpServer = new TCPServer(builder);
		this.threadPool = (ExecutorService) checkForNull(builder.getThreadPool(),
				"Thread pool cannot be null.");
		// Get adaptor.
		this.messageAdaptor = (MessageAdaptor) checkForNull(builder.getMessageAdaptor(), "Message Adaptor cannot be null");
		// Get Identity.
		this.myIdentity = (Process) checkForNull(builder.getMyIdentity(), "Identity cannot be null");
		// Default to 10.
		int maxCacheSize = builder.getOutgoingSocketCacheSize() == null ? 10 : builder.getOutgoingSocketCacheSize();
		this.cache = createCache(maxCacheSize);
	}
	
	
	@Override
	public void initialize() {
		synchronized(this) {
			if (!initialized) {
				checkListener();
				this.initialized = true;
			} else {
				throw new ContextedRuntimeException("Messenger already initialized. Ignoring this call.");
			}
		}
		
		this.tcpServerFut = this.threadPool.submit(this.tcpServer);
	}
	
	@Override
	public Message send(Pair<Process, Message> dstnAndMsg) {
		synchronized(this) {
			checkInitialization();
			checkForFailure();
		}
		try {
			// Opem socket with destination.
			Socket s = getSocket(dstnAndMsg.getLeft());
			// Send destination the message and tell them who sent it.
			this.messageAdaptor.write(s, Pair.of(this.myIdentity, dstnAndMsg.getRight()));
			// Read response back and ignore source of this msg as source of the response msg will be destionation of original msg.
			return this.messageAdaptor.read(s).getRight();
		} catch (IOException e) {
			throw new ContextedRuntimeException(e);
		}
	}
	
	private synchronized Socket getSocket(Process p) throws IOException {
		Socket s = this.cache.get(p);
		if (s == null) {
			s = new Socket(p.getInetAddress(), p.getPort());
			this.cache.put(p, s);
		}
		return s;
	}

	@Override
	public synchronized boolean registerListener(MessageReceiptListener listener) {
		checkForFailure();
		return checkAndSetListener(listener);
	}
	
	@GuardedBy("this")
	private void checkListener() {
		if (this.messageReceiptListener == null) {
			throw new ContextedRuntimeException("Message listener must be registered.");
		}
	}
	
	@GuardedBy("this")
	private boolean checkAndSetListener(MessageReceiptListener listener) {
		if (this.messageReceiptListener == null) {
			this.messageReceiptListener = (MessageReceiptListener) 
										checkForNull(listener, "Null listener can't be registered.");
			this.tcpServer.registerListener(this.messageReceiptListener);
		} else {
			throw new ContextedRuntimeException("Message listener already registered.");
		}
		return true;
	}
	
	@GuardedBy("this")
	private void checkInitialization() {
		if (!initialized) {
			throw new ContextedRuntimeException("Messenger should be initialized first.");
		}
	}
	
	@GuardedBy("this")
	private void checkForFailure() {
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
	private Map<Process, Socket> createCache(final int maxCacheSize) {
		// LRU cache.
		Map<Process, Socket> cache = new LinkedHashMap<Process, Socket>(maxCacheSize, 0.75f, true) {
			private static final long serialVersionUID = 1L;
			
			protected boolean removeEldestEntry(Map.Entry<Process, Socket> eldest) {
				return size() > maxCacheSize;
			}
		};
		return Collections.synchronizedMap(cache);
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
