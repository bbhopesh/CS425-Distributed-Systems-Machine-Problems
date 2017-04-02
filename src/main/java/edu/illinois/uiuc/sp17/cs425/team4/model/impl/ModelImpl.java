package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import edu.illinois.uiuc.sp17.cs425.team4.model.KVRawOpResult;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeyDeleteMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeyReadMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeyWriteMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.TextMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.ValueMessage;
import net.jcip.annotations.ThreadSafe;

/**
 * An implementation of Model factory.
 * 
 * @author bbassi2
 */
@ThreadSafe
public class ModelImpl implements Model {
	
	@Override
	public Message createNoOpMessage(Process originatingProcess) {
		return new NoOpMessageImpl(originatingProcess);
	}

	@Override
	public Message createNoOpMessage(Process originatingProcess, UUID uId) {
		return new NoOpMessageImpl(originatingProcess, uId);
	}

	@Override
	public TextMessage createTextMessage(String text, Process originatingProcess) {
		return new TextMessageImpl(originatingProcess, text);
	}

	@Override
	public TextMessage createTextMessage(String text, Process originatingProcess, UUID uId) {
		return new TextMessageImpl(originatingProcess, text, uId);
	}

	@Override
	public Message createProcessJoinedMessage(Process originatingProcess) {
		return new ProcessJoinedMessageImpl(originatingProcess);
	}

	@Override
	public Message createProcessJoinedMessage(Process originatingProcess, UUID uId) {
		return new ProcessJoinedMessageImpl(originatingProcess, uId);
	}

	@Override
	public Message createProcessLeftMessage(Process originatingProcess) {
		return new ProcessLeftMessageImpl(originatingProcess);
	}

	@Override
	public Message createProcessLeftMessage(Process originatingProcess, UUID uId) {
		return new ProcessLeftMessageImpl(originatingProcess, uId);
	}

	@Override
	public Process createProcess(InetAddress inetAddress, int port, String displayName) {
		return new ProcessImpl(inetAddress, port, displayName);
	}

	@Override
	public Process createProcess(InetAddress inetAddress, int port, String displayName, UUID uId) {
		return new ProcessImpl(inetAddress, port, displayName, uId);
	}

	@Override
	public Message createPingMessage(Process originatingProcess) {
		return new PingMessageImpl(originatingProcess);
	}

	@Override
	public Message createPingMessage(Process originatingProcess, UUID uId) {
		return new PingMessageImpl(originatingProcess, uId);
	}

	@Override
	public Message createAckMessage(Process originatingProcess) {
		return new AckMessageImpl(originatingProcess);
	}

	@Override
	public Message createAckMessage(Process originatingProcess, UUID uId) {
		return new AckMessageImpl(originatingProcess, uId);
	}

	@Override
	public <K> KeyReadMessage<K> createKeyReadMessage(Process originatingProcess, K key, Long timestamp) {
		return new KeyReadMessageImpl<K>(originatingProcess, key, timestamp);
	}

	@Override
	public <K> KeyReadMessage<K> createKeyReadMessage(Process originatingProcess, K key, Long timestamp, UUID uId) {
		return new KeyReadMessageImpl<K>(originatingProcess, key, timestamp, uId);
	}

	@Override
	public <K, V> KeyWriteMessage<K, V> createKeyWriteMessage(Process originatingProcess, K key, V val, Long timestamp) {
		return new KeyWriteMessageImpl<K, V>(originatingProcess, key, val, timestamp);
	}

	@Override
	public <K, V> KeyWriteMessage<K, V> createKeyWriteMessage(Process originatingProcess, K key, V val, Long timestamp, UUID uId) {
		return new KeyWriteMessageImpl<K, V>(originatingProcess, key, val, timestamp, uId);
	}

	@Override
	public <V> ValueMessage<V> createValueMessage(Process originatingProcess, V val, Long timestamp) {
		return new ValueMessageImpl<V>(originatingProcess, val, timestamp);
	}

	@Override
	public <V> ValueMessage<V> createValueMessage(Process originatingProcess, V val, Long timestamp, UUID uId) {
		return new ValueMessageImpl<V>(originatingProcess, val, timestamp, uId);
	}

	@Override
	public <K> KeyDeleteMessage<K> createKeyDeleteMessage(Process originatingProcess, K key) {
		return new KeyDeleteMessageImpl<K>(originatingProcess, key);
	}

	@Override
	public <K> KeyDeleteMessage<K> createKeyDeleteMessage(Process originatingProcess, K key, UUID uId) {
		return new KeyDeleteMessageImpl<K>(originatingProcess, key, uId);
	}
	
	public <R> KVRawOpResult<R> createKVRawOpResult(boolean succeeded, 
			Map<Process, R> completed, 
			Map<Process, Throwable> failures,
			Map<Process, Future<R>> inProgress) {
		return new KVRawOpResultImpl<>(succeeded, completed, failures, inProgress);
	}

	@Override
	public <V> ValueMessage<V> createNullValueMessage(Process originatingProcess) {
		return new ValueMessageImpl<V>(originatingProcess);
	}

	@Override
	public <V> ValueMessage<V> createNullValueMessage(Process originatingProcess, UUID uId) {
		return new ValueMessageImpl<V>(originatingProcess, uId);
	}

}