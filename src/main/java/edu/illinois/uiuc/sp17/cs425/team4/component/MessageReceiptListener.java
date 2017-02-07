package edu.illinois.uiuc.sp17.cs425.team4.component;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * The listener interface for receiving notifications of message receipts.
 * 
 * @author bbassi2
 */
public interface MessageReceiptListener {
	
	/**
	 * Call this function when a message is received.
	 * @param sourceAndMsg Message and it's source.
	 * @return the response that should be sent to the incoming message.
	 */
	public Message messageReceived(Pair<Process,Message>  sourceAndMsg);
	
	/**
	 * This method is intended to be provided to listener in case the delivery of a response fails.
	 * e.g. Let's say, a message was received and messageReceived(Pair<Process,Message>  sourceAndMsg)
	 * function was called on the listener. Listener responded to this call with a message.
	 * Now the listener intended to send this response back to the process who had
	 * sent the original Pair<Process,Message> but somehow there was an failure.
	 * This method is to inform of that failure.
	 * As listener is designed to be called asynchronously, I don't know of a better way
	 * to inform this failure.
	 * In all the known implementations, we are not using this feature however.
	 * @param failedMsg Failed message.
	 * @param exception The failure that occured.
	 */
	public void notifyFailure(Pair<Pair<Process,Message>, Message> failedMsg, Exception exception);
	
}
