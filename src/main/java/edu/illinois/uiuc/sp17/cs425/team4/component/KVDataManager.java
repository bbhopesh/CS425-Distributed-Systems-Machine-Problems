package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public interface KVDataManager<K, V> {

	// TODO For a real system, we will have to provide batched operations for efficiency.
	public Pair<Long, V> read(K key); // TODO some exception to reply that read failed.
	
	public boolean write(K key, V value); // TODO some exception to reply that write failed.
	
	public Pair<Long, V> read(K key, long asOfTimestamp);
	
	public boolean write(K key, V value, long timestamp);
	
	public boolean delete(K key); // TODO some exception to reply that delete failed.
	
	public Set<Pair<Long,V>> list_local();
	
}
