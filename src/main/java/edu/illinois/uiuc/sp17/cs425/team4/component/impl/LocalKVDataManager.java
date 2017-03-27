package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;

public class LocalKVDataManager<K,V> implements KVDataManager<K, V> {

	private final ConcurrentMap<K, ConcurrentNavigableMap<Long,V>> data;
	
	public LocalKVDataManager() {
		this.data = new ConcurrentHashMap<K, ConcurrentNavigableMap<Long,V>>();
	}
	
	@Override
	public Pair<Long, V> read(K key) {
		return read(key, System.currentTimeMillis());
	}

	@Override
	public void write(K key, V value) {
		write(key, value, System.currentTimeMillis());
	}

	@Override
	public Pair<Long, V> read(K key, long asOfTimestamp) {
		ConcurrentNavigableMap<Long, V> timestampedValues = this.data.get(key);
		// Check null.
		if (timestampedValues == null) return null;
		// Return value for timestamp less than or equal to the given timestamp.
		// Ceiling entry because we are storing items in Map by descending order of timestamp.
		Entry<Long, V> valueEntry = timestampedValues.ceilingEntry(asOfTimestamp);
		return Pair.of(valueEntry.getKey(), valueEntry.getValue());
		// Both inner and outer maps are thread-safe, so we don't need to synchronize ourselves.
	}

	@Override
	public void write(K key, V value, long timestamp) {
		ConcurrentNavigableMap<Long, V> timestampedValues = new ConcurrentSkipListMap<Long, V>(createDecLongComp());
		// Atomically add an empty value map if the key is new.
		timestampedValues = this.data.putIfAbsent(key, timestampedValues);
		timestampedValues.put(timestamp, value);
		// Both inner and outer maps are thread-safe, so we don't need to synchronize ourselves.
	}

	@Override
	public void delete(K key) {
		this.data.remove(key);
	}
	

	private Comparator<Long> createDecLongComp() {
		return new Comparator<Long>() {

			@Override
			public int compare(Long o1, Long o2) {
				return -1*o1.compareTo(o2);
			}
		};
	}

}
