package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.math.BigInteger;
import java.util.NavigableSet;
import java.util.Set;

import edu.illinois.uiuc.sp17.cs425.team4.component.Codec;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.HashFunction;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class HashBasedRingKVDataPartitioner<K> implements KVDataPartitioner<K> {
	private final HashFunction hashFunction;
	private final int m;
	private final GroupManager groupManager;
	private final Codec<K> keyCodec;
	private final Codec<Process> processCodec;
	private final Codec<BigInteger> intCodec;
	
	public HashBasedRingKVDataPartitioner(HashFunction hashFunction, int m,
			Codec<K> keyCodec, Codec<Process> processCodec,
			Codec<BigInteger> intCodec,
			GroupManager groupManager) {
		this.hashFunction = hashFunction;
		this.m = m;
		this.keyCodec = keyCodec;
		this.processCodec = processCodec;
		this.intCodec = intCodec;
		this.groupManager = groupManager;
	}


	
	private NavigableSet<Process> getProcessesInRingOrder() {
		// Get membership list from group manager and return in ascending order around the ring.
		return null;
	}

	@Override
	public Process getPrimaryPartition(K key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Process> getReplicas(Process process) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Process> replicaOf(Process process) {
		// TODO Auto-generated method stub
		return null;
	}
	

	
	
	
}
