package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KeyLockManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.TransactionManager;
import edu.illinois.uiuc.sp17.cs425.team4.exceptions.LockServiceException;
import edu.illinois.uiuc.sp17.cs425.team4.exceptions.TransactionAbortedException;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;
import edu.illinois.uiuc.sp17.cs425.team4.model.KVAsyncOpResult;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
/**
 * A transaction manager.
 * 
 * @author bbassi2.
 *
 * @param <K>
 * @param <V>
 */
public class TransactionManagerImpl<K, V> implements TransactionManager<K, V> {

	private static final long DUMMY_TIMESTAMP = Long.MAX_VALUE;
	private final KeyLockManager<Pair<Process, K>> keyLockManager;
	private final KVRawDataManager<K, V> rawDataManager;
	private final Transaction transaction;
	private final Map<Pair<Process, K>, V> tentativeData;
	private final Set<Pair<Process,K>> heldReadLocks;
	private final Set<Pair<Process,K>> heldWriteLocks;
	private final int remoteReadWriteTimeout;
	private boolean closed;

	public TransactionManagerImpl(KeyLockManager<Pair<Process, K>> keyLockManager,
									KVRawDataManager<K,V> rawDataManager,
									int remoteReadWriteTimeout) {
		this.keyLockManager = keyLockManager;
		this.rawDataManager = rawDataManager;
		this.remoteReadWriteTimeout = remoteReadWriteTimeout;
		
		this.transaction = this.keyLockManager.beginNew();
		this.tentativeData = new HashMap<>();
		this.heldReadLocks = new HashSet<>();
		this.heldWriteLocks = new HashSet<>();
		this.closed = false;
	}
	
	@Override
	public V get(Process owner, K key) throws TransactionAbortedException, LockServiceException {
		checkClosed();
		// Get lock.
		readLock(owner, key);
		// Read.
		V value = read(owner, key);
		// If value is null, we have to abort(requirement of MP3)
		if (value == null) {
			abort();
			throw new TransactionAbortedException("Value was not set previously.", new NoSuchElementException());
		} else {
			return value;
		}
	}

	private V read(Process owner, K key) {
		Pair<Process, K> keyR = Pair.of(owner, key);
		if (this.tentativeData.containsKey(keyR)) {
			return this.tentativeData.get(keyR);
		} else {
			KVAsyncOpResult<Pair<Long, V>> result = 
						this.rawDataManager.read(key, DUMMY_TIMESTAMP, new HashSet<>(Arrays.asList(owner)),
										1, this.remoteReadWriteTimeout);
			if (result.succeeded()) {
				return getValue(result, owner);
			} else {
				return null;
			}
		}
	}
	
	private V getValue(KVAsyncOpResult<Pair<Long, V>> rawRes, Process owner) {
		Pair<Long, V> data = rawRes.completed().get(owner);
		if (data != null) {
			return data.getRight();
		} else {
			return null;
		}
	}

	@Override
	public void set(Process owner, K key, V value) throws TransactionAbortedException, LockServiceException {
		checkClosed();
		// Get lock.
		writeLock(owner, key);
		// Write to tentative data, we will only write to server at the time of commit.
		Pair<Process, K> keyR = Pair.of(owner, key);
		this.tentativeData.put(keyR, value);
	}

	@Override
	public void commit() {
		checkClosed();
		// Save tentative data.
		saveTentativeData();
		// Close transaction.
		close();
	}
	
	private void saveTentativeData() {
		// We are not dealing with failures and hoping that everything goes through.
		// With the setup of MP3, there shouldn't be any failures.
		Map<Process, Map<K, NavigableMap<Long, V>>> data =  new HashMap<>();
		
		for (Entry<Pair<Process, K>, V> tentativeEntry: this.tentativeData.entrySet()) {
			Process owner = tentativeEntry.getKey().getLeft();
			K key = tentativeEntry.getKey().getRight();
			V value = tentativeEntry.getValue();
			
			Map<K, NavigableMap<Long, V>> dataMap = data.get(owner);
			if (dataMap == null) {
				dataMap = new HashMap<>();
				data.put(owner, dataMap);
			}
			// A unique <owner, key> pair can only have one value, so it's okay to create a new navigable map without any checks.
			NavigableMap<Long, V> valueMap = new TreeMap<>();
			valueMap.put(DUMMY_TIMESTAMP, value);
			dataMap.put(key, valueMap);
		}
		this.rawDataManager.writeBatch(data, this.remoteReadWriteTimeout);
	}

	@Override
	public void abort() {
		checkClosed();
		// Close discarding tentative data.
		close();
	}
	
	private void close() {
		this.keyLockManager.closeTransaction(this.transaction);
		this.closed = true;
	}
	
	private void checkClosed() {
		if (this.closed) {
			throw new IllegalStateException("Transaction has already been closed.");
		}
	}

	@Override
	public Transaction getTransaction() {
		return this.transaction;
	}
	
	private void readLock(Process owner, K key) throws TransactionAbortedException, LockServiceException {
		if (this.heldReadLocks.contains(key) || this.heldWriteLocks.contains(key)) {
			// Just an optimization, can go to lock service multiple times too. Locks are reentrant.
			return;
		}
		Pair<Process, K> lockKey = Pair.of(owner, key);
		this.keyLockManager.acquireReadLock(this.transaction, lockKey);
		this.heldReadLocks.add(lockKey);
	}
	
	private void writeLock(Process owner, K key) throws TransactionAbortedException, LockServiceException {
		if (this.heldWriteLocks.contains(key)) {
			// Just an optimization, can go to lock service multiple times too. Locks are reentrant.
			return;
		}
		Pair<Process, K> lockKey = Pair.of(owner, key);
		this.keyLockManager.acquireWriteLock(this.transaction, lockKey);
		this.heldWriteLocks.add(lockKey);
	}
}
