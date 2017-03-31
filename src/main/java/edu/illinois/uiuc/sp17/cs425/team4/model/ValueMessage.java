package edu.illinois.uiuc.sp17.cs425.team4.model;

public interface ValueMessage<V> {
	public V getValue();
	
	public Long getTimestamp();
}
