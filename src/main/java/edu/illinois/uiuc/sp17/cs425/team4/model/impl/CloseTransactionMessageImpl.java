package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.CloseTransactionMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

public class CloseTransactionMessageImpl extends MessageBaseImpl implements CloseTransactionMessage, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8411851866753402144L;
	private final Transaction transaction;

	public CloseTransactionMessageImpl(Process originatingSource, Transaction transaction) {
		super(MessageType.CLOSE_TRANSACTION, originatingSource);
		this.transaction = transaction;
	}
	
	public CloseTransactionMessageImpl(Process originatingSource, Transaction transaction, UUID uId) {
		super(MessageType.CLOSE_TRANSACTION, originatingSource, uId);
		this.transaction = transaction;
	}
	
	@Override
	public Transaction getTransaction() {
		return this.transaction;
	}

}
