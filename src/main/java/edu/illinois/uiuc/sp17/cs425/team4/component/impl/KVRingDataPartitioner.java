package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.component.RingTopology;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class KVRingDataPartitioner<K> implements KVDataPartitioner<K> {
	
	private final RingTopology<K> ringTopology;
	private final int numFailures;

	public KVRingDataPartitioner(RingTopology<K> ringTopology, int numFailures) {
		this.ringTopology = ringTopology;
		this.numFailures = numFailures;
	}
	
	
	@Override
	public Process getPrimaryPartition(K key) {
		return this.ringTopology.mapKey(key);
	}

	@Override
	public Set<Process> getReplicas(Process process) {
		int predecessors = numberOfReplicas()/2;
		int successors = numberOfReplicas() - predecessors;
		Set<Process> replicas = new HashSet<>(successors + predecessors);
		// Add predecessors.
		replicas.addAll(this.ringTopology.getPredecessors(process, predecessors));
		// Add successors.
		replicas.addAll(this.ringTopology.getSuccessors(process, successors));
		// Return 
		return replicas;
	}

	@Override
	public Set<Process> replicaOf(Process process) {
		Set<Process> replicaOf =  this.ringTopology.getAllProcesses();
		Iterator<Process> it = replicaOf.iterator();
		// Get replicas of each process.
		// If the provided process is not a replica of some process,  then remove that process from set.
		while (it.hasNext()) {
			Process p = it.next();
			Set<Process> replicas = getReplicas(p);
			if (!replicas.contains(p)) {
				it.remove();
			}
		}
		
		return replicaOf;
	}

	@Override
	public int numberOfReplicas() {
		return this.numFailures;
	}

}
