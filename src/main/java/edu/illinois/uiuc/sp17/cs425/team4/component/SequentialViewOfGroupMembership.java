package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.NavigableSet;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public interface SequentialViewOfGroupMembership {
	public NavigableSet<Process> getGroupMembers();
}
