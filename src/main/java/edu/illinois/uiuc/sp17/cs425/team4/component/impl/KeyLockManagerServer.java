package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.component.DeadlockListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.component.TransactionsDeadlockDetector;
import edu.illinois.uiuc.sp17.cs425.team4.model.LockMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.LockActionType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.LockType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.MessageType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.CloseTransactionMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

/**
 * Key lock server.
 * This class serves request from different (remote) transactions.
 * It runs a thread per transaction and submits request for this transaction on that thread.
 * 
 * @author bbassi2
 *
 * @param <K> Key type.
 */
public class KeyLockManagerServer<K> implements MessageListener, DeadlockListener {

	/** Listener Identifier. */
	private static final String S_IDENTIFIER = "KeyLockService";
	/** Listener Identifier. */
	private static final MessageListenerIdentifier IDENTIFIER = 
			new MessageListenerIdentifierImpl(S_IDENTIFIER);
	/** Read write lock for keys. */
	private final ConcurrentMap<K, ReentrantReadWriteLock> readWriteLocks;
	/** Locks to upgrade atomically upgrade read locks to write locks. */
	private final ConcurrentMap<K, ReentrantLock> upgradeReadToWriteLocks;
	/** Mapping a transaction to a thread. */
	private final ConcurrentMap<Transaction, TransactionThread<K>> transactionThreads;
	/** Deadlock detector. */
	private final WaitForGraphDeadlockDetector<K> deadlockDoctor;
	/** Messenger. */
	private final Messenger messenger;
	/** Process representing identity of lock service. */
	private final Process myIdentity;
	/** Model. */
	private final Model model;
	
	/**
	 * Create an instance.
	 * @param messenger Messenger.
	 * @param myIdentity Identity of lock service. 
	 * @param model Model.
	 */
	public KeyLockManagerServer(Messenger messenger,
									Process myIdentity,
									Model model) {
		this.readWriteLocks = new ConcurrentHashMap<>(50);
		this.upgradeReadToWriteLocks = new ConcurrentHashMap<>(50);
		this.transactionThreads = new ConcurrentHashMap<>();
		
		this.messenger = messenger;
		this.messenger.registerListener(this);
		this.myIdentity = myIdentity;
		this.model = model;
		this.deadlockDoctor = new WaitForGraphDeadlockDetector<>(this);
		this.deadlockDoctor.initialize();
	}

	// This method gets called asynchronously when a request comes in.
	@Override
	public void messageReceived(Pair<Process, Message> sourceAndMsg, ResponseWriter responseWriter) {
		try {
			Message msg = sourceAndMsg.getRight();
			if (msg.getMessageType() == MessageType.LOCK) {
				@SuppressWarnings("unchecked")
				LockMessage<K> lockMsg= (LockMessage<K>) msg;
				// TODO Verify that process sending message is same as transaction.getOwner()
				
				Transaction transaction = lockMsg.getTransaction();
				LockType lockType = lockMsg.getLockType();
				LockActionType actionType = lockMsg.getActionType();
				K key = lockMsg.getKey();
				processNewRequest(transaction, lockType, actionType, key);
			} else if (msg.getMessageType() == MessageType.CLOSE_TRANSACTION) {
				CloseTransactionMessage closeTransactionMessage = (CloseTransactionMessage) msg;
				closeTransaction(closeTransactionMessage.getTransaction());
			}
		} finally {
			// We are not using response writer at all.
			// Response writer is meant to respond on the same incoming socket connection,
			// but our request will be handled by transaction specific thread and not this thread,
			// we just have to let TransactionThread create a new connection and send response.
			responseWriter.close();
		}
		
	}
	
	@Override
	public void abortTransaction(Transaction transaction) {
		Thread thread = getTransactionThread(transaction);
		if (thread != null) {
			// We will interrupt deadlocked thread.
			// Deadlocked thread on receiving interruption will release held locks and inform transaction owner that it has been aborted.
			thread.interrupt();
			this.transactionThreads.remove(transaction);
		}
	}
	
	private void closeTransaction(Transaction transaction) {
		TransactionThread<K> transactionThread = getTransactionThread(transaction);
		if (transactionThread != null) {
			transactionThread.setCloseTransaction();
			this.transactionThreads.remove(transaction);
		}
	}


	public void processNewRequest(Transaction transaction, LockType lockType, LockActionType actionType, K key) {
		initializeLocks(key);
		TransactionThread<K> transactionThread = getTransactionThread(transaction);
		transactionThread.setState(lockType, actionType, key);
		// Transaction thread wakes up every 5 ms and looks for new requests and process them and replies directly to transaction owner.
	}
	
	private void initializeLocks(K key) {
		ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
		this.readWriteLocks.putIfAbsent(key, readWriteLock);
		
		ReentrantLock upgradeLock = new ReentrantLock();
		this.upgradeReadToWriteLocks.putIfAbsent(key, upgradeLock);
	}

	private TransactionThread<K> getTransactionThread(Transaction transaction) {
		TransactionThread<K> transactionThread = new TransactionThread<>(transaction, this.messenger,
				this.myIdentity, this.model, this.readWriteLocks, this.upgradeReadToWriteLocks, this.deadlockDoctor);
		
		TransactionThread<K> old = this.transactionThreads.putIfAbsent(transaction, transactionThread);
		
		if (old != null) {
			transactionThread = old;
		} else {
			transactionThread.start();
		}
		return transactionThread;
	}

	@Override
	public MessageListenerIdentifier getIdentifier() {
		return IDENTIFIER;
	}
	
	
	
	private static class TransactionThread<K> extends Thread {
		/**  Logger. */
		private final static Logger LOG = Logger.getLogger(TransactionThread.class.getName());
		
		private LockType lockType;
		private LockActionType actionType;
		private K key;
		private boolean newRequest;
		
		private boolean closeTransaction;
		
		private final Transaction transaction;
		private final Messenger messenger;
		private final Model model;
		private final Process myIdentity;
		private final ConcurrentMap<K, ReentrantReadWriteLock> readWriteLocks;
		private final ConcurrentMap<K, ReentrantLock> upgradeReadToWriteLocks;
		private final TransactionsDeadlockDetector<K> deadlockDetector;
		
		// Keeps track of all locked keys, used to process releaseAllLocks request by the client.
		// Client will typically send such a request at the end of transaction(commit or abort)
		private final Set<K> allLockedKeys;

		private static final String ERROR_CLASS_KEY = "ErrorClass";
		private static final String ERROR_MESSAGE_KEY = "ErrorMessage";
		private static final String SUCCESS_MESSAGE_KEY = "SuccessMessage.";
		
		
		public TransactionThread(Transaction transaction, Messenger messenger,
									Process myIdentity,
									Model model, ConcurrentMap<K, ReentrantReadWriteLock> readWriteLocks,
									ConcurrentMap<K, ReentrantLock> upgradeReadToWriteLocks,
									TransactionsDeadlockDetector<K> deadlockDetector) {
			this.transaction = transaction;
			this.messenger = messenger;
			this.myIdentity = myIdentity;
			this.model = model;
			this.readWriteLocks = readWriteLocks;
			this.upgradeReadToWriteLocks = upgradeReadToWriteLocks;
			this.deadlockDetector = deadlockDetector;
			this.allLockedKeys = new HashSet<>();
			
			this.newRequest = false;
			this.closeTransaction = false;
		}
		
		public synchronized void setState(LockType lockType, LockActionType actionType, K key) {
			if (lockType == null || actionType == null || key == null) return;
			
			this.newRequest = true;
			this.lockType = lockType;
			this.actionType = actionType;
			this.key = key;
			this.allLockedKeys.add(key);
		}
		
		public synchronized void setCloseTransaction() {
			this.closeTransaction = true;
		}
		
		public void run() {
			LOG.info(String.format("Starting thread with %s id for transaction %s ", Thread.currentThread().getId(), this.transaction));
			while (true) {
				synchronized(this) { 
					// Using intrinsic locking here won't be a cause of deadlocks among transactions.
					// Intrinsic lock on thread instance is just to coordinate requests of a single transaction.
					// Because each transaction has their own thread, this intrinsic lock cannot cause deadlock between multiple locks.
					// Deadlock across multiple transactions can only happen because of locks on a key.
					try {
						if(this.closeTransaction) {
							// Poison. We could have received poison because of two reasons:
							// 1. Deadlock and then interrupt(handled below at time of lock acquisition)
							// 2. Client wanting to close transaction.
							closeTransaction();
							break;
						}
						
						if (this.newRequest) {
							try {
								// When request is processed, we don't inform anyone in current JVM, 
								// just send a reply to transaction owner.
								processNewRequest();
							} finally {
								this.newRequest = false;
							}
						}
					} catch(Throwable t) {
						// ignore. Always go on in loop, thread should never die.
					}/* finally {
						this.newRequest = false;
					}*/
				}

				
				try {
					// sleep before proceeding to next iteration.
					Thread.sleep(5);
				} catch (Throwable t) {
					// ignore.
					// I am eating InterruptedException here.
					// Interrupt are used as mechanism to abort transactions. 
					// Deadlock detector is informed of an attempt to acquire lock right before we call lock.lockInterruptibly()
					// Therefore, when the thread is interrupted to recover from deadlock, it won't be stuck in Thread.sleep
					// but one of the lock methods. Hence, we can safely ignore InterruptedException here.
				}
			}
			LOG.info(String.format("Closing thread with %s id for transaction %s ", Thread.currentThread().getId(), this.transaction));
		}

		private void handleInterruption() {
			LOG.info(String.format("Transaction %s interrupted while waiting on a lock for key %s.", this.transaction, this.key));
			// We will just set close transaction. Due to this in the next iteration, we will release all locks and shut down.
			setCloseTransaction();
		}

		private void processNewRequest() {
			// For a single transaction, there is no identifier for different requests.
			// Clients should make sure that they don't send a new request until they receive response for previous one.
			// Otherwise they won't be able to map responses to requests.
			
			if (this.lockType == LockType.READ && actionType == LockActionType.ACQUIRE) {
				processReadAcquireRequest();
			} else if (this.lockType == LockType.READ && actionType == LockActionType.RELEASE) {
				processReadReleaseRequest();
			} else if (this.lockType == LockType.WRITE && actionType == LockActionType.ACQUIRE) {
				processWriteAcquireRequest();
			} else if (this.lockType == LockType.WRITE && actionType == LockActionType.RELEASE) {
				processWriteReleaseRequest();
			}
			
		}
		
		private void closeTransaction() {
			LOG.info(String.format("Cleaning up and closing transaction %s.", this.transaction.getDisplayName()));
			releaseAllLocks();
		}
		
		private void releaseAllLocks() {
			// This method is called to release all locks when thread is interrupted and asked to abort the transaction.
			// This method shouldn't throw any exception, otherwise locks won't be released and we are doomed.
			
			// Release locks for all keys.
			Iterator<K> lockedKeysIt = this.allLockedKeys.iterator();
			while (lockedKeysIt.hasNext()) {
				K lockedKey = lockedKeysIt.next();
				ReentrantReadWriteLock reLock = this.readWriteLocks.get(lockedKey);
				// Release read lock.
				releaseReadLockCompletely(reLock, lockedKey);
				
				// Release write lock.
				releaseWriteLockCompletely(reLock, lockedKey);
				
				// Release upgrade locks.
				ReentrantLock readToWriteUpgradeLock = this.upgradeReadToWriteLocks.get(lockedKey);
				releaseUpgradeLockCompletely(readToWriteUpgradeLock, lockedKey);
				
				// Remove.
				lockedKeysIt.remove();
			}
		}
		
		private void releaseUpgradeLockCompletely(ReentrantLock readToWriteUpgradeLock, K key) {
			int k = readToWriteUpgradeLock.getHoldCount();
			for (int i = 0; i < k; i++) {
				readToWriteUpgradeLock.unlock();
				if (i == 0) {
					// Only inform once.
					this.deadlockDetector.releaseReadToWriteUpgradeLock(this.transaction, key);
					LOG.info(String.format("Transaction %s releasing read to write upgrade lock on key: %s", this.transaction, key));
				}
			}
		}

		private void processReadAcquireRequest() {
			LOG.info(String.format("Transaction %s requesting read lock on key %s",
										this.transaction.getDisplayName(), this.key));
			Message message = createAckMessage();
			try {
				ReadWriteLock lock = this.readWriteLocks.get(this.key);
				
				this.deadlockDetector.wantReadLock(this.transaction, this.key);
				lock.readLock().lockInterruptibly();
				this.deadlockDetector.gotReadLock(this.transaction, this.key);
				
				LOG.info(String.format("Transaction %s request fulffiled successfully",
						this.transaction.getDisplayName()));
				setSuccess(message);
			} catch (InterruptedException e) {
				handleInterruption();
				setError(message, e); // To inform client that we are aborting him.
			} catch(Throwable t) {
				LOG.info(String.format("Transaction %s request failed.",
						this.transaction.getDisplayName()));
				setError(message, t);
			}
			// Only trying once, if message sending fails we are doomed. Hopefully, TCP will save us.
			// Receiver should close socket if they don't want to send response, otherwise wait here will be infinite.
			this.messenger.send(Pair.of(this.transaction.getOwner(), message), 0); // 0 is infinite timeout.
		}
		
		
		private void processReadReleaseRequest() {
			LOG.info(String.format("Transaction %s attempting to release read lock on key %s",
					this.transaction.getDisplayName(), this.key));
			Message message = createAckMessage();
			try {
				ReentrantReadWriteLock lock = this.readWriteLocks.get(this.key);
				releaseReadLockCompletely(lock, this.key);
				LOG.info(String.format("Transaction %s request fulffiled successfully",
						this.transaction.getDisplayName()));
				setSuccess(message);
			} catch(Throwable t) {
				LOG.info(String.format("Transaction %s request failed.",
						this.transaction.getDisplayName()));
				setError(message, t);
			}
			// Only trying once, if message sending fails we are doomed. Hopefully, TCP will save us.
			// Receiver should close socket if they don't want to send response, otherwise wait here will be infinite.
			this.messenger.send(Pair.of(this.transaction.getOwner(), message), 0); // 0 is infinite timeout.
		}
		
		private void processWriteAcquireRequest() {
			LOG.info(String.format("Transaction %s requesting write lock on key %s",
					this.transaction.getDisplayName(), this.key));
			Message message = createAckMessage();
			try {
				acquireWriteLockSafely();
				LOG.info(String.format("Transaction %s request fulffiled successfully",
						this.transaction.getDisplayName()));
				setSuccess(message);
			} catch (InterruptedException e) {
				handleInterruption();
				setError(message, e); // To inform client that we are aborting him.
			} catch(Throwable t) {
				LOG.info(String.format("Transaction %s request failed.",
						this.transaction.getDisplayName()));
				setError(message, t);
			}
			// Only trying once, if message sending fails we are doomed. Hopefully, TCP will save us.
			// Receiver should close socket if they don't want to send response, otherwise wait here will be infinite.
			this.messenger.send(Pair.of(this.transaction.getOwner(), message), 0); // 0 is infinite timeout.
		}
		
		private void acquireWriteLockSafely() throws InterruptedException {
			ReentrantLock lock1 = this.upgradeReadToWriteLocks.get(this.key);
			ReentrantReadWriteLock lock2 = this.readWriteLocks.get(this.key);
			
			// Get the first lock, so we can atomically release the read lock and acquire write lock.
			try {
				// Logic to handle the case when a thread is upgrading from read lock to write lock.
				 
				LOG.info(String.format("Transaction %s requesting read to write upgrade lock on key %s",
					this.transaction.getDisplayName(), this.key));
				this.deadlockDetector.wantReadToWriteUpgradeLock(this.transaction, this.key);
				lock1.lockInterruptibly();
				this.deadlockDetector.gotReadToWriteUpgradeLock(this.transaction, this.key);
				
				// Only one thread can reach here, so we can atomically release read lock and acquire write lock.
				releaseReadLockCompletely(lock2, this.key);
				
				this.deadlockDetector.wantWriteLock(this.transaction, this.key);
				lock2.writeLock().lockInterruptibly();
				this.deadlockDetector.gotWriteLock(this.transaction, this.key);
				
				// If two threads try to get the write lock, one of them will be stuck at lock1 above
				// and other will pass lock1 and try to get lock from lock2.writeLock... 
				// They will be stuck until the second thread gets write lock and releases lock1, 
				// then first thread will no longer be stuck at lock1
				// but it will be stuck at writeLock until writeLock is available again.
				
				// I hope that the above logic will let us safely upgrade a read lock to write lock.
				// Only issue is when two threads have read locks and both want to upgrade.
				// In such a case, one of them will enter lock1 but wait on writeLock and other guy will keep on waiting on lock1.
				// Thread waiting on writeLock will not get it because the other thread has readLock, while the thread
				// waiting on lock1 will not get it because first thread has it and is waiting for writeLock.
				// We have a deadlock, deadlock detector will save us.
			} finally {
				releaseUpgradeLockCompletely(lock1, this.key);
			}
		}

		private void releaseReadLockCompletely(ReentrantReadWriteLock lock, K key) {
			// If this thread doesn't hold read lock, this method is a no-op.
			int k = lock.getReadHoldCount();
			for (int i = 0; i < k; i++) {
				lock.readLock().unlock();
				if (i == 0) {
					// Only inform once.
					this.deadlockDetector.releaseReadLock(this.transaction, key);
					LOG.info(String.format("Transaction %s releasing read lock on key: %s", this.transaction, key));
				}
			}
		}
		
		private void releaseWriteLockCompletely(ReentrantReadWriteLock lock, K key) {
			// If this thread doesn't hold write lock, this method is a no-op.
			int k = lock.getWriteHoldCount();
			for (int i = 0; i < k; i++) {
				lock.writeLock().unlock();
				if (i == 0) {
					// Only inform once.
					this.deadlockDetector.releaseWriteLock(this.transaction, key);
					LOG.info(String.format("Transaction %s releasing write lock on key: %s", this.transaction, key));
				}
			}
		}

		private void processWriteReleaseRequest() {
			LOG.info(String.format("Transaction %s attempting to release read lock on key %s",
					this.transaction.getDisplayName(), this.key));
			Message message = createAckMessage();
			try {
				ReentrantReadWriteLock lock = this.readWriteLocks.get(this.key);
				releaseWriteLockCompletely(lock, this.key);
				LOG.info(String.format("Transaction %s request fulffiled successfully",
						this.transaction.getDisplayName()));
				setSuccess(message);
			} catch(Throwable t) {
				LOG.info(String.format("Transaction %s request failed.",
						this.transaction.getDisplayName()));
				setError(message, t);
			}
			// Only trying once, if message sending fails we are doomed. Hopefully, TCP will save us.
			// Receiver should close socket if they don't want to respond to this message, otherwise wait here will be infinite.
			this.messenger.send(Pair.of(this.transaction.getOwner(), message), 0); // 0 is infinite timeout.
		}
		
		private void setSuccess(Message message) {
			message.getMetadata().addProperty(SUCCESS_MESSAGE_KEY, true);
		}
		
		private void setError(Message message, Throwable error) {
			String className = error.getClass().getName();
			String errorMessage = error.getMessage();
			message.getMetadata().addProperty(SUCCESS_MESSAGE_KEY, false);
			message.getMetadata().addProperty(ERROR_CLASS_KEY, className);
			message.getMetadata().addProperty(ERROR_MESSAGE_KEY, errorMessage);
		}
		
		private Message createAckMessage() {
			Message msg = this.model.createAckMessage(this.myIdentity);
			msg.setMessageListenerId(KeyLockManagerServer.IDENTIFIER);
			return msg;
		}
	}
}
