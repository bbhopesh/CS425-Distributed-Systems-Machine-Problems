package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.io.Serializable;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;

public class MessageListenerIdentifierImpl implements MessageListenerIdentifier, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3040873711857588605L;
	private final String identifier;
	
	public MessageListenerIdentifierImpl(String identifier) {
		this.identifier =  identifier;
	}
	
	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageListenerIdentifierImpl other = (MessageListenerIdentifierImpl) obj;
		if (identifier == null) {
			if (other.identifier != null)
				return false;
		} else if (!identifier.equals(other.identifier))
			return false;
		return true;
	}
	
	

}
