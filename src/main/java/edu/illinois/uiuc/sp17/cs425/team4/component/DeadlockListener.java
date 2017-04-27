package edu.illinois.uiuc.sp17.cs425.team4.component;

import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

public interface DeadlockListener {
	
	public void abortTransaction(Transaction transaction);
}
