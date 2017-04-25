package edu.illinois.uiuc.sp17.cs425.team4.exceptions;

public class NoSuchTransactionException extends RuntimeException {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6031277327798405326L;

	public NoSuchTransactionException(String message) {
		super(message);
	}
	
	public NoSuchTransactionException() {
		super();
	}
	
	public NoSuchTransactionException(String message, Throwable cause) {
		super(message, cause);
	}

}
