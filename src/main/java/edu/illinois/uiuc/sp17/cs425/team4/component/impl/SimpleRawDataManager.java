package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeyReadMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeyWriteMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.MessageType;

public class SimpleRawDataManager<K, V> implements KVRawDataManager<K, V>, MessageListener {
	
	/** Listener Identifier. */
	private static final String S_IDENTIFIER = "SimpleRawDataManager";
	/** Listener Identifier. */
	private static final MessageListenerIdentifier IDENTIFIER = 
			new MessageListenerIdentifierImpl(S_IDENTIFIER);
	
	private final Messenger messenger;
	private final Model model;
	private final Process myIdentity;
	private final ExecutorService threadPool;
	private final KVDataManager<K, V> localDataManager;
	
	public SimpleRawDataManager(KVDataManager<K, V> localDataManager, 
			Messenger messenger, Model model, Process myIdentity,
			ExecutorService threadPool) {
		this.localDataManager = localDataManager;
		this.messenger = messenger;
		this.model = model;
		this.myIdentity = myIdentity;
		this.threadPool = threadPool;
	}
	
	@Override
	public Pair<Long, V> read(K key, long asOfTimestamp, Set<Process> readFrom, int R) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(K key, V value, long timestamp, Set<Process> writeTo, int W) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(K key) {
		// TODO Auto-generated method stub
		
	}

	
	// Code to handle remote read/write requests.
	@Override
	public void messageReceived(Pair<Process, Message> sourceAndMsg, ResponseWriter responseWriter) {
		// TODO handle remote delete request.
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
		Pair<Long, V> timestampAndVal = this.localDataManager.read(key, asOfTimestamp);
		// Respond
		try {
			if (timestampAndVal != null) {
				responseWriter.writeResponse(createValueMessage(timestampAndVal));
			}
		} finally {
			responseWriter.close();
		}
	}
	
	private Message createValueMessage(Pair<Long, V> timestampAndVal) {
		Message valueMsg = this.model.createValueMessage(this.myIdentity, timestampAndVal.getRight(), timestampAndVal.getLeft());
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

}
