package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Map.Entry;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeyReadMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeyWriteMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.MessageType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class SimpleKVCoordinator<K, V> implements KVDataManager<K, V>, MessageListener {
	
	private final ConcurrentMap<K, NavigableMap<Long,V>> data;
	private final KVDataPartitioner<K> kvDataPartitioner;
	private final Messenger messenger;
	private final Model model;
	private Process myIdentity;
	
	
	public SimpleKVCoordinator(KVDataPartitioner<K> kvDataPartitioner, Messenger messenger,
			Model model, Process myIdentity) {
		this.data = new ConcurrentHashMap<K, NavigableMap<Long,V>>();
		
		this.kvDataPartitioner = kvDataPartitioner;
		this.messenger = messenger;
		this.model = model;
		this.myIdentity = myIdentity;
	}
	
	@Override
	public Pair<Long, V> read(K key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean write(K key, V value) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public Pair<Long, V> read(K key, long timestamp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean write(K key, V value, long timestamp) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void messageReceived(Pair<Process, Message> sourceAndMsg, ResponseWriter responseWriter) {
		Process sender = sourceAndMsg.getLeft();
		Message message = sourceAndMsg.getRight();
		// Delegate to message type specific method.
		MessageType msgType = message.getMessageType();
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
		Entry<Long, V> valueEntry = localRead(key, asOfTimestamp);
		// Respond
		try {
			if (valueEntry != null) {
				responseWriter.writeResponse(createValueMessage(valueEntry));
			}
		} finally {
			responseWriter.close();
		}
	}
	
	private Message createValueMessage(Entry<Long, V> valueEntry) {
		Message valueMsg = this.model.createValueMessage(this.myIdentity, valueEntry.getValue(), valueEntry.getKey());
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
		// write.
		localWrite(key, val, timestamp);
		// Respond.
		try {
			responseWriter.writeResponse(createAckMessage());
		} finally {
			responseWriter.close();
		}
	}
	
	@Override
	public MessageListenerIdentifier getIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	private Entry<Long, V> localRead(K k, long asOfTimestamp) {
		NavigableMap<Long, V> values = this.data.get(k);
		// Check null.
		if (values == null) return null;
		// synchronize on values. Some other thread could be reading or writing value for same key.
		synchronized(values) {
			// Return value for timestamp less than or equal to the given timestamp.
			Entry<Long, V> valueEntry = values.ceilingEntry(asOfTimestamp);
			return valueEntry;
		}
	}
	
	private void localWrite(K k, V v, long timestamp) {
		// Initialize empty values map.
		NavigableMap<Long, V> valuesMap = new TreeMap<Long, V>(createDecLongComp());
		// Atomically add if doesn't exist already.
		valuesMap = this.data.putIfAbsent(k, valuesMap);
		// synchronize because some other thread could be working on same key.
		synchronized(valuesMap) {
			valuesMap.put(timestamp, v);
		}
		this.data.put(k, valuesMap);
	}
	
	private Comparator<Long> createDecLongComp() {
		return new Comparator<Long>() {

			@Override
			public int compare(Long o1, Long o2) {
				return -1*o1.compareTo(o2);
			}
		};
	}

	@Override
	public boolean delete(K key) {
		// TODO Auto-generated method stub
		return false;
	}
}
