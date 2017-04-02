package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.concurrent.Callable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;


final class KVWriteCallable<K, V> implements Callable<Boolean> {

	private final static Logger LOG = Logger.getLogger(KVWriteCallable.class.getName());
	private final K key;
	private final V value;
	private final Long timestamp;
	private final Messenger messenger;
	private final Process writeTo;
	private final MessageListenerIdentifier listenerIdentifier;
	private final int requestTimeout;
	private final Process myIdentity;
	private final Model model;
	private final KVDataManager<K, V> localDataManger;

	public KVWriteCallable(K key, V value, Long timestamp, Messenger messenger, Process writeTo,
			MessageListenerIdentifier listenerIdentifier, int requestTimeout, Process myIdentity, Model model,
			KVDataManager<K, V> localDataManger) {
		this.key = key;
		this.value = value;
		this.timestamp = timestamp;
		this.messenger = messenger;
		this.writeTo = writeTo;
		this.listenerIdentifier = listenerIdentifier;
		this.requestTimeout = requestTimeout;
		this.myIdentity = myIdentity;
		this.model = model;
		this.localDataManger = localDataManger;
	}

	@Override
	public Boolean call() throws Exception {
		// If there is an exception while doing a remote write(MessengerException),
		// then that exception will be propagated up the call stack as it is.
		// Caller can retry if she wishes to.
		if (this.writeTo.equals(this.myIdentity)) { // Write to local.
			return writeToLocal();
		} else { // Write to remote.
			return writeToRemote();
		}
	}

	private boolean writeToLocal() throws Exception {
		LOG.debug(String.format("Sending key write message to %s. Key: %s, Value: %s, Timestamp: %s",
				this.writeTo.getDisplayName(), this.key, this.value, this.timestamp));
		this.localDataManger.write(this.key, this.value, this.timestamp);
		return true;
	}
	
	private boolean writeToRemote() {
		// Write to remote.
		Pair<Process, Message> dstnAndMsg = Pair.of(this.writeTo, createWriteMessage());
		LOG.debug(String.format("Sending key write message %s to %s. Key: %s, Value: %s, Timestamp: %s",
				dstnAndMsg.getRight().getUUID(),
				this.writeTo.getDisplayName(), this.key, this.value, this.timestamp));
		Message ack = this.messenger.send(dstnAndMsg, this.requestTimeout);
		return ack != null;
	}
	
	private Message createWriteMessage() {
		Message writeMsg = this.model.createKeyWriteMessage(this.myIdentity, this.key, this.value, this.timestamp);
		stampMetaData(writeMsg);
		return writeMsg;
	}
	
	private void stampMetaData(Message message) {
		message.setMessageListenerId(this.listenerIdentifier);
	}
}
