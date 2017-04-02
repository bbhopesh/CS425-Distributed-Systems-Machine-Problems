package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
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

public class SimpleRawDataManager<K, V> implements KVRawDataManager<K, V>, MessageListener {
	private final static Logger LOG = Logger.getLogger(SimpleRawDataManager.class.getName());
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
		this.messenger.registerListener(this);
	}
	
	@Override
	public KVRawOpResult<Pair<Long, V>> read(K key, long asOfTimestamp, Set<Process> readFrom, int R, int requestTimeout) {
		// TODO add check that R is less than size of readFrom set.
		// Map to keep track of read progress.
		Map<Process, Future<Pair<Long, V>>> inProgress = new HashMap<Process, Future<Pair<Long, V>>>();
		
		// Start reading from all processes in parallel.
		for (Process readingFrom: readFrom) {
			inProgress.put(readingFrom, 
					submitRead(key, asOfTimestamp, readingFrom, requestTimeout));
		}
		
		// Keep track of progress and reply when R replicas are done reading.
		
		int successfulReads = 0; // counter to keep track of no of successful reads.
		Map<Process, Pair<Long, V>> completed = new HashMap<Process, Pair<Long, V>>();
		Map<Process, Throwable> failures = new HashMap<Process, Throwable>();

		while (successfulReads < R && !inProgress.isEmpty()) {
			Set<Process> doneProcesses = new HashSet<Process>();
			
			for (Entry<Process,Future<Pair<Long, V>>> entry: inProgress.entrySet()) {
				Process readingFrom = entry.getKey();
				Triple<Boolean, Throwable, Pair<Long, V>> futRes = futureResult(entry.getValue());
				if (futRes.getLeft()) {
					// future is done.
					doneProcesses.add(readingFrom);
					if (futRes.getMiddle() != null) {
						// task failed.
						failures.put(readingFrom, futRes.getMiddle());
					} else {
						// task completed.
						completed.put(readingFrom, futRes.getRight());
						successfulReads++;
					}
				}
			}
			inProgress.keySet().removeAll(doneProcesses);
		}
		// Return
		return this.model.createKVRawOpResult(successfulReads >= R, completed, failures, inProgress);
	}
	
	private Future<Pair<Long, V>> submitRead(K key, long asOfTimestamp, Process readFrom, int requestTimeout) {
		return this.threadPool.submit(createTaskToRead(key, asOfTimestamp, readFrom, requestTimeout));
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
		// Map to keep track of write progress.
		Map<Process, Future<Boolean>> inProgress = new HashMap<Process, Future<Boolean>>();
		
		// Start writing from all processes in parallel.
		for (Process writingTo: writeTo) {
			inProgress.put(writingTo, 
					submitWrite(key, value, timestamp, writingTo, requestTimeout));
		}
		
		// Keep track of progress and reply when W replicas are done writing.
		
		int successfulWrites = 0; // counter to keep track of no of successful writes.
		Map<Process, Boolean> completed = new HashMap<Process, Boolean>();
		Map<Process, Throwable> failures = new HashMap<Process, Throwable>();

		while (successfulWrites < W && !inProgress.isEmpty()) {
			Set<Process> doneProcesses = new HashSet<Process>();
			
			for (Entry<Process,Future<Boolean>> entry: inProgress.entrySet()) {
				Process writingTo = entry.getKey();
				Triple<Boolean, Throwable, Boolean> futRes = futureResult(entry.getValue());
				if (futRes.getLeft()) {
					// future is done.
					doneProcesses.add(writingTo);
					if (futRes.getMiddle() != null) {
						// task failed.
						failures.put(writingTo, futRes.getMiddle());
					} else {
						// task completed.
						boolean successfullyWritten = futRes.getRight();
						completed.put(writingTo, successfullyWritten);
						if (successfullyWritten) {
							// if remote peer responded true, only then we increment. If remote pair responds false, or there was some exception we don't increment.
							// This is a bit different from read where whatever remote peer responds, we assume that to be true and increment successfulReads.
							successfulWrites++;
						}
					}
				}
			}
			inProgress.keySet().removeAll(doneProcesses);
		}
		
		// Return
		return this.model.createKVRawOpResult(successfulWrites >= W, completed, failures, inProgress);
	}
	
	private Future<Boolean> submitWrite(K key, V value, long timestamp, Process writeTo, int requestTimeout) {
		return this.threadPool.submit(createTaskToWrite(key, value, timestamp, writeTo, requestTimeout));
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
		// Map to keep track of write progress.
		Map<Process, Future<Boolean>> inProgress = new HashMap<Process, Future<Boolean>>();
		
		// Start writing from all processes in parallel.
		for (Process deletingFrom: deleteFrom) {
			inProgress.put(deletingFrom, 
					submitDelete(key, deletingFrom, requestTimeout));
		}
		
		// Keep track of progress and reply when D replicas are done deleting.
		
		int successfulDeletes = 0; // counter to keep track of no of successful deletes.
		Map<Process, Boolean> completed = new HashMap<Process, Boolean>();
		Map<Process, Throwable> failures = new HashMap<Process, Throwable>();

		while (successfulDeletes < D && !inProgress.isEmpty()) {
			Set<Process> doneProcesses = new HashSet<Process>();
			
			for (Entry<Process,Future<Boolean>> entry: inProgress.entrySet()) {
				Process deletingFrom = entry.getKey();
				Triple<Boolean, Throwable, Boolean> futRes = futureResult(entry.getValue());
				if (futRes.getLeft()) {
					// future is done.
					doneProcesses.add(deletingFrom);
					if (futRes.getMiddle() != null) {
						// task failed.
						failures.put(deletingFrom, futRes.getMiddle());
					} else {
						// task completed.
						boolean successfullyDeleted = futRes.getRight();
						completed.put(deletingFrom, successfullyDeleted);
						if (successfullyDeleted) {
							// if remote peer responded true, only then we increment. If remote pair responds false, or there was some exception we don't increment.
							// This is a bit different from read where whatever remote peer responds, we assume that to be true and increment successfulReads.
							successfulDeletes++;
						}
					}
				}
			}
			inProgress.keySet().removeAll(doneProcesses);
		}
		
		// Return
		return this.model.createKVRawOpResult(successfulDeletes >= D, completed, failures, inProgress);
	}

	private Future<Boolean> submitDelete(K key, Process deleteFrom, int requestTimeout) {
		return this.threadPool.submit(createTaskToDelete(key, deleteFrom, requestTimeout));
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
	
	private <R> Triple<Boolean, Throwable, R> futureResult(Future<R> fut) {
		R result = null;
		Throwable e = null;
		boolean done = false;
		
		if (fut.isDone()) {
			done = true;
			try {
				result = fut.get();
			} catch (InterruptedException e1) {
				// ignore. Code can never come here because of the isDone check.
			} catch (ExecutionException e1) {
				// Exception thrown from the task is wrapped inside the ExceutionException
				e = e1.getCause();
			}
		}
		return Triple.of(done, e, result);
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
	public Set<K> listLocal() {
		return this.localDataManager.listLocal();
	}

}
