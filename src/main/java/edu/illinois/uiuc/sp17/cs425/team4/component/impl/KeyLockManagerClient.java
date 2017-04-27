package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.component.KeyLockManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.exceptions.LockServiceException;
import edu.illinois.uiuc.sp17.cs425.team4.exceptions.NoSuchTransactionException;
import edu.illinois.uiuc.sp17.cs425.team4.exceptions.TransactionAbortedException;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.LockActionType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.LockType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe 
// Should be used by one and only one thread.
// If multiple threads access at once, we don't have enough synchronization to protect state.
// Also, multiple requests will go to lock service and we will no way to map response to request.

// It might seem weird that a lock manager is not meant to be used in multi-threaded environment
// but we are in a distributed environment and this lock manager is supposed to be used only in process
// Similarly each process will have a lock manager and all these will talk to one global lock service.
public class KeyLockManagerClient<K> implements KeyLockManager<K>, MessageListener {
	/**  Logger. */
	private final static Logger LOG = Logger.getLogger(KeyLockManagerClient.class.getName());
	
	/** Listener Identifier. */
	private static final String S_IDENTIFIER = "KeyLockService";
	/** Listener Identifier. */
	private static final MessageListenerIdentifier IDENTIFIER = 
			new MessageListenerIdentifierImpl(S_IDENTIFIER);
	
	
	// Following three constants should be same as in KeyLockManagerServer
	private static final String ERROR_CLASS_KEY = "ErrorClass";
	private static final String ERROR_MESSAGE_KEY = "ErrorMessage";
	private static final String SUCCESS_MESSAGE_KEY = "SuccessMessage.";
	
	private final Messenger messenger;
	private final Process myIdentity;
	private final Model model;
	private final Process lockService;
	
	private int transactionsCount;
	private final Set<Transaction> transactions;
	
	private Result lockServiceResult;
	
	
	public KeyLockManagerClient(Messenger messenger, Process myIdentity, Model model, Process lockService) {
		this.messenger = messenger;
		this.messenger.registerListener(this);
		this.myIdentity = myIdentity;
		this.model = model;
		this.lockService = lockService;
		this.transactionsCount = 0;
		this.transactions = new HashSet<>();
	}
	
	@Override
	public Transaction beginNew() {
		String displayName = createNextTransactionDisplayName();
		Transaction t = this.model.createTransaction(this.myIdentity, displayName);
		LOG.info(String.format("Beginning new transaction: %s", displayName));
		this.transactions.add(t);
		return t;
	}

	@Override
	public void acquireReadLock(Transaction transaction, K key) throws NoSuchTransactionException, LockServiceException, TransactionAbortedException {
		// Check
		checkTransactionExists(transaction);
		LOG.info(String.format("Transaction %s requesting read lock on key %s",
				transaction.getDisplayName(), key));
		// Request
		Message acquireReadLockMessage = createAcquireReadLockMessage(transaction, key);
		this.messenger.send(Pair.of(this.lockService, acquireReadLockMessage), 0); // Infinite timeout
		// Wait.
		waitForRequestToBeFulfilled();
	}

	private Message createAcquireReadLockMessage(Transaction transaction, K key) {
		return stampIdentifier(this.model.createLockMessage(this.myIdentity, key, transaction, LockType.READ, LockActionType.ACQUIRE));
	}
	
	@Override
	public void acquireWriteLock(Transaction transaction, K key) throws NoSuchTransactionException, LockServiceException, TransactionAbortedException {
		// Check
		checkTransactionExists(transaction);
		LOG.info(String.format("Transaction %s requesting write lock on key %s",
				transaction.getDisplayName(), key));
		// Request
		Message acquireWriteLockMessage = createAcquireWriteLockMessage(transaction, key);
		this.messenger.send(Pair.of(this.lockService, acquireWriteLockMessage), 0); // Infinite timeout
		// Wait.
		waitForRequestToBeFulfilled();
	}
	
	private Message createAcquireWriteLockMessage(Transaction transaction, K key) {
		return stampIdentifier(this.model.createLockMessage(this.myIdentity, key, transaction, LockType.WRITE, LockActionType.ACQUIRE));
	}

	@Override
	public void releaseReadLock(Transaction transaction, K key)
			throws NoSuchTransactionException, LockServiceException {
		// Check
		checkTransactionExists(transaction);
		LOG.info(String.format("Transaction %s attempting to release read lock on key %s",
				transaction.getDisplayName(), key));
		// Request
		Message releaseReadLockMessage = createReleaseReadLockMessage(transaction, key);
		this.messenger.send(Pair.of(this.lockService, releaseReadLockMessage), 0); // Infinite timeout
		// Wait.
		waitForRequestToBeFulfilled();
		
	}
	
	private Message createReleaseReadLockMessage(Transaction transaction, K key) {
		return stampIdentifier(this.model.createLockMessage(this.myIdentity, key, transaction, LockType.READ, LockActionType.RELEASE));
	}

	@Override
	public void releaseWriteLock(Transaction transaction, K key)
			throws NoSuchTransactionException, LockServiceException {
		// Check
		checkTransactionExists(transaction);
		LOG.info(String.format("Transaction %s attempting to release write lock on key %s",
				transaction.getDisplayName(), key));
		// Request
		Message releaseWriteLockMessage = createReleaseWriteLockMessage(transaction, key);
		this.messenger.send(Pair.of(this.lockService, releaseWriteLockMessage), 0); // Infinite timeout
		// Wait.
		waitForRequestToBeFulfilled();
	}
	
	private Message createReleaseWriteLockMessage(Transaction transaction, K key) {
		return stampIdentifier(this.model.createLockMessage(this.myIdentity, key, transaction, LockType.WRITE, LockActionType.RELEASE));
	}
	
	@Override
	public void closeTransaction(Transaction transaction) throws LockServiceException {
		// Check
		checkTransactionExists(transaction);
		LOG.info(String.format("Transaction %s attempting to release all held locks.",
				transaction.getDisplayName()));
		// Request
		Message closeTransactionMessage = createCloseTransactionMessage(transaction);
		this.messenger.send(Pair.of(this.lockService, closeTransactionMessage), 100); // Arbitrary timeout.
		this.transactions.remove(transaction);
	}

	private Message createCloseTransactionMessage(Transaction transaction) {
		return stampIdentifier(this.model.createCloseTransactionMessage(this.myIdentity, transaction));
	}

	private void waitForRequestToBeFulfilled() {
		// Using condition queues would be better here but polling is okay too :)
		
		while (true) {
			synchronized(this) {
				if (this.lockServiceResult != null) {
					try {
						if (this.lockServiceResult.isSuccess()) {
							LOG.info("Request succeeded.");
							return; 
						} else {
							LOG.info("Request failed.");
							throw this.lockServiceResult.getError();
						}
					} finally {
						// Get ready for next request.
						this.lockServiceResult = null;
					}
				}
			}
			
			try {
				// Sleep and wait for request to arrive.
				Thread.sleep(2);
			} catch (InterruptedException e) {
				// Ignore.
			}
		}
		
	}
	
	
	@Override
	public synchronized void messageReceived(Pair<Process, Message> sourceAndMsg, ResponseWriter responseWriter) {
		try {
			// Not verifying source of message, should be lock service.
			Message message = sourceAndMsg.getRight();
			if (message.getMetadata().getBoolean(SUCCESS_MESSAGE_KEY)) {
				this.lockServiceResult = new Result();
			} else {
				this.lockServiceResult = new Result(message.getMetadata().getString(ERROR_CLASS_KEY),
														message.getMetadata().getString(ERROR_MESSAGE_KEY));
			}
		} finally {
			responseWriter.close();
		}
	}

	@Override
	public MessageListenerIdentifier getIdentifier() {
		return IDENTIFIER;
	}
	
	private Message stampIdentifier(Message message) {
		message.setMessageListenerId(getIdentifier());
		return message;
	}
	
	private void checkTransactionExists(Transaction t) throws NoSuchTransactionException {
		if (!this.transactions.contains(t)) {
			throw new NoSuchTransactionException();
		}
	}
	
	private String createNextTransactionDisplayName() {
		this.transactionsCount += 1;
		return String.format("%s_T%s", this.myIdentity.getDisplayName(), this.transactionsCount);
	}
	
	private static class Result {
		private final boolean success;
		
		private final String errorClass;
		
		private final String errorMessage;
		
		public Result(String errorClass, String errorMessage) {
			this(false, errorClass, errorMessage);
		}
		
		public Result() {
			this(true, null, null);
		}
		
		private Result(boolean success, String errorClass, String errorMessage) {
			this.success = success;
			this.errorClass = errorClass == null ? "" : errorClass;
			this.errorMessage = errorMessage == null ? "" : errorMessage;
		}
		
		public boolean isSuccess() {
			return this.success;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public RuntimeException getError() {
			try {
				Class c = Class.forName(this.errorClass);
				System.out.println(this.errorClass);
				System.out.println(this.errorMessage);
				Throwable d = (Throwable) c.getDeclaredConstructor(String.class).newInstance(this.errorMessage);
				if (d instanceof InterruptedException) {
					// Using interruptible locks is the strategy to safely kill one of the transactions in case of deadlocks.
					// Deadlock doctor at lock server will interrupt the transaction thread that it wants to kill.
					// That interrupted exception will be sent here by the lock server and we will translate it to transaction
					// aborted exception.
					return new TransactionAbortedException(this.errorMessage, d);
				} else {
					return new LockServiceException(d.getMessage(), d);
				}
			} catch (Exception e) {
				return new LockServiceException(String.format("%s occured at remote with message: %s",
														this.errorClass, this.errorMessage));
			}
		}
	}
}
