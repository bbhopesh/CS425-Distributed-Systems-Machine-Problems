package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.Set;

public interface GroupMembershipMessage extends Message {
	
	public Set<Process> getGroupMembers();
}
