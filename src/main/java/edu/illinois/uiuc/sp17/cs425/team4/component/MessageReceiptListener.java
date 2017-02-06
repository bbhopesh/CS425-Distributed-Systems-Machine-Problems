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
	 * Call this function when a message is received with an incoming connection.
	 * As the incoming connection objects for different type of connections are very different,
	 * interface has to define it as Object.
	 * Implementations for specific type of connections will have to cast to appropriate type.
	 * @param incomingConnection Incoming connection object for the particular message
	 */
	public Message messageReceived(Pair<Process,Message>  sourceAndMsg);
}
