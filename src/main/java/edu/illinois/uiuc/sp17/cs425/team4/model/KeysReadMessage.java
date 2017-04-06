package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.Set;

public interface KeysReadMessage<K> extends Message {
	
	public Set<K> readKeys();

}
