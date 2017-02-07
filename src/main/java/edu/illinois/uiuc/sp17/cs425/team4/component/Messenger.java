package edu.illinois.uiuc.sp17.cs425.team4.component;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * Basic interface to send messages and respond to incoming messages.
 * Intention of the interface is to decouple protocol/application code from the transmission protocol used.
 * 
 * @author bbassi2
 */
public interface Messenger {
	
	/** 
	 * Initialize messenger.
	 * This method should be called before calling any other method on the messenger.
	 */
	public void initialize();
	
	/**
	 * Send message to the given destination.
	 * @param dstnAndMsg Pair containing message and it's destination.
	 * @return The reply of the sent message.
	 */
	// TODO Add timeout argument to send.
	public Message send(Pair<Process, Message> dstnAndMsg);
	
	
	/**
	 * Register the listener that should be called when a message is received for this process.
	 * The interface doesn't specify the order in which listeners are called if multiple are registered.
	 * Neither it does specify minimum and maximum number of listeners that can be registered.
	 * 
	 * Ideally, listeners passed to this function should be stateless
	 * because same listener would be called multiple times on receipt of multiple messages.  
	 * @param listener register this listener.
	 * @return true if register successfully false otherwise.
	 */
	public boolean registerListener(MessageReceiptListener listener);
	
	/**
	 * A builder interface for messenger.
	 * 
	 * @author bbassi2
	 */
	public static interface Builder {
		
		/**
		 * Returns an instance of a messenger.
		 * @return messenger.
		 * @throws Exception if there is some problem in building messenger.
		 */
		public Messenger build() throws Exception;
	}
	
}