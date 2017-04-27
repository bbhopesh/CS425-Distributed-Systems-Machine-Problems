package edu.illinois.uiuc.sp17.cs425.team4.model;

public interface CloseTransactionMessage extends Message {
	public Transaction getTransaction();
}
