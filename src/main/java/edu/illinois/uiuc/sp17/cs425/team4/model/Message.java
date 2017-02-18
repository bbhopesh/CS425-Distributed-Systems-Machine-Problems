package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.configuration2.Configuration;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import net.jcip.annotations.Immutable;

/**
 * An interface to represent messages that are exchanged between various nodes.
 * 
 * @author bbassi2
 *
 */
@Immutable 
// Interface is designed with the understanding in mind that implementations will be immutable.
// If you have a good reason to make an implementation mutable, please document so.
public interface Message extends Serializable {
	public enum MessageType {
		// Add more message types as needed.
		NO_OP, // No Op message(just a placeholder)
		JOINED, // Indicating that a process has joined the system.
		LEFT, // Indicating that a process has left.
		TEXT,  // Text message.
		PING, // Ping message.
		ACK // Acknowledge message.
	}
	
	/** Get message type. */
	public MessageType getMessageType();
	
	/** Get message unique indetifier. */
	public UUID getUUID();
	
	/** Get the process where this message started. */
	public Process getOriginatingSource();
	
	/**
	 * Who is this message sent to?
	 * By who, we mean here which part of the application.
	 * Some messages could be intended for failure dtector, some for isis algo etc.
	 * @return the message listener that this message is intended for.
	 */
	public MessageListenerIdentifier getMessageListenerId();
	
	public void setMessageListenerId(MessageListenerIdentifier messageSentTo);
	
	/** Get other metadata associated with this message. */
	public Configuration getMetadata();
}
