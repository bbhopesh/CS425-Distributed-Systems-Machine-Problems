package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.configuration2.Configuration;

/**
 * An interface to represent messages that are exchanged between various nodes.
 * 
 * @author bbassi2
 *
 */
public interface Message extends Serializable {
	public enum MessageType {
		// Add more message types as needed.
		NO_OP, // No Op message(just a placeholder)
		TEXT  // Text message.
	}
	
	/** Get message type. */
	public MessageType getMessageType();
	
	/** Get message unique indetifier. */
	public UUID getUUID();
	
	/** Get other metadata associated with this message. */
	public Configuration getMetadata();
}
