package edu.illinois.uiuc.sp17.cs425.team4.model;

public interface LockMessage<K> extends Message {
	
	public K getKey();
	
	public Transaction getTransaction();
	
	public LockType getLockType();
	
	public LockActionType getActionType();
}
