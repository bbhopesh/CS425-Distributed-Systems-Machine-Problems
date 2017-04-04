package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.model.KVRawOpResult;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeyReadMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeyWriteMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.ValueMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.MessageType;

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
	private final KVDataManager<K, V> localDataManager;
	
	public SequentialKVRawDataManager(KVDataManager<K, V> localDataManager, 
			Messenger messenger, Model model, Process myIdentity) {
		this.localDataManager = localDataManager;
		this.messenger = messenger;
		this.model = model;
		this.myIdentity = myIdentity;
		this.messenger.registerListener(this);
	}
	
	@Override
	public KVRawOpResult<Pair<Long, V>> read(K key, long asOfTimestamp, Set<Process> readFrom, int R, int requestTimeout) {
		// TODO add check that R is less than size of readFrom set.
		int successfulReads = 0; // counter to keep track of no of successful reads.
		Map<Process, Pair<Long, V>> completed = new HashMap<Process, Pair<Long, V>>();
		Map<Process, Throwable> failures = new HashMap<Process, Throwable>();
	
		for (Process readingFrom: readFrom) {
			//if (successfulReads == R) break; // Ignoring R because we are reading sequentially.
			KVReadCallable<K, V> reader = createTaskToRead(key, asOfTimestamp, readingFrom, requestTimeout);
			try {
				Pair<Long, V> timestampedValue = reader.call();
				completed.put(readingFrom, timestampedValue);
				successfulReads++;
			} catch(Exception e) {
				failures.put(readingFrom, e);
			}
		}
		// Return
		return this.model.createKVRawOpResult(successfulReads >= R, completed, failures, new HashMap<Process,Future<Pair<Long,V>>>());
	}
	
	private KVReadCallable<K, V> createTaskToRead(K key, long asOfTimestamp, Process readFrom, int requestTimeout) {
		return new KVReadCallable<K, V>(key, 
										asOfTimestamp, 
										this.messenger, 
										readFrom, 
										getIdentifier(), 
										requestTimeout, 
										this.myIdentity, 
										this.model,
										this.localDataManager);
	}
	
	
	@Override
	public KVRawOpResult<Boolean> write(K key, V value, long timestamp, Set<Process> writeTo, int W, int requestTimeout) {
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
		return new KVWriteCallable<K, V>(key, 
										value, 
										timestamp, 
										this.messenger, 
										writeTo, 
										getIdentifier(), 
										requestTimeout, 
										this.myIdentity, 
										this.model, 
										this.localDataManager);
	}

	@Override
	public KVRawOpResult<Boolean> delete(K key, Set<Process> deleteFrom, int D, int requestTimeout) {
		// TODO add check that D is less than size of deleteFrom set.
		int successfulDeletes = 0; // counter to keep track of no of successful deletes.
		Map<Process, Boolean> completed = new HashMap<Process, Boolean>();
		Map<Process, Throwable> failures = new HashMap<Process, Throwable>();
	
		for (Process deletingFrom: deleteFrom) {
			// if (successfulDeletes == D) break; // ignore D because deleting sequentially instead of in parallel have to delete from all.
			KVKeyDeleteCallable<K, V> deleter = createTaskToDelete(key, deletingFrom, requestTimeout);
			try {
				Boolean successfullyDeleted = deleter.call();
				completed.put(deletingFrom, successfullyDeleted);
				if (successfullyDeleted) {
					successfulDeletes++;
				}
			} catch(Exception e) {
				failures.put(deletingFrom, e);
			}
		}
		// Return
		return this.model.createKVRawOpResult(successfulDeletes >= D, completed, failures, new HashMap<Process,Future<Boolean>>());
	}

	private KVKeyDeleteCallable<K, V> createTaskToDelete(K key, Process deleteFrom, int requestTimeout) {
		return new KVKeyDeleteCallable<K, V>(key, 
											this.messenger,
											deleteFrom,
											getIdentifier(), 
											requestTimeout, 
											this.myIdentity, 
											this.model,
											this.localDataManager);
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
		if (msgType == MessageType.KEY_READ) {
			handleRemoteReadReq(sender, message, responseWriter);
		} else if (msgType == MessageType.KEY_WRITE) {
			handleRemoteWriteReq(sender, message, responseWriter);
		} else {
			throw new RuntimeException("Can only handle key_read or key_write messages.");
		}
	}

	@SuppressWarnings("unchecked")
	private void handleRemoteReadReq(Process sender, Message msg, ResponseWriter responseWriter) {
		// TODO For MP one message will have one read request only. We would do batching in a real world system.
		// Extract info from message.
		KeyReadMessage<K> keyReadMsg = (KeyReadMessage<K>) msg;
		K key = keyReadMsg.getKey();
		Long asOfTimestamp = keyReadMsg.getTimestamp();
		// Read data.
		Pair<Long, V> timestampAndVal = this.localDataManager.read(key, asOfTimestamp);
		LOG.debug(String.format("Received key read message %s from %s. Key: %s, AsOfTimestamp: %s",
				keyReadMsg.getUUID(),
				sender.getDisplayName(), key, asOfTimestamp));
		// Respond
		try {
			responseWriter.writeResponse(createValueMessage(timestampAndVal));
		} finally {
			responseWriter.close();
		}
	}
	
	private ValueMessage<V> createValueMessage(Pair<Long, V> timestampAndVal) {
		ValueMessage<V> valueMsg;
		if (timestampAndVal == null) {
			valueMsg = this.model.createNullValueMessage(this.myIdentity);
		} else {
			valueMsg = this.model.createValueMessage(this.myIdentity, timestampAndVal.getRight(), timestampAndVal.getLeft());
		}
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
		// TODO For MP one message will have one write request only. We would do batching in a real world system.
		
		// Extract info from message.
		KeyWriteMessage<K, V> keyWriteMsg = (KeyWriteMessage<K, V>) msg;
		K key = keyWriteMsg.getKey();
		V val = keyWriteMsg.getValue();
		Long timestamp = keyWriteMsg.getTimestamp();
		LOG.debug(String.format("Received key write message %s from %s. Key: %s, Value: %s, Timestamp: %s",
								keyWriteMsg.getUUID(),
								sender.getDisplayName(), key, val, timestamp));
		// write.
		this.localDataManager.write(key, val, timestamp);
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
		return this.localDataManager.getLocalSnapshot();
	}


}
