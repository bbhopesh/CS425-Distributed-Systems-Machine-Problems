package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import com.google.common.util.concurrent.Striped;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.model.LockMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.LockActionType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.LockType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

public class KeyLockManagerServer<K> implements MessageListener {

	/** Listener Identifier. */
	private static final String S_IDENTIFIER = "KeyLockService";
	/** Listener Identifier. */
	private static final MessageListenerIdentifier IDENTIFIER = 
			new MessageListenerIdentifierImpl(S_IDENTIFIER);
	
	private final Striped<ReadWriteLock> readWriteLocks;
	@SuppressWarnings("unused")
	private final Striped<Lock> upgradeReadToWriteLocks;
	
	// TODO We can optimize to remove transactions that are committed or aborted, not doing as of now.
	private final ConcurrentMap<Transaction, TransactionThread<K>> transactionThreads;
	
	private final Messenger messenger;
	private final Process myIdentity;
	private final Model model;
	
	public KeyLockManagerServer(Messenger messenger,
									Process myIdentity,
									Model model) {
		this.readWriteLocks = Striped.readWriteLock(50);
		this.upgradeReadToWriteLocks = Striped.lock(50);
		this.transactionThreads = new ConcurrentHashMap<>();
		
		this.messenger = messenger;
		this.messenger.registerListener(this);
		this.myIdentity = myIdentity;
		this.model = model;
	}
	
	
	// This method gets called asynchronously when a request comes in.
	@Override
	public void messageReceived(Pair<Process, Message> sourceAndMsg, ResponseWriter responseWriter) {
		try {
			@SuppressWarnings("unchecked")
			LockMessage<K> lockMsg= (LockMessage<K>) sourceAndMsg.getRight();
			// TODO Verify that process sending message is same as transaction.getOwner()
			
			Transaction transaction = lockMsg.getTransaction();
			LockType lockType = lockMsg.getLockType();
			LockActionType actionType = lockMsg.getActionType();
			K key = lockMsg.getKey();
			
			processNewRequest(transaction, lockType, actionType, key);
		} finally {
			// We are not using response writer at all.
			// Response writer is meant to respond on the same incoming socket connection,
			// but our request will be handled by transaction specific thread and not this thread,
			// we just have to let TransactionThread create a new connection and send response.
			responseWriter.close();
		}
		
	}
	
	public void processNewRequest(Transaction transaction, LockType lockType, LockActionType actionType, K key) {
		TransactionThread<K> transactionThread = getTransactionThread(transaction);
		transactionThread.setState(lockType, actionType, key);
		// Transaction thread wakes up every 5 ms and looks for new requests and process them and replies directly to transaction owner.
	}
	
	private TransactionThread<K> getTransactionThread(Transaction transaction) {
		TransactionThread<K> transactionThread = new TransactionThread<>(transaction, this.messenger,
				this.myIdentity, this.model, this.readWriteLocks);
		
		TransactionThread<K> old = this.transactionThreads.putIfAbsent(transaction, transactionThread);
		
		//LOG.info("Got transaction thread.");
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
		
		private final Transaction transaction;
		private final Messenger messenger;
		private final Model model;
		private final Process myIdentity;
		private final Striped<ReadWriteLock> readWriteLocks;
		
		private static final String ERROR_CLASS_KEY = "ErrorClass";
		private static final String ERROR_MESSAGE_KEY = "ErrorMessage";
		private static final String SUCCESS_MESSAGE_KEY = "SuccessMessage.";
		
		
		public TransactionThread(Transaction transaction, Messenger messenger,
									Process myIdentity,
									Model model, Striped<ReadWriteLock> readWriteLocks) {
			this.transaction = transaction;
			this.messenger = messenger;
			this.myIdentity = myIdentity;
			this.model = model;
			this.readWriteLocks = readWriteLocks;
		}
		
		public synchronized void setState(LockType lockType, LockActionType actionType, K key) {
			if (lockType == null || actionType == null || key == null) return;
			
			this.newRequest = true;
			this.lockType = lockType;
			this.actionType = actionType;
			this.key = key;
		}
		
		public void run() {
			LOG.info("Starting thread.");
			while (true) {
				synchronized(this) {
					try {
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
					// I am eating InterruptedException which is bad practice but this whole server is jugaad :)
				}
			}	
		}

		private void processNewRequest() {
			// For a single transaction, there is no identifier for different requests.
			// Clients should make sure that they don't send a new request until they receive response for previous one.
			// Otherwise they won't be able to map responses to requests.
			
			// TODO Add the lock acquire requests to some graph structure to detect deadlocks.
			// TODO make acquiring lock interruptible so that we can gracefully come out of deadlocks.(will do when write code to handle deadlocks)
			
			if (this.lockType == LockType.READ && actionType == LockActionType.ACQUIRE) {
				processReadAcquireRequest();
			}
			
			if (this.lockType == LockType.READ && actionType == LockActionType.RELEASE) {
				processReadReleaseRequest();
			}
			
			if (this.lockType == LockType.WRITE && actionType == LockActionType.ACQUIRE) {
				processWriteAcquireRequest();
			}
			
			if (this.lockType == LockType.WRITE && actionType == LockActionType.RELEASE) {
				processWriteReleaseRequest();
			}
		}
		
		private void processReadAcquireRequest() {
			LOG.info(String.format("Transaction %s requesting read lock on key %s",
										this.transaction.getDisplayName(), this.key));
			Message message = createAckMessage();
			try {
				ReadWriteLock lock = this.readWriteLocks.get(this.key);
				lock.readLock().lock();
				LOG.info(String.format("Transaction %s request fulffiled successfully",
						this.transaction.getDisplayName()));
				setSuccess(message);
			} catch(Throwable t) {
				LOG.info(String.format("Transaction %s request failed.",
						this.transaction.getDisplayName()));
				setError(message, t);
			}
			// Only trying once, if message sending fails we are doomed. Hopefully, TCP will save us.
			// Receiver should close socket if they don't want to send message, otherwise wait here will be infinite.
			this.messenger.send(Pair.of(this.transaction.getOwner(), message), 0); // 0 is infinite timeout.
		}
		
		
		private void processReadReleaseRequest() {
			LOG.info(String.format("Transaction %s attempting to release read lock on key %s",
					this.transaction.getDisplayName(), this.key));
			Message message = createAckMessage();
			try {
				ReadWriteLock lock = this.readWriteLocks.get(this.key);
				lock.readLock().unlock();
				LOG.info(String.format("Transaction %s request fulffiled successfully",
						this.transaction.getDisplayName()));
				setSuccess(message);
			} catch(Throwable t) {
				LOG.info(String.format("Transaction %s request failed.",
						this.transaction.getDisplayName()));
				setError(message, t);
			}
			// Only trying once, if message sending fails we are doomed. Hopefully, TCP will save us.
			// Receiver should close socket if they don't want to send message, otherwise wait here will be infinite.
			this.messenger.send(Pair.of(this.transaction.getOwner(), message), 0); // 0 is infinite timeout.
		}
		
		private void processWriteAcquireRequest() {
			LOG.info(String.format("Transaction %s requesting write lock on key %s",
					this.transaction.getDisplayName(), this.key));
			Message message = createAckMessage();
			try {
				ReadWriteLock lock = this.readWriteLocks.get(this.key);
				lock.writeLock().lock();
				LOG.info(String.format("Transaction %s request fulffiled successfully",
						this.transaction.getDisplayName()));
				setSuccess(message);
			} catch(Throwable t) {
				LOG.info(String.format("Transaction %s request failed.",
						this.transaction.getDisplayName()));
				setError(message, t);
			}
			// Only trying once, if message sending fails we are doomed. Hopefully, TCP will save us.
			// Receiver should close socket if they don't want to send message, otherwise wait here will be infinite.
			this.messenger.send(Pair.of(this.transaction.getOwner(), message), 0); // 0 is infinite timeout.
		}
		
		private void processWriteReleaseRequest() {
			LOG.info(String.format("Transaction %s attempting to release read lock on key %s",
					this.transaction.getDisplayName(), this.key));
			Message message = createAckMessage();
			try {
				ReadWriteLock lock = this.readWriteLocks.get(this.key);
				lock.writeLock().unlock();
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
