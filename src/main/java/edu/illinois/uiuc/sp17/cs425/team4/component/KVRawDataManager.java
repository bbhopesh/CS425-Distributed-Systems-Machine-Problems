package edu.illinois.uiuc.sp17.cs425.team4.component;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.model.KVAsyncOpResult;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * Raw data manager for kv store.
 * It is raw because where to read from and where to write to are all passed in the methods.
 * On the other hand KvDataManager is supposed to figure out where to store and where to read from on it's own.
 * @see KvDataManager
 * 
 * @author bbassi2
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public interface KVRawDataManager<K, V> {
	
	/**
	 * Read value. The method is designed so that caller doesn't wait for read to finish at all processes.
	 * The method returns after read from R replicas is finished. Status if rest of the reads can be queried from the returned object.
	 * @param key Key.
	 * @param asOfTimestamp Read as of this timestamp.
	 * @param readFrom Read from these sets.
	 * @param R wait for R reads to finish. Should be less than size of readFrom to be meanigful.
	 * @param requestTimeout timeout of read request to another process.
	 * @return A result object carrying read value and status of unfinished returns at the time of returning.
	 */
	public KVAsyncOpResult<Pair<Long, V>> read(K key, long asOfTimestamp, Set<Process> readFrom, int R, int requestTimeout);
	
	/**
	 * Write the given key-value pair and it's version. The method call returns when W writes are done.
	 * Rest are carried in background and their status is in the returned object.
	 * @param key Key.
	 * @param value Value.
	 * @param timestamp Timestamp.
	 * @param writeTo Write to the 
	 * @param W number of writes to complete before returning.
	 * @param requestTimeout timeout of request.
	 * @return A result object carrying overall write status and status of unfinished returns at the time of returning. 
	 */
	public KVAsyncOpResult<Boolean> write(K key, V value, long timestamp, Set<Process> writeTo, int W, int requestTimeout);
	
	/**
	 * Delete the key from the given processes.
	 * @param key Key.
	 * @param deleteFrom Delete from these processes.
	 * @param D Number of processes from which key is to be deleted before method call returns.
	 * @param requestTimeout timeout of request.
	 * @return A result object carrying overall status of delete and status of unfinished deletes.
	 */
	public KVAsyncOpResult<Boolean> delete(K key, Set<Process> deleteFrom, int D, int requestTimeout);
	
	/**
	 * Write batch of data.
	 * @param data Data.
	 * @param requestTimeout Timeout of request.
	 * @return A result object carrying status of the write.
	 */
	public KVAsyncOpResult<Boolean> writeBatch(Map<Process, Map<K, NavigableMap<Long, V>>> data, int requestTimeout);
	
	/**
	 * Read of the data.
	 * @param perProcessKeys Keys which have to be read.
	 * @param requestTimeout Timeout of the request.
	 * @return A result object carrying status of read.
	 */
	public KVAsyncOpResult<Map<K, NavigableMap<Long, V>>> readBatch(Map<Process, Set<K>> perProcessKeys, int requestTimeout);
	
	/**
	 * Read all keys from the given process.
	 * @param readFrom Read keys from these processes.
	 * @param requestTimeout Timeout of the request.
	 * @return A result object carrying status of read.
	 */
	public KVAsyncOpResult<Map<K, NavigableMap<Long, V>>> readBatch(Set<Process> readFrom, int requestTimeout);
	
	/**
	 * Get snapshot of data stored at current process.
	 * @return Local snapshot.
	 */
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot();
	
	/**
	 * Get snapshot of the local data as of provided timestamp.
	 * @param asOfTimestamp Snapshot as of timestamp.
	 * @return Local snapshot.
	 */
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot(Long asOfTimestamp);
}
