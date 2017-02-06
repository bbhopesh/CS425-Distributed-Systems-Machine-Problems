package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;

import edu.illinois.uiuc.sp17.cs425.team4.model.Message;

/**
 * An abstract implementation of a message.
 * equals and hashcode only consider the UUID and no other attribute.
 * Child classes are not allowed to override these two methods.
 * 
 * @author bbassi2
 */
abstract class MessageBaseImpl implements Message, Serializable {
	
	/** Serial Version UID. */
	private static final long serialVersionUID = -2082386563754210354L;
	/** Type of message. */
	private final MessageType messageType;
	/** Unique identifier of message. */
	private final UUID universallyUniqueId;
	/** Metadata associated with the message. */
	private final Map<String,Object> metaData;
	
	/**
	 * Create an instance.
	 * @param messageType Message type.
	 * @param uId Unique identifier for this message.
	 */
	public MessageBaseImpl(MessageType messageType, UUID uId) {
		this.messageType = messageType;
		this.universallyUniqueId = uId;
		this.metaData = new ConcurrentHashMap<String, Object>();
	}
	
	/**
	 * Create an instance with a random identifier.
	 * @param messageType Message type.
	 */
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
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((universallyUniqueId == null) ? 0 : universallyUniqueId.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageBaseImpl other = (MessageBaseImpl) obj;
		if (universallyUniqueId == null) {
			if (other.universallyUniqueId != null)
				return false;
		} else if (!universallyUniqueId.equals(other.universallyUniqueId))
			return false;
		return true;
	}

	
}
