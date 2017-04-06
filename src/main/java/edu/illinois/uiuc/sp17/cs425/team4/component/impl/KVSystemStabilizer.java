package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Arrays;
import java.util.HashSet;
import org.apache.log4j.Logger;

import java.util.Set;

import edu.illinois.uiuc.sp17.cs425.team4.component.GroupChangeListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.RingTopology;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import net.jcip.annotations.GuardedBy;

public class KVSystemStabilizer<K, V> implements GroupChangeListener {
	
	private final static Logger LOG = Logger.getLogger(KVSystemStabilizer.class.getName());

	@GuardedBy("this")
	private final RingTopology<K> activeRingTopology;
	@GuardedBy("this")
	private RingTopology<K> beforeFailuresRingTopology;
	private final int maxFailures;
	@GuardedBy("this")
	private final Set<Process> failures;
	@GuardedBy("this")
	private final KVRawDataManager<K, V> rawDataManager;
	private final Process myIdentity;
	private final int requestTimeout;
	private final int tryCount;

	public KVSystemStabilizer(RingTopology<K> ringTopology, int maxFailures,
			KVRawDataManager<K, V> rawDataManager, Process myIdentity, int requestTimeout,
			int tryCount) {
		// We maintain two copies of ring topology.
		// Active one always stays up to date.
		// Other one is used to view how the system looked before failures and in the old topology who is holding
		// same keys as the failed processes
		this.activeRingTopology = ringTopology;
		this.beforeFailuresRingTopology = this.activeRingTopology.copy();
		this.maxFailures = maxFailures;
		this.failures = new HashSet<Process>();
		this.rawDataManager = rawDataManager;
		this.myIdentity = myIdentity;
		this.requestTimeout = requestTimeout;
		this.tryCount = tryCount;
	}
	
	@Override
	public void processJoined(Process j) {
		throw new UnsupportedOperationException();
	}

	@Override
	@GuardedBy("this")
	public synchronized void processLeft(Process l) {
		LOG.debug(String.format("Process %s left the system", l));
		System.err.println(String.format("Process %s left the system", l.getDisplayName()));
		// Inform active ring about failure.
		this.activeRingTopology.removeProcesses(new HashSet<Process>(Arrays.asList(l)));
		// Add to failed processes.
		this.failures.add(l);
		// if number of failures are equal to max failures, then redistribute.
		if (this.failures.size() == this.maxFailures && shouldRedistributeLocalKeys()) {
			try {
				redistributeLocalKeys();
			} catch (Exception e) {
				// if code reaches here, we are screwed for demo
				LOG.error(e);
			}
			LOG.debug(String.format("Redistributing local keys because %s failed.", this.failures));
			// Prepare for future failures.
			prepareForNextRoundOfFailures();
		}
	}
	
	@GuardedBy("this")
	private boolean shouldRedistributeLocalKeys() {
		boolean shouldRedistribute = false;
		KVDataPartitioner<K> dataPartitioner = new KVRingDataPartitioner<K>
														(this.beforeFailuresRingTopology, this.maxFailures);
		for (Process failure: this.failures) {
			if (dataPartitioner.getReplicas(this.myIdentity).contains(failure) ||
					dataPartitioner.getReplicas(failure).contains(this.myIdentity)) {
				// Failed process is one of my replicas or I am replica of failed process.
				shouldRedistribute = true;
				break;
			}
		}
		return shouldRedistribute;
	}
	
	@GuardedBy("this")
	private void redistributeLocalKeys() throws Exception {
		KVDataPartitioner<K> dataPartitioner = new KVRingDataPartitioner<K>(this.activeRingTopology, this.maxFailures);
		KVDataManager<K, V> dataManager = createDataManager(dataPartitioner);
		dataManager.writeBatch(this.rawDataManager.getLocalSnapshot());
		LOG.debug("Done redistributing.");
	}

	@GuardedBy("this")
	private void prepareForNextRoundOfFailures() {
		this.beforeFailuresRingTopology = this.activeRingTopology.copy();
		// Clear failures.
		this.failures.clear();
	}
	
	private KVDataManager<K, V> createDataManager(KVDataPartitioner<K> dataPartitoner) {
		return new SimpleKVDataManager<>(this.myIdentity, this.rawDataManager, 
											dataPartitoner, this.requestTimeout, this.tryCount);
	}
}
