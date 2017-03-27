package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.Set;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public interface KVDataPartitioner<K> {
	public Process getPrimaryPartition(K key);
	
	public Set<Process> getReplicas(Process process);
	
	public Set<Process> replicaOf(Process process);
}
