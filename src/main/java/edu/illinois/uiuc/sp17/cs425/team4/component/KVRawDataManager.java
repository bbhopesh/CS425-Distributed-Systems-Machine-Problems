package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public interface KVRawDataManager<K, V> {
	
	// TODO For a real system, we will have to provide batched operations for efficiency.
	public Pair<Long, V> read(K key, long asOfTimestamp, Set<Process> readFrom, int R); // TODO some exception to reply that read failed.
	
	public void write(K key, V value, long timestamp, Set<Process> writeTo, int W); // TODO some exception to reply that write failed.
	
	public void delete(K key); // TODO some exception to reply that delete failed.
}
