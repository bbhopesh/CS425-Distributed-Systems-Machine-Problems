package edu.illinois.uiuc.sp17.cs425.team4.component;

import edu.illinois.uiuc.sp17.cs425.team4.exceptions.LockServiceException;
import edu.illinois.uiuc.sp17.cs425.team4.exceptions.TransactionAbortedException;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public interface TransactionManager<K, V> {

	public V get(Process owner, K key) throws TransactionAbortedException, LockServiceException;
	
	public void set(Process owner, K key, V value) throws TransactionAbortedException, LockServiceException;
	
	public void commit();
	
	public void abort();
	
	// Transaction managed by this manager.
	public Transaction getTransaction();
}
