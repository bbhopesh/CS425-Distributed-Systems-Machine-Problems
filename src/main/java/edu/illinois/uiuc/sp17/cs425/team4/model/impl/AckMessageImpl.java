package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * Implementation of ack message.
 * 
 * @author bbassi2
 */
class AckMessageImpl extends MessageBaseImpl implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1202809108361799248L;
	
	public AckMessageImpl(Process originatingSource) {
		super(MessageType.ACK, originatingSource);
	}
	
	public AckMessageImpl(Process originatingSource, UUID uid) {
		super(MessageType.ACK, originatingSource);
	}
	
	@Override
	public String toString() {
		return getMessageType().toString();
	}

}
