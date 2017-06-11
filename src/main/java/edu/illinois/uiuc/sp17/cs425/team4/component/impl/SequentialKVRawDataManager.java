package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.model.KVAsyncOpResult;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeysReadMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeysWriteMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.ValuesMessage;
import edu.illinois.uiuc.sp17.cs425.team4.util.KVUtils;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.MessageType;

/**
 * A raw data manager which does reads/writes sequentially.
 * This essentially means that there will be no pending read/writes in the returned result object.
 * 
 * @author bbassi2
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class SequentialKVRawDataManager<K, V> implements KVRawDataManager<K, V>, MessageListener {
	private final static Logger LOG = Logger.getLogger(SequentialKVRawDataManager.class.getName());
	/** Listener Identifier. */
	private static final String S_IDENTIFIER = "SequentialKVRawDataManager";
	/** Listener Identifier. */
	private static final MessageListenerIdentifier IDENTIFIER = 
			new MessageListenerIdentifierImpl(S_IDENTIFIER);
	
	private final Messenger messenger;
	private final Model model;
	private final Process myIdentity;
	private final KVLocalDataStore<K, V> localDataStore;
	
	public SequentialKVRawDataManager(KVLocalDataStore<K, V> localDataStore, 
			Messenger messenger, Model model, Process myIdentity) {
		this.localDataStore = localDataStore;
		this.messenger = messenger;
		this.model = model;
		this.myIdentity = myIdentity;
		this.messenger.registerListener(this);
	}
	
	@Override
	public KVAsyncOpResult<Pair<Long, V>> read(K key, long asOfTimestamp, Set<Process> readFrom, int R, int requestTimeout) {
		// TODO add check that R is less than size of readFrom set.
		int successfulReads = 0; // counter to keep track of no of successful reads.
		Map<Process, Pair<Long, V>> completed = new HashMap<Process, Pair<Long, V>>();
		Map<Process, Throwable> failures = new HashMap<Process, Throwable>();
	
		for (Process readingFrom: readFrom) {
			//if (successfulReads == R) break; // Ignoring R because we are reading sequentially.
			KVReadCallable<K, V> reader = createTaskToRead(key, readingFrom, requestTimeout);
			try {
				Pair<Long, V> timestampedValue = getLatest(reader, key, asOfTimestamp);
				completed.put(readingFrom, timestampedValue);
				successfulReads++;
			} catch(Exception e) {
				failures.put(readingFrom, e);
			}
		}
		// Return
		return this.model.createKVRawOpResult(successfulReads >= R, completed, failures, new HashMap<Process,Future<Pair<Long,V>>>());
	}
	
	private Pair<Long, V> getLatest(KVReadCallable<K, V> reader, K key, Long asOfTimestamp) throws Exception {
		Map<K, NavigableMap<Long, V>> data = reader.call();
		if (data.containsKey(key)) {
			Entry<Long, V> dataEntry = data.get(key).ceilingEntry(asOfTimestamp);
			return dataEntry != null ? Pair.of(dataEntry.getKey(), dataEntry.getValue()) : null;
		} else {
			return null;
		}
	}
	
	private KVReadCallable<K, V> createTaskToRead(K key, Process readFrom, int requestTimeout) {
		return createTaskToRead(new HashSet<K>(Arrays.asList(key)), readFrom, requestTimeout);
	}
	
	private KVReadCallable<K, V> createTaskToRead(Set<K> keys, Process readFrom, int requestTimeout) {
		return new KVReadCallable<K, V>(keys, 
										this.messenger, 
										readFrom, 
										getIdentifier(), 
										requestTimeout, 
										this.myIdentity, 
										this.model,
										this.localDataStore);
	}
	
	
	@Override
	public KVAsyncOpResult<Boolean> write(K key, V value, long timestamp, Set<Process> writeTo, int W, int requestTimeout) {
		// TODO add check that W is less than size of writeTo set.
		
		int successfulWrites = 0; // counter to keep track of no of successful writes.
		Map<Process, Boolean> completed = new HashMap<Process, Boolean>();
		Map<Process, Throwable> failures = new HashMap<Process, Throwable>();
	
		for (Process writingTo: writeTo) {
			// if (successfulWrites == W) break; // ignore W because writing sequentially instead of in paralle.
			KVWriteCallable<K, V> writer = createTaskToWrite(key, value, timestamp, writingTo, requestTimeout);
			try {
				Boolean successfullyWritten = writer.call();
				completed.put(writingTo, successfullyWritten);
				if (successfullyWritten) {
					successfulWrites++;
				}
			} catch(Exception e) {
				failures.put(writingTo, e);
			}
		}
		// Return
		return this.model.createKVRawOpResult(successfulWrites >= W, completed, failures, new HashMap<Process,Future<Boolean>>());
	}
	
	
	private KVWriteCallable<K, V> createTaskToWrite(K key, V value, long timestamp, Process writeTo, int requestTimeout) {
		NavigableMap<Long, V> valueMap = new TreeMap<>(KVUtils.createDecLongComp());
		valueMap.put(timestamp, value);
		Map<K, NavigableMap<Long, V>> data = new HashMap<K, NavigableMap<Long, V>>();
		data.put(key, valueMap); 
		return createTaskToWrite(data, writeTo, requestTimeout);
	}
	
	private KVWriteCallable<K, V> createTaskToWrite(Map<K, NavigableMap<Long, V>> data, Process writeTo, int requestTimeout) {
		return new KVWriteCallable<K, V>(data,
										this.messenger, 
										writeTo, 
										getIdentifier(), 
										requestTimeout, 
										this.myIdentity, 
										this.model, 
										this.localDataStore);
	}

	@Override
	public KVAsyncOpResult<Boolean> delete(K key, Set<Process> deleteFrom, int D, int requestTimeout) {
		throw new UnsupportedOperationException();
	}
	
	// Code to handle remote read/write requests.
	@Override
	public void messageReceived(Pair<Process, Message> sourceAndMsg, ResponseWriter responseWriter) {
		// TODO handle remote delete request.
		Process sender = sourceAndMsg.getLeft();
		Message message = sourceAndMsg.getRight();
		// Delegate to message type specific method.
		MessageType msgType = message.getMessageType();
		LOG.debug(String.format("Received message %s with message type %s from %s", message.getUUID(), msgType, sender.getDisplayName()));
		if (msgType == MessageType.KEYS_READ) {
			handleRemoteReadReq(sender, message, responseWriter);
		} else if (msgType == MessageType.KEYS_WRITE) {
			handleRemoteWriteReq(sender, message, responseWriter);
		} else {
			throw new RuntimeException("Can only handle key_read or key_write messages.");
		}
	}

	@SuppressWarnings("unchecked")
	private void handleRemoteReadReq(Process sender, Message msg, ResponseWriter responseWriter) {
		// TODO For MP one message will have one read request only. We would do batching in a real world system.
		// Extract info from message.
		KeysReadMessage<K> keyReadMsg = (KeysReadMessage<K>) msg;
		// Read data.
		Map<K, NavigableMap<Long, V>> data = this.localDataStore.read(keyReadMsg.readKeys());
		LOG.debug(String.format("Received keys read message %s from %s. Data: %s",
				keyReadMsg.getUUID(),
				sender.getDisplayName(), data));
		// Respond
		try {
			Message valuesMsg = createValueMessage(data);
			responseWriter.writeResponse(valuesMsg);
		} finally {
			responseWriter.close();
		}
	}
	
	private ValuesMessage<K, V> createValueMessage(Map<K, NavigableMap<Long, V>> data) {
		ValuesMessage<K, V> valueMsg;
		valueMsg = this.model.createValueMessage(this.myIdentity, data);
		stampMetaData(valueMsg);
		return valueMsg;
	}
	
	private Message createAckMessage() {
		Message ack = this.model.createAckMessage(this.myIdentity);
		stampMetaData(ack);
		return ack;
	}
	
	private void stampMetaData(Message message) {
		message.setMessageListenerId(getIdentifier());
	}
	
	@SuppressWarnings("unchecked")
	private void handleRemoteWriteReq(Process sender, Message msg, ResponseWriter responseWriter) {
		// Extract info from message.
		KeysWriteMessage<K, V> keysWriteMsg = (KeysWriteMessage<K, V>) msg;
		Map<K, NavigableMap<Long, V>> data =  keysWriteMsg.getData();
		LOG.debug(String.format("Received keya write message %s from %s. Data: ",
								keysWriteMsg.getUUID(),
								sender.getDisplayName(), data));
		// write.
		this.localDataStore.write(data);
		try {
			// Respond.
			responseWriter.writeResponse(createAckMessage());
		} finally {
			responseWriter.close();
		}
	}

	@Override
	public MessageListenerIdentifier getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot() {
		return this.localDataStore.getSnapshot();
	}

	@Override
	public KVAsyncOpResult<Boolean> writeBatch(Map<Process, Map<K, NavigableMap<Long, V>>> perProcessData, int requestTimeout) {
		int successfulWrites = 0; // counter to keep track of no of successful writes.
		Map<Process, Boolean> completed = new HashMap<Process, Boolean>();
		Map<Process, Throwable> failures = new HashMap<Process, Throwable>();
	
		for (Entry<Process, Map<K, NavigableMap<Long, V>>> entry: perProcessData.entrySet()) {
			Process writingTo = entry.getKey();
			Map<K, NavigableMap<Long, V>> data = entry.getValue();
			KVWriteCallable<K, V> writer = createTaskToWrite(data, writingTo, requestTimeout);
			try {
				Boolean successfullyWritten = writer.call();
				completed.put(writingTo, successfullyWritten);
				if (successfullyWritten) {
					successfulWrites++;
				}
			} catch(Exception e) {
				failures.put(writingTo, e);
			}
		}
		// Return
		return this.model.createKVRawOpResult(successfulWrites == perProcessData.keySet().size(), completed, failures, new HashMap<Process,Future<Boolean>>());
	}

	@Override
	public KVAsyncOpResult<Map<K, NavigableMap<Long, V>>> readBatch(Map<Process, Set<K>> perProcessKeys, int requestTimeout) {
		int successfulReads = 0; // counter to keep track of no of successful reads.
		Map<Process, Map<K, NavigableMap<Long, V>>> completed = new HashMap<>();
		Map<Process, Throwable> failures = new HashMap<Process, Throwable>();
	
		for (Entry<Process, Set<K>> entry: perProcessKeys.entrySet()) {
			Process readFrom = entry.getKey();
			Set<K> keysToRead = entry.getValue();
			KVReadCallable<K, V> reader = createTaskToRead(keysToRead, readFrom, requestTimeout);
			try {
				Map<K, NavigableMap<Long, V>> data = reader.call();
				completed.put(readFrom, data);
				successfulReads++;
			} catch(Exception e) {
				failures.put(readFrom, e);
			}
		}
		// Return
		return this.model.createKVRawOpResult(successfulReads == perProcessKeys.size(), completed, failures, new HashMap<Process,Future<Map<K, NavigableMap<Long, V>>>>());
	}
	
	@Override
	public KVAsyncOpResult<Map<K, NavigableMap<Long, V>>> readBatch(Set<Process> readFrom, int requestTimeout) {
		Map<Process, Set<K>> perProcessKeys = new HashMap<>();
		for (Process p: readFrom) {
			perProcessKeys.put(p, new HashSet<>()); // empty set of keys mean read all keys.
		}
		return readBatch(perProcessKeys, requestTimeout);
	}

	@Override
	public Map<K, NavigableMap<Long, V>> getLocalSnapshot(Long asOfTimestamp) {
		return this.localDataStore.getSnapshot(asOfTimestamp);
	}
}
