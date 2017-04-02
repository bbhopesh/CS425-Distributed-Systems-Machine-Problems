package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.List;
import java.util.Set;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public interface RingTopology<K> {
	
	public Set<Process> getAllProcesses();
	
	public Process mapKey(K key);
	
	// First element is closest predecessor.
	public List<Process> getPredecessors(Process p, int num);
	
	// First element is closest successor.
	public List<Process> getSuccessors(Process p, int num);
	
	public void addProcesses(Set<Process> tobeAdded);
	
	public void removeProcesses(Set<Process> tobeRemoved);
	
	public RingTopology<K> copy();
}
