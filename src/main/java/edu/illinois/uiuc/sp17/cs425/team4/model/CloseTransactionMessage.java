package edu.illinois.uiuc.sp17.cs425.team4.model;

/**
 * Message asking to close the transaction.
 * 
 * @author bbassi2.
 *
 */
public interface CloseTransactionMessage extends Message {
	/**
	 * Get transaction to be closed.
	 * @return Transaction transaction to be closed.
	 */
	public Transaction getTransaction();
}
