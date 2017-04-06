package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.Map;
import java.util.NavigableMap;

public interface KeysWriteMessage<K, V> extends Message {
	public Map<K, NavigableMap<Long, V>> getData();
}
