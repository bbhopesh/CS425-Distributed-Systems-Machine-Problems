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
	public Message createNoOpMessage(Process originatingProcess);
	
	/**
	 * Create No Op message with provided unique id.
	 * @param originatingProcess Originating process for the message.
	 * @param uId unique id of the message to be created.
	 * @return No Op message
	 */
	public Message createNoOpMessage(Process originatingProcess, UUID uId);
	
	/**
	 * Create text message with random unique id.
	 * @param text Text of the message.
	 * @param originatingProcess Originating process for the message.
	 * @return Text message
	 */
	public TextMessage createTextMessage(String text, Process originatingProcess);
	
	/**
	 * Create text message with provided unique id.
	 * @param text Text of the message.
	 * @param originatingProcess Originating process for the message.
	 * @param uId unique id of the message to be created.
	 * @return Text message
	 */
	public TextMessage createTextMessage(String text, Process originatingProcess, UUID uId);
	
	/**
	 * Create Process joined message with random unique id.
	 * @param originatingProcess Originating process for the message.
	 * @return Process joined message.
	 */
	public Message createProcessJoinedMessage(Process originatingProcess);
	
	/**
	 * Create Process joined message with provided unique id.
	 * @param originatingProcess Originating process for the message.
	 * @param uId unique id of the message to be created.
	 * @return Process joined message.
	 */
	public Message createProcessJoinedMessage(Process originatingProcess, UUID uId);
	
	/**
	 * Create Process left message with random unique id.
	 * @param originatingProcess Originating process for the message.
	 * @param uId unique id of the message to be created.
	 * @return Process left message.
	 */
	public Message createProcessLeftMessage(Process originatingProcess);
	
	/**
	 * Create Process left message with provided unique id.
	 * @param originatingProcess Originating process for the message.
	 * @param uId unique id of the message to be created.
	 * @return Process left message.
	 */
	public Message createProcessLeftMessage(Process originatingProcess, UUID uId);
	
	/**
	 * Create a ping message with random uuid.
	 * @param originatingProcess Originator of the message.
	 * @return ping message.
	 */
	public Message createPingMessage(Process originatingProcess);
	
	/**
	 * Create a ping message with provided uuid.
	 * @param originatingProcess Originator of the message.
	 * @param uId Unique identifier of the message.
	 * @return ping message.
	 */
	public Message createPingMessage(Process originatingProcess, UUID uId);
	
	/**
	 * Create ack message with random id.
	 * @param originatingProcess Originator of the message.
	 * @return ack message.
	 */
	public Message createAckMessage(Process originatingProcess);
	
	/**
	 * Create ack message with provided  uuid.
	 * @param originatingProcess Originator of the message.
	 * @param uId Unique identifier of the message.
	 * @return ack message.
	 */
	public Message createAckMessage(Process originatingProcess, UUID uId);
	
	public <K> Message createKeyReadMessage(Process originatingProcess, K key, Long timestamp);
	
	public <K> Message createKeyReadMessage(Process originatingProcess, K key, Long timestamp, UUID uId);
	
	public <K, V> Message createKeyWriteMessage(Process originatingProcess, K key, V val, Long timestamp);
	
	public <K, V> Message createKeyWriteMessage(Process originatingProcess, K key, V val, Long timestamp, UUID uId);
	
	public <V> Message createValueMessage(Process originatingProcess, V val, Long timestamp);
	
	public <V> Message createValueMessage(Process originatingProcess, V val, Long timestamp, UUID uId);
	
	public <K> Message createKeyDeleteMessage(Process originatingProcess, K key);
	
	public <K> Message createKeyDeleteMessage(Process originatingProcess, K key, UUID uId);
	
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

}
