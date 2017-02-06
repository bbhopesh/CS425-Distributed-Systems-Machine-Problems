package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.configuration2.Configuration;

public interface Message extends Serializable {
	public enum MessageType {
		// Add more message types as needed.
		NO_OP, TEXT
	}
	
	public MessageType getMessageType();
	
	public UUID getUUID();
	
	public Configuration getMetadata();
}
