package edu.illinois.uiuc.sp17.cs425.team4.component;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * Listens to changes in the group.
 * 
 * @author bbassi2
 */
public interface GroupChangeListener {
	/**
	 * Gets called when a process joins the group.
	 * @param j joining process.
	 */
	public void processJoined(Process j);
	
	/**
	 * Gets called when a process leaves a group.
	 * @param l leaving process.
	 */
	public void processLeft(Process l);
}
