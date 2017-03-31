package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.KeyWriteMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

class KeyWriteMessageImpl<K, V> extends MessageBaseImpl implements KeyWriteMessage<K, V> {

	private K key;
	private V value;
	private Long timestamp;

	KeyWriteMessageImpl(Process originatingSource, K key, V value, Long timestamp) {
		super(MessageType.KEY_WRITE, originatingSource);
		this.key = key;
		this.value = value;
		this.timestamp = timestamp;
	}
	
	KeyWriteMessageImpl(Process originatingSource, K key, V value, Long timestamp, UUID uId) {
		super(MessageType.KEY_WRITE, originatingSource, uId);
		this.key = key;
		this.value = value;
		this.timestamp = timestamp;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -7347803406651647781L;

	@Override
	public K getKey() {
		return this.key;
	}

	@Override
	public V getValue() {
		return this.value;
	}
	
	@Override
	public Long getTimestamp() {
		return this.timestamp;
	}
}
