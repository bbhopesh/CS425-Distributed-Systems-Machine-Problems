package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;


/**
 * An implementation of the Process joined message.
 * 
 * @author bbassi2
 */
class ProcessJoiningMessageImpl extends MessageBaseImpl implements Serializable {

	/** Serial Version UID. */
	private static final long serialVersionUID = 5909311563816131916L;

	/**
	 * Create an instance. Leaves responsibility of assigning unique id to base class.
	 */
	public ProcessJoiningMessageImpl(Process originatingSource) {
		super(MessageType.JOINING, originatingSource);
	}
	
	/**
	 * Create an instance with the provided unique identifier.
	 * @param uId unique identifier.
	 */
	public ProcessJoiningMessageImpl(Process originatingSource, UUID uId) {
		super(MessageType.JOINING, originatingSource, uId);
	}
	
	@Override
	public String toString() {
		return getMessageType().toString();
	}
}
