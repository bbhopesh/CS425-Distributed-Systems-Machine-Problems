package edu.illinois.uiuc.sp17.cs425.team4.exceptions;

public class LockServiceException extends RuntimeException {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8805916326218492402L;

	public LockServiceException(String message) {
		super(message);
	}
	
	public LockServiceException() {
		super();
	}
	
	public LockServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
