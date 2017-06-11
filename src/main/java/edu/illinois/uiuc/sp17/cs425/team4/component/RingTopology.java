package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.List;
import java.util.Set;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * Logical ring topology of processes for the key-value store.
 * 
 * @author bbassi2.
 *
 * @param <K> Key type.
 */
public interface RingTopology<K> {
	
	/**
	 * All processes in the ring.
	 * @return Set of all processes.
	 */
	public Set<Process> getAllProcesses();
	
	/**
	 * Maps a key to a process.
	 * @param key Key.
	 * @return Processes mapped to the given key.
	 */
	public Process mapKey(K key);
	
	/**
	 * Get "num" predecessors of the given process.
	 * @param p Process.
	 * @param num Number of predecessors.
	 * @return List of predecessors. First element of list is the closest predecessor and so on.
	 */
	public List<Process> getPredecessors(Process p, int num);
	
	/**
	 * Get "num" successors of the given process.
	 * @param p Process.
	 * @param num Number of successors.
	 * @return List of successors. First element of list is the closest successor and so on.
	 */
	public List<Process> getSuccessors(Process p, int num);
	
	/**
	 * Add these processes to the ring.
	 * @param tobeAdded Processes to be added.
	 */
	public void addProcesses(Set<Process> tobeAdded);
	
	/**
	 * Remove these processes from the ring.
	 * @param tobeRemoved Processes to be removed.
	 */
	public void removeProcesses(Set<Process> tobeRemoved);
	
	/**
	 * Copy the ring. Implementations should document if they are making shallow or deep company.
	 * @return Ring topology.
	 */
	public RingTopology<K> copy();
}
