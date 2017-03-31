package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Comparator;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.model.KVRawOpResult;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class FailureTolerantKVDataManager<K,V> implements KVDataManager<K, V> {

	private final KVRawDataManager<K, V> rawDataManager;
	private final KVDataPartitioner<K> dataPartitioner;
	private final int requestTimeout;
	private final int R;
	private final int W;
	private final int D;

	public FailureTolerantKVDataManager(KVRawDataManager<K, V> rawDataManager,
							KVDataPartitioner<K> dataPartitioner,
							int requestTimeout,
							int R, int W, int D) {
		this.rawDataManager = rawDataManager;
		this.dataPartitioner = dataPartitioner;
		this.requestTimeout = requestTimeout;
		this.R = R;
		this.W = W;
		this.D = D;
	}
	
	@Override
	public Pair<Long, V> read(K key) {
		
		
		return null;
	}

	@Override
	public boolean write(K key, V value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Pair<Long, V> read(K key, long asOfTimestamp) {
		// TODO Auto-generated method stub
		Set<Process> partitions = getPartitions(key);
		KVRawOpResult<Pair<Long, V>> readResult = this.rawDataManager.read(key, 
														asOfTimestamp, 
														partitions, 
														this.R, 
														this.requestTimeout);
		NavigableSet<Pair<Long, V>> values = new TreeSet<Pair<Long, V>>(new Comparator<Pair<Long, V>>() {

			@Override
			public int compare(Pair<Long, V> arg0, Pair<Long, V> arg1) {
				return arg0.getLeft().compareTo(arg1.getLeft());
			}
		});
		
		//values.
		return null;
	}

	@Override
	public boolean write(K key, V value, long timestamp) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(K key) {
		// TODO Auto-generated method stub
		return false;
	}
	
	private Set<Process> getPartitions(K key) {
		Set<Process> partitions = new HashSet<Process>();
		Process primaryPartition = this.dataPartitioner.getPrimaryPartition(key);
		partitions.add(primaryPartition);
		partitions.addAll(this.dataPartitioner.getReplicas(primaryPartition));
		return partitions;
	}

}
