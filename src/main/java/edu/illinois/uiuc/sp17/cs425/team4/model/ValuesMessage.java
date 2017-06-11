package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.Map;
import java.util.NavigableMap;

/**
 * Message containing keys and coressponding values.
 * @author bbassi2
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public interface ValuesMessage<K, V> extends Message {
	// If key doesn't exist in system, then it shouldn't be present in Map, but method should never 
	// return null. If no key exists, then it should return an empty map.
	/**
	 * @return Values and their values.
	 */
	public Map<K, NavigableMap<Long,V>> getValues();
}
