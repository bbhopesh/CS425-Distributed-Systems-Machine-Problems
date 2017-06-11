package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.component.Codec;
import edu.illinois.uiuc.sp17.cs425.team4.component.HashFunction;
import edu.illinois.uiuc.sp17.cs425.team4.component.RingTopology;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * A Cassandra like ring which uses hashing to partition data across ring.
 * 
 * @author bbassi2
 *
 * @param <K> Key type.
 */
public class CassandraLikeRing<K> implements RingTopology<K> {
	/** Logger. */
	private final static Logger LOG = Logger.getLogger(CassandraLikeRing.class.getName());
	/** Mapping of process to an integer(place on ring.) */
	private final NavigableMap<BigInteger, Process> processes;
	/** Hash function. */
	private final HashFunction hashFunction;
	/** Number of bytes the hash is truncated to before converting to a number to map on a string. */
	private final int mBytes;
	/** Key codec. */
	private final Codec<K> keyCodec;
	/** Process codec. */
	private final Codec<Process> processCodec;

	public CassandraLikeRing(Set<Process> initialProcesses,
			HashFunction hashFunction, int mBytes,
			Codec<K> keyCodec, Codec<Process> processCodec) {
		// TODO check that number of bytes produced by hash function is greater than equal to mBytes.
		this.hashFunction = hashFunction;
		this.mBytes = mBytes;
		this.keyCodec = keyCodec;
		this.processCodec = processCodec;
		
		this.processes = initializeProcesses(initialProcesses);
	}
	
	@Override
	public Set<Process> getAllProcesses() {
		return new HashSet<Process>(this.processes.values());
	}
	
	private NavigableMap<BigInteger, Process> initializeProcesses(Set<Process> initialProcesses) {
		// create a new map.
		NavigableMap<BigInteger, Process> processes = new ConcurrentSkipListMap<BigInteger, Process>();
		// add processes
		addProcesses(initialProcesses, processes);
		// Return.
		return processes;
	}
	
	private void addProcesses(Set<Process> toBeAdded, NavigableMap<BigInteger, Process> addTo) {
		for (Process p: toBeAdded) {
			addTo.put(processToPositionOnRing(p), p);
		}
	}
	
	private void removeProcesses(Set<Process> toBeRemoved, NavigableMap<BigInteger, Process> removeFrom) {
		for (Process p: toBeRemoved) {
			removeFrom.remove(processToPositionOnRing(p));
		}
	}
	
	private BigInteger processToPositionOnRing(Process process) {
		byte[] processBytes = this.processCodec.encode(process);
		// byte[] to integer.
		return hashTruncateConvert(processBytes);
	}
	
	@Override
	public Process mapKey(K key) {
		// Key to byte[]
		byte[] keyByteArray = this.keyCodec.encode(key);
		// byte[] to integer.
		BigInteger positionOnRing = hashTruncateConvert(keyByteArray);
		return processForKey(positionOnRing);
	}
	
	private Process processForKey(BigInteger positionOnRing) {
		// Clockwise Process whose key is greater than or equal to given position on ring.
		// Create a cyclic iterator which starts from positionOnRing inclusive.
		Iterator<Map.Entry<BigInteger, Process>> cyclicIt = 
				new CyclicIteration<BigInteger, Process>(this.processes, positionOnRing, false).iterator();
		// There should be atleast one process, so not checking for hasNext()
		return this.processes.get(cyclicIt.next().getKey());
	}
	
	
	private BigInteger hashTruncateConvert(byte[] bytes) {
		// Hash.
		byte[] hash = this.hashFunction.hash(bytes);
		// Truncate.
		int endExclusive = hash.length;
		int startInclusive = endExclusive - this.mBytes;
		byte[] lastMBytes = ArrayUtils.subarray(hash, startInclusive, endExclusive);
		// always a positive integer.
		return new BigInteger(1, lastMBytes);
	}

	@Override
	public List<Process> getPredecessors(Process p, int num) {
		// Getting successors from descending map is equivalent to getting predecessors from original map.
		return getSuccessors(this.processes.descendingMap(), p, num);
	}

	@Override
	public List<Process> getSuccessors(Process p, int num) {
		return getSuccessors(this.processes, p, num);
	}
	
	private List<Process> getSuccessors(NavigableMap<BigInteger, Process> processes, Process p, int num) {
		BigInteger posOnRing = processToPositionOnRing(p);
		// Create a cyclic iterator which starts from positionOnRing exclusive.
		Iterator<Map.Entry<BigInteger, Process>> cyclicIt = 
				new CyclicIteration<BigInteger, Process>(processes, posOnRing, true).iterator();
		
		int i = 0;
		List<Process> successors = new ArrayList<Process>(num);
		while (i < num && cyclicIt.hasNext()) {
			successors.add(cyclicIt.next().getValue());
			i++;
		}
		if (successors.contains(p)) {
			// If the same process got added at end because of wrapping around, remove it.
			successors.remove(p);
		}
		return successors;
	}

	@Override
	public void addProcesses(Set<Process> tobeAdded) {
		LOG.debug(String.format("Adding to ring: %s", tobeAdded));
		addProcesses(tobeAdded, this.processes);
	}

	@Override
	public void removeProcesses(Set<Process> tobeRemoved) {
		LOG.debug(String.format("Removing from ring: %s", tobeRemoved));
		removeProcesses(tobeRemoved, this.processes);
	}

	@Override
	public RingTopology<K> copy() {
		return new CassandraLikeRing<>(getAllProcesses(), 
										this.hashFunction, 
										this.mBytes,
										this.keyCodec,
										this.processCodec);
	}
	
	public Map<BigInteger, Process> getProcessMapping() {
		return Collections.unmodifiableMap(this.processes);
		
	}
}
