package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeysReadMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.ValuesMessage;

final class KVReadCallable<K, V> implements Callable<Map<K, NavigableMap<Long, V>>> {
	
	private final static Logger LOG = Logger.getLogger(KVReadCallable.class.getName());
	private final Set<K> keys;
	private final Messenger messenger;
	private final Process readFrom;
	private final MessageListenerIdentifier listenerIdentifier;
	private final int requestTimeout;
	private final Process myIdentity;
	private final Model model;
	private final KVLocalDataStore<K, V> localDataManger;

	public KVReadCallable(Set<K> keys, Messenger messenger, Process readFrom,
			MessageListenerIdentifier listenerIdentifier, int requestTimeout, Process myIdentity, Model model,
			KVLocalDataStore<K, V> localDataManger) {
		this.keys = keys;
		this.messenger = messenger;
		this.readFrom = readFrom;
		this.listenerIdentifier = listenerIdentifier;
		this.requestTimeout = requestTimeout;
		this.myIdentity = myIdentity;
		this.model = model;
		this.localDataManger = localDataManger;
	}

	@Override
	public Map<K, NavigableMap<Long, V>> call() throws Exception {
		// If there is an exception while doing a remote read(MessengerException),
		// then that exception will be propagated up the call stack as it is.
		// Caller can retry if she wishes to.
		if (this.readFrom.equals(this.myIdentity)) { // Read from local.
			return readFromLocal();
		} else { // Read from remote.
			return readFromRemote();
		}
	}

	private Map<K, NavigableMap<Long, V>> readFromLocal() {
		LOG.debug(String.format("Reading from local. Keys: %s", this.keys));
		return this.localDataManger.read(this.keys);
	}
	
	private Map<K, NavigableMap<Long, V>> readFromRemote() {
		// Read from remote.
		Pair<Process, Message> dstnAndMsg = Pair.of(this.readFrom, createReadMessage());
		LOG.debug(String.format("Sending keys read message %s to %s. Keys: %s",
				dstnAndMsg.getRight().getUUID(),
				this.readFrom.getDisplayName(), this.keys));
		Message valueMessage = this.messenger.send(dstnAndMsg, this.requestTimeout);
		return extractValues(valueMessage, dstnAndMsg);
	}
	
	private KeysReadMessage<K> createReadMessage() {
		KeysReadMessage<K> readMsg = this.model.createKeysReadMessage(this.myIdentity, this.keys);
		stampMetaData(readMsg);
		return readMsg;
	}
	
	private void stampMetaData(Message message) {
		message.setMessageListenerId(this.listenerIdentifier);
	}
	
	@SuppressWarnings("unchecked")
	private Map<K, NavigableMap<Long, V>> extractValues(Message valueMsg, Pair<Process, Message> dstnAndMsg) {
		// Unsafe cast. Message type must be value message.
		// Not checking before cast because if check fails, I would want to throw some exception.
		// that will be done via ClassCastException anyway.
		ValuesMessage<K, V> valueMsgO = (ValuesMessage<K, V>) valueMsg;
		LOG.debug(String.format("%s replied in message %s for keys read message %s. Values: %s", 
				dstnAndMsg.getLeft().getDisplayName(), valueMsgO.getUUID(), dstnAndMsg.getRight().getUUID(),
				valueMsgO.getValues()));
		return valueMsgO.getValues();
	}
}
