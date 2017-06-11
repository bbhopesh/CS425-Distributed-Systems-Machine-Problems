package edu.illinois.uiuc.sp17.cs425.team4.model;

/**
 * A message requesting lock on a key.
 * 
 * @author bbassi2
 * 
 * @param <K> Key type.
 */
public interface LockMessage<K> extends Message {
	
	/** 
	 * @return key.
	 */
	public K getKey();
	
	/**
	 * @return transaction requesting lock.
	 */
	public Transaction getTransaction();
	
	/**
	 * @return Type of lock requested
	 */
	public LockType getLockType();
	
	/**
	 * @return Type of lock action requested.
	 */
	public LockActionType getActionType();
}
