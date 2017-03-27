package edu.illinois.uiuc.sp17.cs425.team4.model;

public interface KeyReadMessage<K> extends Message {
	
	public K getKey();
	
	public Long getTimestamp();

}
