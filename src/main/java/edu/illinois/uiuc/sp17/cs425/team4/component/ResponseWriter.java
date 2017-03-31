package edu.illinois.uiuc.sp17.cs425.team4.component;


import edu.illinois.uiuc.sp17.cs425.team4.exceptions.MessengerException;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;

/**
 * An interface to be provided to message listeners so that respond to the message received.
 * 
 * This interface was introduced to make the call to message listener totally asynchronous.
 * Now, messenger can just pass a response writer to message listener instead of waiting for reply
 * and then replying itself.
 * 
 * @author bbassi2
 */
public interface ResponseWriter {
	/**
	 * Write response. This method should not be called after calling close().
	 * @param response
	 * @throws MessengerException
	 */
	public void writeResponse(Message response) throws MessengerException;
	
	/**
	 * Close response writer.
	 */
	public void close();
}
