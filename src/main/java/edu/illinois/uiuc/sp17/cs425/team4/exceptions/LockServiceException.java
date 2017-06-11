package edu.illinois.uiuc.sp17.cs425.team4.exceptions;

/**
 * An exception to be raised from locking service.
 * 
 * @author bbassi2
 *
 */
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
