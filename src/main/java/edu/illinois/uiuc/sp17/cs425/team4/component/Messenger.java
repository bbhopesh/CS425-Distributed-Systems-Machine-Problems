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
	 * Send message to the provided address.
	 * Configuration object can be used to pass certain parameters to the implementations.
	 * The configuration is made part of the interface instead of leaving to implementations so that
	 * each send can be configured differently if required.
	 * Implementations should treat null configuration as empty configuration.
	 * @param sendTo Send the message to this address. 
	 * @param msg Message to be sent.
	 * @param configuration configure send using this argument.
	 * @return The response from the server.
	 */
	public Message send(Pair<Process, Message> dstnAndMsg);
	
	/**
	 * Register the listener that should be called when a message is received for this process.
	 * The interface doesn't guarantee the order in which listeners are called if multiple are registered.
	 * 
	 * Ideally, listeners passed to this function should be stateless
	 * because same listener would be called multiple times on receipt of multiple messages.  
	 * @param listener register this listener.
	 * @return true if register successfully false otherwise.
	 */
	public boolean registerListener(MessageReceiptListener listener);
	
	/**
	 * A builder interface for messenger.
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
