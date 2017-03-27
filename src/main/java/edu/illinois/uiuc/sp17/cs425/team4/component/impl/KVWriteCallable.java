package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.concurrent.Callable;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.exceptions.MessengerException;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;


final class KVWriteCallable<K, V> implements Callable<Boolean> {

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
		// or if remote peer doesn't send a ValueMessage, then exception will be thrown as it is from this method.
		// TCPMessenger currently throws an ContexedRuntimeException if there is an IOException while communicating with remote server.
		// However, TCPMessenger returns null if remote peer closes socket or if request times out.
		// I should specify the behavior of Messenger at the interface level but leaving it as it for now.
		
		Boolean writeSuccessful = false;
		
		try {
			if (this.writeTo.equals(this.myIdentity)) { // Write to local.
				writeSuccessful = writeToLocal();
			} else { // Write to remote.
				writeSuccessful = writeToRemote();
			}
		} catch (MessengerException e) {
			// ignore. We return false if there is an error.
		}
		return writeSuccessful;
	}

	private boolean writeToLocal() throws Exception {
		this.localDataManger.write(this.key, this.value);
		return true;
	}
	
	private boolean writeToRemote() {
		// Write to remote.
		Pair<Process, Message> dstnAndMsg = Pair.of(this.writeTo, createWriteMessage());
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
