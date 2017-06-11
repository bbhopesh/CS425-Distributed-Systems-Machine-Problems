package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.Set;

/** 
 * Message containing group members.
 * 
 * @author bbassi2
 *
 */
public interface GroupMembershipMessage extends Message {
	
	/**
	 * Get group members.
	 * @return set of group members.
	 */
	public Set<Process> getGroupMembers();
}
