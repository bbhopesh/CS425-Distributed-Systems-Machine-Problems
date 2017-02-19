package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.Set;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * A group manager. Group here means nothing but a set of processes.
 * 
 * @author bbassi2
 */
public interface GroupManager {
	
	/** 
	 * Initialize GroupManager.
	 */
	public void initialize();
	
	
	/**
	 * Returns current process.
	 * @return current process.
	 */
	public Process getMyIdentity();
	
	/**
	 * Return set of all processes in the group.
	 * @return Set of all processes in the group.
	 */
	public Set<Process> getGroupMembers();
	
	/**
	 * Register a listener who will listen to changes in group.
	 * @param groupChangeListener Group change listener.
	 * @return true if registered successfully.
	 */
	public boolean registerGroupChangeListener(GroupChangeListener groupChangeListener);
}
