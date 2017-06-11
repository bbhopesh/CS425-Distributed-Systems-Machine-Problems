package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.Map;
import java.util.NavigableMap;

/**
 * Message asking to write key-value pairs and value versions.
 * 
 * @author bbassi2
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public interface KeysWriteMessage<K, V> extends Message {
	/**
	 * Get data to be written.
	 * @return data to be written.
	 */
	public Map<K, NavigableMap<Long, V>> getData();
}
