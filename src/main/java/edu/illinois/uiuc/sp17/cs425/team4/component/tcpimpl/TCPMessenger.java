package edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl;

import java.io.IOException;
import java.net.Socket;
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
 * Upon receipt of messages, registered listener is submitted to a thread pool 
 * where it is run on a dedicated thread.
 * The implementation maintains a bounded cache of open Sockets for outgoing connections
 * and oldest entries are removed to make space for the new ones.
 * Default max size of cache is 10.
 * 
 * This implementation only supports one listener as of now.
 * 
 * @author bbassi2
 */
@ThreadSafe
final class TCPMessenger implements Messenger {
	private final TCPServer tcpServer;
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
		this.myIdentity = (Process) checkForNull(builder.getMyIdentity(), "Identity cannot be null");
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
			//checkInitialization(); // Initializations tarts server, we don't have to check for it here.
			checkForFailure();
		}
		try {
			// Open socket with destination.
			Process p = dstnAndMsg.getLeft();
			Socket s = new Socket(p.getInetAddress(), p.getPort());
			// Send destination the message and tell them who sent it.
			this.messageAdaptor.write(s, Pair.of(this.myIdentity, dstnAndMsg.getRight()));
			// Read response back and ignore source of this msg as source of the response msg will be destination of original msg.
			Pair<Process, Message> srcAndMsg = this.messageAdaptor.read(s);
			s.close();
			return srcAndMsg == null ? null: srcAndMsg.getRight();
		} catch (IOException e) {
			throw new ContextedRuntimeException(
					"Couldn't send message to " + dstnAndMsg.getLeft().toString(),
					e);
		}
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
			} catch (Exception e) {
				throw new ContextedRuntimeException(e);
			}
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
