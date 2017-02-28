package edu.illinois.uiuc.sp17.cs425.team4.model;

/** 
 * Interface that represents a text message exchanged between processes. 
 * 
 * @author bbassi2
 */
public interface TextMessage extends Message {
	/**
	 * Get text.
	 * @return text of the message.
	 */
	public String getText();
}
