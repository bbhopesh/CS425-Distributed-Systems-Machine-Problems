package edu.illinois.uiuc.sp17.cs425.team4.component;

import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

public interface TransactionsDeadlockDetector<K> {
	public void wantWriteLock(Transaction transaction, K key);
	
	public void gotWriteLock(Transaction transaction, K key);
	
	public void releaseWriteLock(Transaction transaction, K key);
	
	public void wantReadLock(Transaction transaction, K key);
	
	public void gotReadLock(Transaction transaction, K key);
	
	public void releaseReadLock(Transaction transaction, K key);
	
	public void wantReadToWriteUpgradeLock(Transaction transaction, K key);
	
	public void gotReadToWriteUpgradeLock(Transaction transaction, K key);
	
	public void releaseReadToWriteUpgradeLock(Transaction transaction, K key);
	
	public void clear(Transaction transaction);
}
