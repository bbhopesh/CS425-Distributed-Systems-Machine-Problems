package edu.illinois.uiuc.sp17.cs425.team4.exceptions;

public class MessengerException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7208142202352416611L;
	
	
	public MessengerException(String message) {
		super(message);
	}
	
	public MessengerException() {
		super();
	}
	
	public MessengerException(String message, Throwable cause) {
		super(message, cause);
	}
}
