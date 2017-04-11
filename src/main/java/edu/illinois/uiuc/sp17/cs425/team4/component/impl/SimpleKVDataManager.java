package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.model.KVAsyncOpResult;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.util.KVUtils;

public class SimpleKVDataManager<K,V> implements KVDataManager<K, V> {

	private final static Logger LOG = Logger.getLogger(SimpleKVDataManager.class.getName());
	
	private final KVRawDataManager<K, V> rawDataManager;
	private final KVDataPartitioner<K> dataPartitioner;
	private final int requestTimeout;
	private final int W;
	private final int R;
	private final int tryCount;
	private final Process myIdentity;

	public SimpleKVDataManager(Process myIdentity,
							KVRawDataManager<K, V> rawDataManager,
							KVDataPartitioner<K> dataPartitioner,
							int requestTimeout, int tryCount) {
		this.myIdentity = myIdentity;
		this.rawDataManager = rawDataManager;
		this.dataPartitioner = dataPartitioner;
		// Plus 1 for primary partition.
		this.W = this.dataPartitioner.numberOfReplicas() + 1;
		// Read from 1.
		this.R = 1;
		
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
			LOG.debug(String.format("Read failed even after %s tries.", this.tryCount));
			//System.err.println(String.format("Read failed even after %s tries.", this.tryCount));
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
		Set<Process> partitions = KVUtils.getAllPartitions(key, this.dataPartitioner);
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
			LOG.debug(String.format("Write failed even after %s tries.", this.tryCount));
			//System.err.println(String.format("Write failed even after %s tries.", this.tryCount));
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
	
	private KVAsyncOpResult<Boolean> writeOnce(K key, V value, long timestamp) {
		// Get partitions.
		Set<Process> partitions = KVUtils.getAllPartitions(key, this.dataPartitioner);
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
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot() {
		return this.rawDataManager.getLocalSnapshot();
	}


	@Override
	public Map<K, NavigableMap<Long, V>> readMyData() {
		KVAsyncOpResult<Map<K, NavigableMap<Long, V>>> readResult;
		int tries = 0;
		// Try reading until we succeed.
		do {
			readResult = readMyKeysOnce();
			tries += 1;
		} while(!readResult.succeeded() && tries < this.tryCount);
		
		// Return
		if (readResult.succeeded()) {
			LOG.debug("Read succeeded.");
			Map<K, NavigableMap<Long, V>> myData = new HashMap<>();
			for (Entry<Process, Map<K, NavigableMap<Long, V>>> entry: readResult.completed().entrySet()) {
				filterAndAddMyKeys(entry.getValue(), myData);
			}
			return myData;
		} else {
			LOG.debug(String.format("Read for my keys failed even after %s tries.", this.tryCount));
			//System.err.println(String.format("Read for my keys failed even after %s tries.", this.tryCount));
			// TODO It's really not clear to me about what should be returned if we don't succeed even after retries.
			// One thought was to take all the exceptions in raw result, wrap it in one and throw, and let caller deal with it.
			// Other thought was to just return null because this class is to be used by the user interface directly.
			// Going with the second approach for now.
			// If such a situation arises, then we are going to get penalty for MP anyway, so who cares which approach I really use :)
			return null;
		}
	}
	
	public KVAsyncOpResult<Map<K, NavigableMap<Long, V>>> readMyKeysOnce() {
		Set<Process> commonProcesses = this.dataPartitioner.getReplicas(this.myIdentity);
		commonProcesses.addAll(this.dataPartitioner.replicaOf(this.myIdentity));
		return this.rawDataManager.readBatch(commonProcesses, this.requestTimeout);
	}


	@Override
	public boolean writeBatch(Map<K, NavigableMap<Long, V>> data) {
		KVAsyncOpResult<Boolean> writeResult;
		int tries = 0;
		// Try writing until we succeed.
		do {
			Map<Process, Map<K, NavigableMap<Long, V>>> processWiseData = 
					KVUtils.segregateDataProcessWise(data, this.dataPartitioner);
			writeResult = writeBatchOnce(processWiseData);
			tries += 1;
			data = getUnsuccessfulWrites(writeResult, processWiseData);
		} while(!writeResult.succeeded() && tries < this.tryCount);
		
		// Return
		if (writeResult.succeeded()) {
			return true;
		} else {
			LOG.debug(String.format("Batch Write failed even after %s tries.", this.tryCount));
			//System.err.println(String.format("Batch Write failed even after %s tries.", this.tryCount));
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
	
	private KVAsyncOpResult<Boolean> writeBatchOnce(Map<Process, Map<K, NavigableMap<Long, V>>> processWiseData) {
		return this.rawDataManager.writeBatch(processWiseData, this.requestTimeout);
	}
	
	private Map<K, NavigableMap<Long, V>> getUnsuccessfulWrites(KVAsyncOpResult<Boolean> writeResult,
			Map<Process, Map<K, NavigableMap<Long, V>>> processWiseData) {
		Map<K, NavigableMap<Long, V>> unsuccessfulWrites = new HashMap<>();
		
		for (Entry<Process, Boolean> completed: writeResult.completed().entrySet()) {
			Process p = completed.getKey();
			Boolean success = completed.getValue();
			if (!success) {
				addAll(processWiseData.get(p), unsuccessfulWrites);
			}
		}
		
		for (Entry<Process, Throwable> failures: writeResult.failures().entrySet()) {
			Process p = failures.getKey();
			addAll(processWiseData.get(p), unsuccessfulWrites);
		}
		
		return unsuccessfulWrites;
	}


	private void addAll(Map<K, NavigableMap<Long, V>> add, Map<K, NavigableMap<Long, V>> addTo) {
		for (Entry<K, NavigableMap<Long, V>> addEntry: add.entrySet()) {
			K key = addEntry.getKey();
			NavigableMap<Long, V> values = addEntry.getValue();
			NavigableMap<Long, V> existingValues = addTo.get(key);
			if (existingValues == null) {
				existingValues = new TreeMap<>(KVUtils.createDecLongComp());
				addTo.put(key, existingValues);
			}
			existingValues.putAll(values);
		}
		
	}
	
	private void filterAndAddMyKeys(Map<K, NavigableMap<Long, V>> add, Map<K, NavigableMap<Long, V>> addTo) {
		for (Entry<K, NavigableMap<Long, V>> addEntry: add.entrySet()) {
			K key = addEntry.getKey();
			// If I am not partition for the key, ignore it.
			if (!KVUtils.getAllPartitions(key, dataPartitioner).contains(this.myIdentity)) continue;
			
			NavigableMap<Long, V> values = addEntry.getValue();
			NavigableMap<Long, V> existingValues = addTo.get(key);
			if (existingValues == null) {
				existingValues = new TreeMap<>(KVUtils.createDecLongComp());
				addTo.put(key, existingValues);
			}
			existingValues.putAll(values);
		}
		
	}


	@Override
	public Map<K, NavigableMap<Long, V>> readBatch(Map<K, NavigableSet<Long>> keys) {
		KVAsyncOpResult<Map<K, NavigableMap<Long, V>>> readResult;
		int tries = 0;
		// Try reading until we succeed.
		do {
			readResult = readBatchOnce(keys);
			tries += 1;
		} while(!readResult.succeeded() && tries < this.tryCount);
		
		// Return
		if (readResult.succeeded()) {
			LOG.debug("Read succeeded.");
			return extractValuesForTimestamps(keys, readResult);
		} else {
			LOG.debug(String.format("Batch Read failed even after %s tries.", this.tryCount));
			//System.err.println(String.format("Batch Read failed even after %s tries.", this.tryCount));
			// TODO It's really not clear to me about what should be returned if we don't succeed even after retries.
			// One thought was to take all the exceptions in raw result, wrap it in one and throw, and let caller deal with it.
			// Other thought was to just return null because this class is to be used by the user interface directly.
			// Going with the second approach for now.
			// If such a situation arises, then we are going to get penalty for MP anyway, so who cares which approach I really use :)
			return null;
		}
	}
	
	private Map<K, NavigableMap<Long, V>> extractValuesForTimestamps(Map<K, NavigableSet<Long>> keys, 
			KVAsyncOpResult<Map<K, NavigableMap<Long, V>>> readResult) {
		Map<K, NavigableMap<Long, V>> rawData = new HashMap<>();
		
		for (Entry<Process, Map<K, NavigableMap<Long, V>>> entry: readResult.completed().entrySet()) {
			addAll(entry.getValue(), rawData);
		}
		
		Map<K, NavigableMap<Long, V>> finalResult = new HashMap<>();
		for (Entry<K, NavigableSet<Long>> keyEntry: keys.entrySet()) {
			K key = keyEntry.getKey();
			if (rawData.containsKey(key)) {
				NavigableMap<Long, V> rawalues = rawData.get(key);
				NavigableMap<Long, V> values = new TreeMap<>(KVUtils.createDecLongComp());
				for (Long timestamp: keyEntry.getValue()) {
					Entry<Long, V> valueEntry = rawalues.ceilingEntry(timestamp);
					V v = null;
					if (valueEntry != null) {
						v = valueEntry.getValue();
					}
					values.put(timestamp, v);
				}
				finalResult.put(key, values);
			}
		}
		return finalResult;
	}
	
	private KVAsyncOpResult<Map<K, NavigableMap<Long, V>>> readBatchOnce(Map<K, NavigableSet<Long>> keys) {
		Map<Process, Map<K, NavigableSet<Long>>> processWiseKeys = 
				KVUtils.segregateDataProcessWise(keys, this.dataPartitioner);
		Map<Process, Set<K>> perProcessKeys = new HashMap<>();
		for (Entry<Process, Map<K, NavigableSet<Long>>> entry: processWiseKeys.entrySet()) {
			perProcessKeys.put(entry.getKey(), new HashSet<K>(entry.getValue().keySet()));
		}
		return this.rawDataManager.readBatch(perProcessKeys, this.requestTimeout);
	}


	@Override
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot(Long asOfTimestamp) {
		return this.rawDataManager.getLocalSnapshot(asOfTimestamp);
	}

}
