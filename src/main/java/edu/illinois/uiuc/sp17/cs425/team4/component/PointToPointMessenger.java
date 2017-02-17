package edu.illinois.uiuc.sp17.cs425.team4.component;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public interface PointToPointMessenger {
	/**
	 * Send message to the given destination.
	 * @param dstnAndMsg Pair containing message and it's destination.
	 * @return The reply of the sent message.
	 * @throws ContexedRuntimeException if there is a problem in sending the message.
	 */
	// TODO Add timeout argument to send.
	public Message send(Pair<Process, Message> dstnAndMsg) throws ContextedRuntimeException;
}
