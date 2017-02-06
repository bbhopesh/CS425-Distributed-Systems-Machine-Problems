package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;

import edu.illinois.uiuc.sp17.cs425.team4.model.Message;

abstract class MessageBaseImpl implements Message, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2082386563754210354L;
	private final MessageType messageType;
	private final UUID universallyUniqueId;
	private final Map<String,Object> metaData;
	
	public MessageBaseImpl(MessageType messageType, UUID uId) {
		this.messageType = messageType;
		this.universallyUniqueId = uId;
		this.metaData = new ConcurrentHashMap<String, Object>();
	}
	
	public MessageBaseImpl(MessageType messageType) {
		this(messageType, UUID.randomUUID());
	}
	
	@Override
	public MessageType getMessageType() {
		return messageType;
	}

	@Override
	public UUID getUUID() {
		return this.universallyUniqueId;
	}

	@Override
	public Configuration getMetadata() {
		return new MapConfiguration(this.metaData);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((messageType == null) ? 0 : messageType.hashCode());
		result = prime * result + ((universallyUniqueId == null) ? 0 : universallyUniqueId.hashCode());
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
		MessageBaseImpl other = (MessageBaseImpl) obj;
		if (messageType != other.messageType)
			return false;
		if (universallyUniqueId == null) {
			if (other.universallyUniqueId != null)
				return false;
		} else if (!universallyUniqueId.equals(other.universallyUniqueId))
			return false;
		return true;
	}

	

	

}
