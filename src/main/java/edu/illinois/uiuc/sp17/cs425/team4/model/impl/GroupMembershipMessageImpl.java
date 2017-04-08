package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.GroupMembershipMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

class GroupMembershipMessageImpl extends MessageBaseImpl implements GroupMembershipMessage, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3461976541232439661L;
	private Set<Process> groupMembers;

	public GroupMembershipMessageImpl(Process originatingSource, Set<Process> groupMembers) {
		super(MessageType.GROUP_MEMBERS, originatingSource);
		this.groupMembers = new HashSet<Process>(groupMembers);
	}
	
	public GroupMembershipMessageImpl(Process originatingSource, Set<Process> groupMembers, UUID uId) {
		super(MessageType.GROUP_MEMBERS, originatingSource, uId);
		this.groupMembers = new HashSet<Process>(groupMembers);
	}
	
	@Override
	public Set<Process> getGroupMembers() {
		return this.groupMembers;
	}

}
