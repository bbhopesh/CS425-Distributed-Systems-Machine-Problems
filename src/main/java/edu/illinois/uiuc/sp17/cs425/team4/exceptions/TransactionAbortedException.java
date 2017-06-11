package edu.illinois.uiuc.sp17.cs425.team4.exceptions;

/**
 * An exception raised when system decides to abort an transaction.
 * 
 * @author bbassi2.
 *
 */
public class TransactionAbortedException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 600080350577053741L;

	public TransactionAbortedException(String message) {
		super(message);
	}
	
	public TransactionAbortedException() {
		super();
	}
	
	public TransactionAbortedException(String message, Throwable cause) {
		super(message, cause);
	}

}
