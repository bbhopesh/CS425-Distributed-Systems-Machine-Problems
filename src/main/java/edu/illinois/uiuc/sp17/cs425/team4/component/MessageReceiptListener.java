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
	
}
