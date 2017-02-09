package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Factory class for various model elements.
 * 
 * @author bbassi2
 */
public interface Model {
	/**
	 * Create No Op message with random unique id.
	 * @return No Op message.
	 */
	public Message createNoOpMessage();
	
	/**
	 * Create No Op message with provided unique id.
	 * @param uId unique id of the message to be created.
	 * @return No Op message
	 */
	public Message createNoOpMessage(UUID uId);
	
	/**
	 * Create text message with random unique id.
	 * @param text Text of the message.
	 * @return Text message
	 */
	public TextMessage createTextMessage(String text);
	
	/**
	 * Create text message with provided unique id.
	 * @param text Text of the message.
	 * @param uId unique id of the message to be created.
	 * @return Text message
	 */
	public TextMessage createTextMessage(String text, UUID uId);
	
	/**
	 * Create Process joined message with random unique id.
	 * @return Process joined message.
	 */
	public Message createProcessJoinedMessage();
	
	/**
	 * Create Process joined message with provided unique id.
	 * @param uId unique id of the message to be created.
	 * @return Process joined message.
	 */
	public Message createProcessJoinedMessage(UUID uId);
	
	/**
	 * Create Process left message with random unique id.
	 * @param uId unique id of the message to be created.
	 * @return Process left message.
	 */
	public Message createProcessLeftMessage();
	
	/**
	 * Create Process left message with provided unique id.
	 * @param uId unique id of the message to be created.
	 * @return Process left message.
	 */
	public Message createProcessLeftMessage(UUID uId);
	
	/**
	 * Create process with provided unique id.
	 * @param inetAddress InetAddress
	 * @param port Port
	 * @param displayName Display name of the process.
	 * @return Process.
	 */
	public Process createProcess(InetAddress inetAddress, int port, String displayName);
	
	/**
	 * Create process with provided unique id.
	 * @param inetAddress InetAddress
	 * @param port Port
	 * @param displayName Display name of the process.
	 * @param uId Unique id of the process to be created
	 * @return Process.
	 */
	public Process createProcess(InetAddress inetAddress, int port, String displayName, UUID uId);
	
	/** 
	 * Set which process should be stamped on various model objects as "this" process.
	 * @param myIdentity process that should be stamped on various model objects as "this" process.
	 */
	public void setMyIdentity(Process myIdentity);

	/**
	 * Check if this model contains the input process
	 * @param proc the input process to check against this.myIdentity
	 * @return boolean
	 */
	public boolean containsSameProcess(Process proc);
}
