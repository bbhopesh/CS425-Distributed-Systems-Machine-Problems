package edu.illinois.uiuc.sp17.cs425.team4.component;

import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

/**
 * A listener interface that is informed when deadlocks occur.
 * 
 * @author bbassi2
 */
public interface DeadlockListener {
	/**
	 * Abort transaction.
	 * @param transaction Transaction that will break deadlock.
	 */
	public void abortTransaction(Transaction transaction);
}
