package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public interface KVDataManager<K, V> {

	public Pair<Long, V> read(K key);
	
	public boolean write(K key, V value);
	
	public Pair<Long, V> read(K key, long asOfTimestamp);
	
	public boolean write(K key, V value, long timestamp);
	
	public Map<K, NavigableMap<Long, V>> readMyData(); 
	
	public boolean writeBatch(Map<K, NavigableMap<Long, V>> data);
	
	public Map<K, Pair<Long, V>> readBatch(Set<K> keys, long asOfTimestamp);
	
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot();
	
}
