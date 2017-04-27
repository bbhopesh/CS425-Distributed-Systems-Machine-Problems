package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.net.InetAddress;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

import edu.illinois.uiuc.sp17.cs425.team4.model.GroupMembershipMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.KVAsyncOpResult;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeyDeleteMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeysWriteMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.LockMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.KeysReadMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.LockActionType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.LockType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.CloseTransactionMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.TextMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;
import edu.illinois.uiuc.sp17.cs425.team4.model.ValuesMessage;
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
	public <K> KeysReadMessage<K> createKeysReadMessage(Process originatingProcess, Set<K> keys) {
		return new KeysReadMessageImpl<>(originatingProcess, keys);
	}
	
	@Override
	public <K> KeysReadMessage<K> createKeysReadMessage(Process originatingProcess, Set<K> keys, UUID uId) {
		return new KeysReadMessageImpl<>(originatingProcess, keys, uId);
	}


	@Override
	public <K, V> KeysWriteMessage<K, V> createKeysWriteMessage(Process originatingProcess, Map<K, NavigableMap<Long, V>> data) {
		return new KeysWriteMessageImpl<K, V>(originatingProcess, data);
	}

	@Override
	public <K, V> KeysWriteMessage<K, V> createKeysWriteMessage(Process originatingProcess, Map<K, NavigableMap<Long, V>> data, UUID uId) {
		return new KeysWriteMessageImpl<K, V>(originatingProcess, data, uId);
	}

	@Override
	public <K, V> ValuesMessage<K, V> createValueMessage(Process originatingProcess, Map<K, NavigableMap<Long,V>> values) {
		return new ValuesMessageImpl<K, V>(originatingProcess, values);
	}

	@Override
	public <K, V> ValuesMessage<K, V> createValueMessage(Process originatingProcess, Map<K, NavigableMap<Long,V>> values, UUID uId) {
		return new ValuesMessageImpl<K, V>(originatingProcess, values, uId);
	}

	@Override
	public <K> KeyDeleteMessage<K> createKeyDeleteMessage(Process originatingProcess, K key) {
		return new KeyDeleteMessageImpl<K>(originatingProcess, key);
	}

	@Override
	public <K> KeyDeleteMessage<K> createKeyDeleteMessage(Process originatingProcess, K key, UUID uId) {
		return new KeyDeleteMessageImpl<K>(originatingProcess, key, uId);
	}
	
	public <R> KVAsyncOpResult<R> createKVRawOpResult(boolean succeeded, 
			Map<Process, R> completed, 
			Map<Process, Throwable> failures,
			Map<Process, Future<R>> inProgress) {
		return new KVRawOpResultImpl<>(succeeded, completed, failures, inProgress);
	}

	@Override
	public Message createProcessJoiningMessage(Process originatingProcess) {
		return new ProcessJoiningMessageImpl(originatingProcess);
	}

	@Override
	public Message createProcessJoiningMessage(Process originatingProcess, UUID uId) {
		return new ProcessJoiningMessageImpl(originatingProcess, uId);
	}

	@Override
	public GroupMembershipMessage createGroupMembershipMessage(Process originatingProcess, Set<Process> groupMembers) {
		return new GroupMembershipMessageImpl(originatingProcess, groupMembers);
	}

	@Override
	public GroupMembershipMessage createGroupMembershipMessage(Process originatingProcess, Set<Process> groupMembers, UUID uId) {
		return new GroupMembershipMessageImpl(originatingProcess, groupMembers, uId);
	}

	@Override
	public Transaction createTransaction(Process owner, String displayName) {
		return new TransactionImpl(owner, displayName);
	}

	@Override
	public Transaction createTransaction(Process owner, String displayName, UUID uId) {
		return new TransactionImpl(owner, displayName, uId);
	}

	@Override
	public <K> LockMessage<K> createLockMessage(Process originatingSource, K key, Transaction transaction,
			LockType lockType, LockActionType actionType) {
		return new LockMessageImpl<K>(originatingSource, key, transaction, lockType, actionType);
	}

	@Override
	public <K> LockMessage<K> createLockMessage(Process originatingSource, K key, Transaction transaction,
			LockType lockType, LockActionType actionType, UUID uId) {
		return new LockMessageImpl<K>(originatingSource, key, transaction, lockType, actionType, uId);
	}

	@Override
	public CloseTransactionMessage createCloseTransactionMessage(Process originatingSource, Transaction transaction) {
		return new CloseTransactionMessageImpl(originatingSource, transaction);
	}

	@Override
	public CloseTransactionMessage createCloseTransactionMessage(Process originatingSource, Transaction transaction, UUID uId) {
		return new CloseTransactionMessageImpl(originatingSource, transaction, uId);
	}
}