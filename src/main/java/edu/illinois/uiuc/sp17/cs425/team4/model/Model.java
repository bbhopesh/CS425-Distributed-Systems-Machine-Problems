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
	 * @param originatingSource specify the originating source.
	 * @return No Op message.
	 */
	public Message createNoOpMessage(Process originatingSource);
	
	/**
	 * Create No Op message with provided unique id.
	 * @param originatingSource specify the originating source.
	 * @param uId unique id of the message to be created.
	 * @return No Op message
	 */
	public Message createNoOpMessage(Process originatingSource, UUID uId);
	
	/**
	 * Create text message with random unique id.
	 * @param originatingSource specify the originating source.
	 * @param text Text of the message.
	 * @return Text message
	 */
	public TextMessage createTextMessage(Process originatingSource, String text);
	
	/**
	 * Create text message with provided unique id.
	 * @param originatingSource specify the originating source.
	 * @param text Text of the message.
	 * @param uId unique id of the message to be created.
	 * @return Text message
	 */
	public TextMessage createTextMessage(Process originatingSource, String text, UUID uId);
	
	/**
	 * Create Process joined message with random unique id.
	 * @param originatingSource specify the originating source.
	 * @return Process joined message.
	 */
	public Message createProcessJoinedMessage(Process originatingSource);
	
	/**
	 * Create Process joined message with provided unique id.
	 * @param originatingSource specify the originating source.
	 * @param uId unique id of the message to be created.
	 * @return Process joined message.
	 */
	public Message createProcessJoinedMessage(Process originatingSource, UUID uId);
	
	/**
	 * Create Process left message with random unique id.
	 * @param uId unique id of the message to be created.
	 * @return Process left message.
	 */
	public Message createProcessLeftMessage(Process originatingSource);
	
	/**
	 * Create Process left message with provided unique id.
	 * @param originatingSource specify the originating source.
	 * @param uId unique id of the message to be created.
	 * @return Process left message.
	 */
	public Message createProcessLeftMessage(Process originatingSource, UUID uId);
	
	/**
	 * Create process with provided unique id.
	 * @param inetAddress InetAddress
	 * @param port Port
	 * @return Process.
	 */
	public Process createProcess(InetAddress inetAddress, int port);
	
	/**
	 * Create process with provided unique id.
	 * @param inetAddress InetAddress
	 * @param port Port
	 * @param uId Unique id of the process to be created
	 * @return Process.
	 */
	public Process createProcess(InetAddress inetAddress, int port, UUID uId);
}
