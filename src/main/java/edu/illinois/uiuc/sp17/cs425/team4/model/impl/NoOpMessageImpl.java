package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

public class NoOpMessageImpl extends MessageBaseImpl implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 163257467419369700L;

	public NoOpMessageImpl() {
		super(MessageType.NO_OP);
	}
	
	public NoOpMessageImpl(UUID uId) {
		super(MessageType.NO_OP, uId);
	}
	
	

}
