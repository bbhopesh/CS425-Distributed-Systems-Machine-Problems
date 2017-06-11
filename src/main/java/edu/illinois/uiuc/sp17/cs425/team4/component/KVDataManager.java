package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;

import org.apache.commons.lang3.tuple.Pair;

/** 
 * Data manager for kv-store.
 * @author bbassi2
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public interface KVDataManager<K, V> {
	
	/**
	 * Read value and it's version for the given key.
	 * @param key Key.
	 * @return Value and it's version.
	 */
	public Pair<Long, V> read(K key);
	
	/**
	 * Write key-value pair. Version of this value is determined internally(current timestamp typically.)
	 * @param key Key.
	 * @param value Value.
	 * @return true if successfully written.
	 */
	public boolean write(K key, V value);
	
	/**
	 * Read value for the given key as of the provided timestamp.
	 * @param key
	 * @param asOfTimestamp
	 * @return value and it's version.
	 */
	public Pair<Long, V> read(K key, long asOfTimestamp);
	
	/**
	 * Write key value pair with version of the value as provided.
	 * @param key Key.
	 * @param value Value.
	 * @param timestamp Version of the value.
	 * @return true if written successfully.
	 */
	public boolean write(K key, V value, long timestamp);
	
	/**
	 * Read the entire data.
	 * @return Map containing entire data in the key-value store.
	 */
	public Map<K, NavigableMap<Long, V>> readMyData(); 
	
	/**
	 * Write entire batch of data.
	 * @param data Entire data.
	 * @return true if entire batch is written.
	 */
	public boolean writeBatch(Map<K, NavigableMap<Long, V>> data);
	
	/**
	 * Read batch of data.
	 * @param keys Keys and versions as of which values have to be read.
	 * @return map containing data along with versions in the database.
	 */
	public Map<K, NavigableMap<Long, V>> readBatch(Map<K, NavigableSet<Long>> keys);
	
	/**
	 * Get snapshot of the data as of the provided timestamp.
	 * @param asOfTimestamp As of timestamp.
	 * @return snapshot of the data.
	 */
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot(Long asOfTimestamp);
	
	/**
	 * Get snapshot of entire data in database.
	 * @return snapshot of the data.
	 */
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot();
	
}
