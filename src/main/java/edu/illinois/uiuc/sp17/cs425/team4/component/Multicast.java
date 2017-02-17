package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * An interface representing multicast.
 * 
 * @author bbassi2
 */
public interface Multicast {
	/**
	 * Multicast message m
	 * @param m message to be multicasted.
	 * @return A list of process to which multicast couldn't be sent along with the errors that occured.
	 */
	public List<Pair<Process, Exception>> multicast(Message m);
	
	/**
	 * Register application with the multicast.
	 * @param appliation application to be registered.
	 * @return true if successfully registered.
	 */
	public boolean registerApplication(Application appliation);
}
