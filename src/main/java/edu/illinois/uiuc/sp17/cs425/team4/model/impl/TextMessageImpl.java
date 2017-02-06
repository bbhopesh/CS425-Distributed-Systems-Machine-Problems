package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.TextMessage;

public class TextMessageImpl extends MessageBaseImpl implements TextMessage,Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3806320361615887426L;
	private final String text;

	public TextMessageImpl(String text) {
		super(MessageType.TEXT);
		this.text = text;
	}
	
	public TextMessageImpl(String text, UUID uId) {
		super(MessageType.TEXT, uId);
		this.text = text;
	}

	@Override
	public String getText() {
		return this.text;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		TextMessageImpl other = (TextMessageImpl) obj;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return this.text;
	}
	
	
}
