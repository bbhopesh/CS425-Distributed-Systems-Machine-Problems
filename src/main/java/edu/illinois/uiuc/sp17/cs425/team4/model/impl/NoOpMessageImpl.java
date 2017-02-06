package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

/**
 * An implementation of the NO_OP message.
 * 
 * @author bbassi2
 */
public class NoOpMessageImpl extends MessageBaseImpl implements Serializable {

	/** Serial Version UID. */
	private static final long serialVersionUID = 163257467419369700L;

	/**
	 * Create an instance. Leaves responsibility of assigning unique id to base class.
	 */
	public NoOpMessageImpl() {
		super(MessageType.NO_OP);
	}
	
	/**
	 * Create an instance with the provided unique identifier.
	 * @param uId unique idnetifier.
	 */
	public NoOpMessageImpl(UUID uId) {
		super(MessageType.NO_OP, uId);
	}
	
	@Override
	public String toString() {
		return getMessageType().toString();
	}
}
