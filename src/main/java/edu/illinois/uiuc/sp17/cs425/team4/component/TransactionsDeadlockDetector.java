package edu.illinois.uiuc.sp17.cs425.team4.component;

import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

/**
 * An interface to detect deadlock among transactions.
 * @author bbassi2.
 *
 * @param <K> Key type.
 */
public interface TransactionsDeadlockDetector<K> {
	
	/**
	 * Initialize deadlock detector.
	 */
	public void initialize();
	
	/**
	 * Transaction wants write lock on the given key.
	 * @param transaction Transaction.
	 * @param key Key.
	 */
	public void wantWriteLock(Transaction transaction, K key);
	
	/**
	 * Transaction got write lock on the key.
	 * @param transaction Transaction.
	 * @param key Key.
	 */
	public void gotWriteLock(Transaction transaction, K key);
	
	/**
	 * Transaction releasing write lock on the key.
	 * @param transaction Transaction.
	 * @param key Key.
	 */
	public void releaseWriteLock(Transaction transaction, K key);
	
	/**
	 * Transaction wants read lock on the given key.
	 * @param transaction Transaction.
	 * @param key Key.
	 */
	public void wantReadLock(Transaction transaction, K key);
	
	/**
	 * Transaction got read lock on the key.
	 * @param transaction Transaction.
	 * @param key Key.
	 */
	public void gotReadLock(Transaction transaction, K key);
	
	/**
	 * Transaction releasing read lock on the key.
	 * @param transaction Transaction.
	 * @param key Key.
	 */
	public void releaseReadLock(Transaction transaction, K key);
	
	/**
	 * Transaction wants read to write upgrade lock on the given key.
	 * @param transaction Transaction.
	 * @param key Key.
	 */
	public void wantReadToWriteUpgradeLock(Transaction transaction, K key);
	
	/**
	 * Transaction got read lock to write upgrade lock on the given key.
	 * @param transaction Transaction.
	 * @param key Key.
	 */
	public void gotReadToWriteUpgradeLock(Transaction transaction, K key);
	
	/**
	 * Transaction released read to write upgrade lock on the given key.
	 * @param transaction Transaction.
	 * @param key Key.
	 */
	public void releaseReadToWriteUpgradeLock(Transaction transaction, K key);
	
	/**
	 * Clear data of the transaction.
	 * @param transaction Transaction.
	 */
	public void clear(Transaction transaction);
	
	/**
	 * Close transaction deadlock detector.
	 */
	public void close();
}
