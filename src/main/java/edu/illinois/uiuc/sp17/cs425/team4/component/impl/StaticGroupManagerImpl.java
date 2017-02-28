package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Collections;
import java.util.Set;

import edu.illinois.uiuc.sp17.cs425.team4.component.GroupChangeListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * An implementation of group manager which can only deal with static group.
 * In other words, this class cannot deal with changing group members over time.
 * 
 * For dynamic groups, some other implementation must be used.
 * 
 * @author bbassi2
 */
public class StaticGroupManagerImpl implements GroupManager {
	/** My identity. */
	private final Process myIdentity;
	/** Members of the group. */
	private final Set<Process> groupMembers;
	
	/**
	 * Create an instance.
	 * @param myIdentity My identity.
	 * @param groupMembers Members of the group.
	 */
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
		// No op
		return false;
	}

	@Override
	public void initialize() {
		// No op
	}

	@Override
	public void close() {
		// No op.
	}

	
	
}
