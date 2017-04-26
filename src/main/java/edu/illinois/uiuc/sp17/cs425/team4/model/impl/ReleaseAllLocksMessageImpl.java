package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.ReleaseAllLocksMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

public class ReleaseAllLocksMessageImpl extends MessageBaseImpl implements ReleaseAllLocksMessage, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8411851866753402144L;
	private final Transaction transaction;

	public ReleaseAllLocksMessageImpl(Process originatingSource, Transaction transaction) {
		super(MessageType.RELEASE_ALL_LOCKS, originatingSource);
		this.transaction = transaction;
	}
	
	public ReleaseAllLocksMessageImpl(Process originatingSource, Transaction transaction, UUID uId) {
		super(MessageType.RELEASE_ALL_LOCKS, originatingSource, uId);
		this.transaction = transaction;
	}
	
	@Override
	public Transaction getTransaction() {
		return this.transaction;
	}

}
