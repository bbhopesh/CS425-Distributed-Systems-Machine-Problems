package edu.illinois.uiuc.sp17.cs425.team4.component;

import org.apache.commons.lang3.tuple.Pair;

public interface KVDataManager<K, V> {

	// TODO For a real system, we will have to provide batched operations for efficiency.
	public Pair<Long, V> read(K key); // TODO some exception to reply that read failed.
	
	public void write(K key, V value); // TODO some exception to reply that write failed.
	
	public Pair<Long, V> read(K key, long asOfTimestamp);
	
	public void write(K key, V value, long timestamp);
	
	public void delete(K key); // TODO some exception to reply that delete failed.
	
}
