package edu.illinois.uiuc.sp17.cs425.team4.component;



import edu.illinois.uiuc.sp17.cs425.team4.model.Message;

/**
 * An interface representing multicast.
 * 
 * @author bbassi2
 */
public interface Multicast {
	/**
	 * Multicast message m
	 * @param m message to be multicasted.
	 */
	public void multicast(Message m);
	
	/**
	 * Register application with the multicast.
	 * @param appliation application to be registered.
	 * @return true if successfully registered.
	 */
	public boolean registerApplication(Application application);
}
