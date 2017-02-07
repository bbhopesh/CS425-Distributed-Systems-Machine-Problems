package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * An implementation of the Process joined message.
 * 
 * @author bbassi2
 */
class ProcessLeftMessageImpl extends MessageBaseImpl implements Serializable {

	/** Serial Version UID. */
	private static final long serialVersionUID = 8568431742044784001L;

	/**
	 * Create an instance. Leaves responsibility of assigning unique id to base class.
	 */
	public ProcessLeftMessageImpl(Process originatingSource) {
		super(MessageType.JOINED, originatingSource);
	}
	
	/**
	 * Create an instance with the provided unique identifier.
	 * @param uId unique idnetifier.
	 */
	public ProcessLeftMessageImpl(Process originatingSource,UUID uId) {
		super(MessageType.JOINED, originatingSource, uId);
	}
	
	@Override
	public String toString() {
		return getMessageType().toString();
	}
}
