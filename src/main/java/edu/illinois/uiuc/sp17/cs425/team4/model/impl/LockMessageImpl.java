package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.LockMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

public class LockMessageImpl<K> extends MessageBaseImpl implements LockMessage<K>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7401690522543514611L;
	private final K key;
	private final Transaction transaction;
	private final LockType lockType;
	private final LockActionType actionType;

	public LockMessageImpl(Process originatingSource, K key, Transaction transaction, 
								LockType lockType, LockActionType actionType) {
		super(MessageType.LOCK, originatingSource);
		this.key = key;
		this.transaction = transaction;
		this.lockType = lockType;
		this.actionType = actionType;
	}
	
	public LockMessageImpl(Process originatingSource, K key, Transaction transaction, 
			LockType lockType, LockActionType actionType, UUID uId) {
		super(MessageType.LOCK, originatingSource, uId);
		this.key = key;
		this.transaction = transaction;
		this.lockType = lockType;
		this.actionType = actionType;
	}

	@Override
	public K getKey() {
		return this.key;
	}

	@Override
	public Transaction getTransaction() {
		return this.transaction;
	}

	@Override
	public LockType getLockType() {
		return this.lockType;
	}

	@Override
	public LockActionType getActionType() {
		return this.actionType;
	}

}
