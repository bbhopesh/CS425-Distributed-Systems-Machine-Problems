package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.concurrent.Callable;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

final class KVKeyDeleteCallable<K, V> implements Callable<Boolean> {

	private final K key;
	private final Messenger messenger;
	private final Process deleteFrom;
	private final MessageListenerIdentifier listenerIdentifier;
	private final int requestTimeout;
	private final Process myIdentity;
	private final Model model;
	private final KVDataManager<K, V> localDataManger;
	
	
	
	public KVKeyDeleteCallable(K key, Messenger messenger, Process deleteFrom,
			MessageListenerIdentifier listenerIdentifier, int requestTimeout, Process myIdentity, Model model,
			KVDataManager<K, V> localDataManger) {
		this.key = key;
		this.messenger = messenger;
		this.deleteFrom = deleteFrom;
		this.listenerIdentifier = listenerIdentifier;
		this.requestTimeout = requestTimeout;
		this.myIdentity = myIdentity;
		this.model = model;
		this.localDataManger = localDataManger;
	}



	@Override
	public Boolean call() throws Exception {
		// If there is an exception while doing a remote delete(MessengerException),
		// then that exception will be propagated up the call stack as it is.
		// Caller can retry if she wishes to.
		if (this.deleteFrom.equals(this.myIdentity)) { // Delete from local.
			return delteFromLocal();
		} else { // Delete from remote.
			return deleteFromRemote();
		}
	}



	private Boolean deleteFromRemote() {
		// Write to remote.
		Pair<Process, Message> dstnAndMsg = Pair.of(this.deleteFrom, createDeleteMessage());
		Message ack = this.messenger.send(dstnAndMsg, this.requestTimeout);
		return ack != null;
	}

	private Message createDeleteMessage() {
		Message writeMsg = this.model.createKeyDeleteMessage(this.myIdentity, this.key);
		stampMetaData(writeMsg);
		return writeMsg;
	}
	
	private void stampMetaData(Message message) {
		message.setMessageListenerId(this.listenerIdentifier);
	}


	private Boolean delteFromLocal() {
		this.localDataManger.delete(this.key);
		return true;
	}

}
