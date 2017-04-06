package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.Map;
import java.util.NavigableMap;

public interface ValuesMessage<K, V> extends Message {
	// If key doesn't exist in system, then it shouldn't be present in Map, but method should never 
	// return null. If no key exists, then it should return an empty map.
	public Map<K, NavigableMap<Long,V>> getValues();
}
