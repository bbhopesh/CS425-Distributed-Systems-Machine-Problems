package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.model.KVAsyncOpResult;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class SimpleKVDataManager<K,V> implements KVDataManager<K, V> {

	private final static Logger LOG = Logger.getLogger(SimpleKVDataManager.class.getName());
	
	private final KVRawDataManager<K, V> rawDataManager;
	private final KVDataPartitioner<K> dataPartitioner;
	private final int requestTimeout;
	private final int W;
	private final int R;
	private final int D;
	private final int tryCount;

	public SimpleKVDataManager(KVRawDataManager<K, V> rawDataManager,
							KVDataPartitioner<K> dataPartitioner,
							int requestTimeout, int tryCount) {
		this.rawDataManager = rawDataManager;
		this.dataPartitioner = dataPartitioner;
		// Plus 1 for primary partition.
		this.W = this.dataPartitioner.numberOfReplicas() + 1;
		// Read from 1.
		this.R = 1;
		// delete from all.
		this.D = this.W;
		
		this.requestTimeout = requestTimeout;
		// Retry read/write/delete these many times if failures keep occuring. 
		this.tryCount  = tryCount;
		// TODO We might want to sleep for sometime between each retry, so that if something
		// is wrong in the system, it gets time to recover.
		// Not doing it for now.
	}
	
	
	@Override
	public Pair<Long, V> read(K key) {
		return read(key, System.currentTimeMillis());
	}

	@Override
	public boolean write(K key, V value) {
		return write(key, value, System.currentTimeMillis());
	}

	@Override
	public Pair<Long, V> read(K key, long asOfTimestamp) {
		KVAsyncOpResult<Pair<Long, V>> readResult;
		int tries = 0;
		// Try reading until we succeed.
		do {
			readResult = readOnce(key, asOfTimestamp);
			tries += 1;
		} while(!readResult.succeeded() && tries < this.tryCount);
		
		// Return
		if (readResult.succeeded()) {
			LOG.debug("Read succeeded.");
			return getLatest(readResult);
		} else {
			LOG.debug("Read failed.");
			// TODO It's really not clear to me about what should be returned if we don't succeed even after retries.
			// One thought was to take all the exceptions in raw result, wrap it in one and throw, and let caller deal with it.
			// Other thought was to just return null because this class is to be used by the user interface directly.
			// Going with the second approach for now.
			// If such a situation arises, then we are going to get penalty for MP anyway, so who cares which approach I really use :)
			return null;
		}
	}
	
	private KVAsyncOpResult<Pair<Long, V>> readOnce(K key, long asOfTimestamp) {
		// Get partitions.
		Set<Process> partitions = getPartitions(key);
		// Read from atleast R of them.
		KVAsyncOpResult<Pair<Long, V>> readResult = this.rawDataManager.read(key, 
														asOfTimestamp, 
														partitions, 
														this.R, 
														this.requestTimeout);
		// Return.
		return readResult;
	}
	
	private Pair<Long, V> getLatest(KVAsyncOpResult<Pair<Long, V>> rawRes) {
		NavigableMap<Long, V> values = new TreeMap<Long, V>();
		
		for (Entry<Process, Pair<Long, V>> entry: rawRes.completed().entrySet()) {
			Pair<Long, V> val = entry.getValue();
			if (val != null && val.getLeft() != null) {
				values.put(val.getLeft(), val.getRight());
			}
		}
		Entry<Long, V> highestEntry = values.lastEntry();
		if (highestEntry != null) {
			return Pair.of(highestEntry.getKey(), highestEntry.getValue());
		} else {
			return null;
		}
		
	}

	@Override
	public boolean write(K key, V value, long timestamp) {
		KVAsyncOpResult<Boolean> writeResult;
		int tries = 0;
		// Try writing until we succeed.
		do {
			writeResult = writeOnce(key, value, timestamp);
			tries += 1;
		} while(!writeResult.succeeded() && tries < this.tryCount);
		
		// Return
		if (writeResult.succeeded()) {
			return true;
		} else {
			// TODO It's really not clear to me about what should be returned if we don't succeed even after retries.
			// One thought was to take all the exceptions in raw result, wrap it in one and throw, and let caller deal with it.
			// Other thought was to just return false because this class is to be used by the user interface directly
			// and if we don deal with exceptions here, we have to deal in user interface.
			// Going with the second approach for now.
			// This was the reason return type was kept as boolean in KVDataManager interface.
			// If such a situation arises, then we are going to get penalty for MP anyway, so who cares which approach I really use :)
			return false;
		}
	}
	
	public KVAsyncOpResult<Boolean> writeOnce(K key, V value, long timestamp) {
		// Get partitions.
		Set<Process> partitions = getPartitions(key);
		// Write to atleast W of them.
		KVAsyncOpResult<Boolean> writeResult = this.rawDataManager.write(key, 
														value, 
														timestamp,
														partitions,
														this.W,
														this.requestTimeout);
		// Return.
		return writeResult;
	}

	@Override
	public boolean delete(K key) {
		KVAsyncOpResult<Boolean> deleteResult;
		int tries = 0;
		// Try writing until we succeed.
		do {
			deleteResult = deleteOnce(key);
			tries += 1;
		} while(!deleteResult.succeeded() && tries < this.tryCount);
		
		// Return
		if (deleteResult.succeeded()) {
			return true;
		} else {
			// TODO It's really not clear to me about what should be returned if we don't succeed even after retries.
			// One thought was to take all the exceptions in raw result, wrap it in one and throw, and let caller deal with it.
			// Other thought was to just return false because this class is to be used by the user interface directly
			// and if we don deal with exceptions here, we have to deal in user interface.
			// Going with the second approach for now.
			// This was the reason return type was kept as boolean in KVDataManager interface.
			// If such a situation arises, then we are going to get penalty for MP anyway, so who cares which approach I really use :)
			return false;
		}
	}
	
	public KVAsyncOpResult<Boolean> deleteOnce(K key) {
		// Get partitions.
		Set<Process> partitions = getPartitions(key);
		// Delete from atleast D of partitions.
		KVAsyncOpResult<Boolean> deleteResult = this.rawDataManager.delete(key, 
													partitions, 
													this.D, 
													this.requestTimeout);
		// Return.
		return deleteResult;
	}
	
	private Set<Process> getPartitions(K key) {
		Set<Process> partitions = new HashSet<Process>();
		Process primaryPartition = this.dataPartitioner.getPrimaryPartition(key);
		partitions.add(primaryPartition);
		partitions.addAll(this.dataPartitioner.getReplicas(primaryPartition));
		return partitions;
	}


	@Override
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot() {
		return this.rawDataManager.getLocalSnapshot();
	}
}
