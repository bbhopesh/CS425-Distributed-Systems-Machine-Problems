package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * An implementation of ping message.
 * 
 * @author bbassi2.
 */
class PingMessageImpl extends MessageBaseImpl implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1202809108361799248L;
	
	public PingMessageImpl(Process originatingSource) {
		super(MessageType.PING, originatingSource);
	}
	
	public PingMessageImpl(Process originatingSource, UUID uid) {
		super(MessageType.PING, originatingSource);
	}
	
	@Override
	public String toString() {
		return getMessageType().toString();
	}

}
