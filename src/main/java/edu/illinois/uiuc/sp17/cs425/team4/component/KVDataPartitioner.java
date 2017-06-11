package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.Set;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * Data partitioner interface for the kv store.
 * @author bbassi2.
 *
 * @param <K> Key type.
 */
public interface KVDataPartitioner<K> {
	/**
	 * Get primary partition of the key.
	 * @param key Key.
	 * @return the primary Process which owns this key.
	 */
	public Process getPrimaryPartition(K key);
	
	/**
	 * Get replicas which will act as backup for the provided process.
	 * @param process Process whose replicas need to be returned.
	 * @return The replicas of the given process.
	 */
	public Set<Process> getReplicas(Process process);
	
	/**
	 * Get all processes whose replica is the provided process.
	 * @param process Process.
	 * @return 
	 */
	public Set<Process> replicaOf(Process process);
	
	/**
	 * Get number of replicas that any process has. All processes have same number of replicas.
	 * @return Number of replicas for a process.
	 */
	public int numberOfReplicas();
}
