package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Collections;
import java.util.Set;

import edu.illinois.uiuc.sp17.cs425.team4.component.GroupChangeListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class StaticGroupManagerImpl implements GroupManager {
	private final Process myIdentity;
	
	private final Set<Process> groupMembers;
	
	
	public StaticGroupManagerImpl(Process myIdentity, Set<Process> groupMembers) {
		this.myIdentity = myIdentity;
		this.groupMembers = Collections.unmodifiableSet(groupMembers);
	}

	@Override
	public Process getMyIdentity() {
		return this.myIdentity;
	}

	@Override
	public Set<Process> getGroupMembers() {
		return this.groupMembers;
	}

	@Override
	public boolean registerGroupChangeListener(GroupChangeListener groupChangeListener) {
		// TODO for second check-point.
		return false;
	}

	
	
}
