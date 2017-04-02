package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.concurrent.Callable;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.ValueMessage;

final class KVReadCallable<K, V> implements Callable<Pair<Long, V>> {
	
	private final K key;
	private final Long asOfTimestamp;
	private final Messenger messenger;
	private final Process readFrom;
	private final MessageListenerIdentifier listenerIdentifier;
	private final int requestTimeout;
	private final Process myIdentity;
	private final Model model;
	private final KVDataManager<K, V> localDataManger;

	public KVReadCallable(K key, Long asOfTimestamp, Messenger messenger, Process readFrom,
			MessageListenerIdentifier listenerIdentifier, int requestTimeout, Process myIdentity, Model model,
			KVDataManager<K, V> localDataManger) {
		this.key = key;
		this.asOfTimestamp = asOfTimestamp;
		this.messenger = messenger;
		this.readFrom = readFrom;
		this.listenerIdentifier = listenerIdentifier;
		this.requestTimeout = requestTimeout;
		this.myIdentity = myIdentity;
		this.model = model;
		this.localDataManger = localDataManger;
	}

	@Override
	public Pair<Long, V> call() {
		// If there is an exception while doing a remote read(MessengerException),
		// then that exception will be propagated up the call stack as it is.
		// Caller can retry if she wishes to.
		
		if (this.readFrom.equals(this.myIdentity)) { // Read from local.
			return readFromLocal();
		} else { // Read from remote.
			return readFromRemote();
		}
	}

	private Pair<Long, V> readFromLocal() {
		return this.localDataManger.read(this.key);
	}
	
	private Pair<Long, V> readFromRemote() {
		// Read from remote.
		Pair<Process, Message> dstnAndMsg = Pair.of(this.readFrom, createReadMessage());
		Message valueMessage = this.messenger.send(dstnAndMsg, this.requestTimeout);
		return extractValue(valueMessage);
	}
	
	private Message createReadMessage() {
		Message readMsg = this.model.createKeyReadMessage(this.myIdentity, this.key, this.asOfTimestamp);
		stampMetaData(readMsg);
		return readMsg;
	}
	
	private void stampMetaData(Message message) {
		message.setMessageListenerId(this.listenerIdentifier);
	}
	
	@SuppressWarnings("unchecked")
	private Pair<Long, V> extractValue(Message valueMsg) {
		// Unsafe cast. Message type must be value message.
		// Not checking before cast because if check fails, I would want to throw some exception.
		// that will be done via ClassCastException anyway.
		ValueMessage<V> valueMsgO = (ValueMessage<V>) valueMsg;
		return Pair.of(valueMsgO.getTimestamp(), valueMsgO.getValue());
	}
}