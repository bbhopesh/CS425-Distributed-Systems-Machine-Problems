package edu.illinois.uiuc.sp17.cs425.team4.component;

import edu.illinois.uiuc.sp17.cs425.team4.exceptions.LockServiceException;
import edu.illinois.uiuc.sp17.cs425.team4.exceptions.NoSuchTransactionException;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

/**
 * A lock manager where a separate ReadWrite lock is used for each key.
 * The permits are given to transactions instead of threads like in traditional locking.
 * 
 * Ideally, there should be just one instance of lock manager in your application,
 * otherwise for same key, you could end up using different locks.
 * 
 * @author bbassi2
 *
 * @param <K>
 */
public interface KeyLockManager<K> {
	
	/**
	 * Begin new transaction.
	 * @return a new transaction.
	 */
	public Transaction beginNew();
	
	/**
	 * Acquire read lock on the given key for this transaction. The method blocks until lock is available.
	 * @param transaction Transaction.
	 * @param key Key.
	 * @throws NoSuchTransactionException if the provided transaction doesn't exist.
	 * @throws LockServiceException If there is some exception in handling lock request.
	 */
	public void acquireReadLock(Transaction transaction, K key) throws NoSuchTransactionException, LockServiceException;
	
	/**
	 * Acquire write lock on the given key for this transaction. The method blocks until lock is available
	 * @param transaction Transaction.
	 * @param key Key.
	 * @throws NoSuchTransactionException If the provided transaction doesn't exist.
	 * @throws LockServiceException If there is some exception in handling lock request.
	 */
	public void acquireWriteLock(Transaction transaction, K key) throws NoSuchTransactionException, LockServiceException;
	
	/**
	 * Release read lock held by given transaction on the given key.
	 * @param transaction Transaction.
	 * @param key Key.
	 * @throws NoSuchTransactionException If the provided transaction doesn't exist.
	 * @throws LockServiceException If there is some exception in handling lock request.
	 */
	public void releaseReadLock(Transaction transaction, K key) throws NoSuchTransactionException, LockServiceException;
	
	/**
	 * Release write lock held by given transaction on the given key.
	 * @param transaction Transaction.
	 * @param key Key.
	 * @throws NoSuchTransactionException If the provided transaction doesn't exist.
	 * @throws LockServiceException If there is some exception in handling lock request.
	 */
	public void releaseWriteLock(Transaction transaction, K key) throws NoSuchTransactionException, LockServiceException;
	
	// TODO maybe a method to indicate that transaction is done, so that lock manager could clear transaction specific state.
}
