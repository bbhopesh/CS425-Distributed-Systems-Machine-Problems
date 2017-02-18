package edu.illinois.uiuc.sp17.cs425.team4.component;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * An interface representing application.
 * 
 * @author bbassi2
 */
public interface Application {
	/**
	 * Deliver a message to this application.
	 * @param incomingMessage incoming message and it's source.
	 */
	public void deliver(Pair<Process, Message> incomingMessage);
}
