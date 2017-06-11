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
/**
 * Local key-value store.
 * 
 * @author bbassi2.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class KVLocalDataStore<K,V> {
	/** Data. */
	private final ConcurrentMap<K, ConcurrentNavigableMap<Long,V>> data;
	
	/**
	 * Create an instance.
	 */
	public KVLocalDataStore() {
		this.data = new ConcurrentHashMap<K, ConcurrentNavigableMap<Long,V>>();
	}
	
	/**
	 * Read all versions for the given key.
	 * @param key Key.
	 * @return All versions of the given key.
	 */
	private NavigableMap<Long,V> read(K key) {
		if (this.data.containsKey(key)) {
			NavigableMap<Long,V> values = new TreeMap<Long, V>(KVUtils.createDecLongComp());
			values.putAll(this.data.get(key));
			return values;
		} else {
			return null;
		}
	}
	/**
	 * Read all versions for the given key.
	 * @param keys Keys.
	 * @return All versions of the given key.
	 */
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

	/**
	 * Write the given data.
	 * @param data Data.
	 */
	public void write(Map<K, NavigableMap<Long, V>> data) {
		for (Entry<K, NavigableMap<Long, V>> dataEntry: data.entrySet()) {
			write(dataEntry.getKey(), dataEntry.getValue());
		}
	}
	
	/**
	 * Write the key and varios values along with versions.
	 * @param key Key.
	 * @param values Values and their versions.
	 */
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
	

	/**
	 * Get snapshot.
	 * @return Data snapshot.
	 */
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
	
	/**
	 * Get data snapshot as of given timestamp.
	 * @param asOfTimestamp As of timestamp.
	 * @return Local data as of given timestamp.
	 */
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
