package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.KeyReadMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

class KeyReadMessageImpl<K> extends MessageBaseImpl implements KeyReadMessage<K> {

	private K key;
	
	private Long timestamp;

	KeyReadMessageImpl(Process originatingSource, K key, Long timestamp) {
		super(MessageType.KEY_READ, originatingSource);
		this.key = key;
		this.timestamp = timestamp;
	}
	
	KeyReadMessageImpl(Process originatingSource, K key, Long timestamp, UUID uId) {
		super(MessageType.KEY_READ, originatingSource, uId);
		this.key = key;
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
	public Long getTimestamp() {
		return this.timestamp;
	}
}
