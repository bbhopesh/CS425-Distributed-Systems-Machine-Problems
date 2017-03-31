package edu.illinois.uiuc.sp17.cs425.team4.model;

public interface KeyWriteMessage<K, V> extends Message {
	public K getKey();
	
	public V getValue();
	
	public Long getTimestamp();
}
