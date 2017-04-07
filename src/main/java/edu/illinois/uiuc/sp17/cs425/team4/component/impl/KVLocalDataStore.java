package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import edu.illinois.uiuc.sp17.cs425.team4.util.KVUtils;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class KVLocalDataStore<K,V> {
	private final ConcurrentMap<K, ConcurrentNavigableMap<Long,V>> data;
	
	public KVLocalDataStore() {
		this.data = new ConcurrentHashMap<K, ConcurrentNavigableMap<Long,V>>();
	}
	
	private NavigableMap<Long,V> read(K key) {
		if (this.data.containsKey(key)) {
			NavigableMap<Long,V> values = new TreeMap<Long, V>(KVUtils.createDecLongComp());
			values.putAll(this.data.get(key));
			return values;
		} else {
			return null;
		}
	}

	public Map<K, NavigableMap<Long,V>> read(Set<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return getSnapshot();
		}
		// If no key exists returns an empty map but never returns a null map.
		Map<K, NavigableMap<Long, V>> valuesMap =  new HashMap<>();
		for (K key: keys) {
			NavigableMap<Long,V> values = read(key);
			if (values != null) {
				valuesMap.put(key, read(key));
			}
		}
		return valuesMap;
	}

	public void write(Map<K, NavigableMap<Long, V>> data) {
		for (Entry<K, NavigableMap<Long, V>> dataEntry: data.entrySet()) {
			write(dataEntry.getKey(), dataEntry.getValue());
		}
	}
	
	private void write(K key, NavigableMap<Long, V> values) {
		ConcurrentNavigableMap<Long, V> timestampedValues = new ConcurrentSkipListMap<Long, V>(KVUtils.createDecLongComp());
		// Atomically add an empty value map if the key is new.
		ConcurrentNavigableMap<Long, V> previousVal = this.data.putIfAbsent(key, timestampedValues);
		if (previousVal != null) {
			// If there was already a mapping use that.
			timestampedValues = previousVal;
		}
		// values shouldn't have a null key/value.
		timestampedValues.putAll(values);
	}
	

	public Map<K, NavigableMap<Long, V>> getSnapshot() {
		Map<K, NavigableMap<Long, V>> dataSnapshot =  new HashMap<K, NavigableMap<Long, V>>();
		for (Entry<K, ConcurrentNavigableMap<Long, V>> dataEntry: this.data.entrySet()) {
			NavigableMap<Long, V> values = new TreeMap<Long,V>(KVUtils.createDecLongComp());
			for (Entry<Long, V> valueEntry: dataEntry.getValue().entrySet()) {
				values.put(valueEntry.getKey(), valueEntry.getValue());
			}
			dataSnapshot.put(dataEntry.getKey(), values);
		}
		return dataSnapshot;
	}
	
	public Map<K, NavigableMap<Long, V>> getSnapshot(Long asOfTimestamp) {
		Map<K, NavigableMap<Long, V>> dataSnapshot =  new HashMap<K, NavigableMap<Long, V>>();
		for (Entry<K, ConcurrentNavigableMap<Long, V>> dataEntry: this.data.entrySet()) {
			NavigableMap<Long, V> values = new TreeMap<Long,V>(KVUtils.createDecLongComp());
			for (Entry<Long, V> valueEntry: dataEntry.getValue().entrySet()) {
				if (valueEntry.getKey() <= asOfTimestamp) {
					values.put(valueEntry.getKey(), valueEntry.getValue());
				}
			}
			dataSnapshot.put(dataEntry.getKey(), values);
		}
		return dataSnapshot;
	}
}
