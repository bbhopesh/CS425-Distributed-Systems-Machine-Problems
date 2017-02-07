package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.TextMessage;

/**
 * An implementation of the text message.
 * 
 * @author bbassi2
 */
class TextMessageImpl extends MessageBaseImpl implements TextMessage,Serializable {

	/** Serial Version UID. */
	private static final long serialVersionUID = 3806320361615887426L;
	/** Text message. */
	private final String text;

	/**
	 * Create an instance. 
	 * Responsibility of assigning unique identifier is left to base class.
	 * @param text text message.
	 */
	public TextMessageImpl(Process originatingSource, String text) {
		super(MessageType.TEXT, originatingSource);
		this.text = text;
	}
	
	/**
	 * Create an instance.
	 * @param text text message.
	 * @param uId Unique identifier of the message.
	 */
	public TextMessageImpl(Process originatingSource, String text, UUID uId) {
		super(MessageType.TEXT, originatingSource, uId);
		this.text = text;
	}

	@Override
	public String getText() {
		return this.text;
	}
	
	@Override
	public String toString() {
		return this.text;
	}
	
	
}
