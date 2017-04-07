package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.model.KVAsyncOpResult;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public interface KVRawDataManager<K, V> {
	
	public KVAsyncOpResult<Pair<Long, V>> read(K key, long asOfTimestamp, Set<Process> readFrom, int R, int requestTimeout);
	
	public KVAsyncOpResult<Boolean> write(K key, V value, long timestamp, Set<Process> writeTo, int W, int requestTimeout);
	
	public KVAsyncOpResult<Boolean> delete(K key, Set<Process> deleteFrom, int D, int requestTimeout);
	
	public KVAsyncOpResult<Boolean> writeBatch(Map<Process, Map<K, NavigableMap<Long, V>>> data, int requestTimeout);
	
	public KVAsyncOpResult<Map<K, NavigableMap<Long, V>>> readBatch(Map<Process, Set<K>> perProcessKeys, int requestTimeout);
	
	public KVAsyncOpResult<Map<K, NavigableMap<Long, V>>> readBatch(Set<Process> readFrom, int requestTimeout);
	
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot();
	
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot(Long asOfTimestamp);
}
