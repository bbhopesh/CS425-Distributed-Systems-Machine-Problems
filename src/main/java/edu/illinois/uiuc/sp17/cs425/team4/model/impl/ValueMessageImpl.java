package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.ValueMessage;

class ValueMessageImpl<V> extends MessageBaseImpl implements ValueMessage<V> {

	private V value;
	
	private Long timestamp;

	ValueMessageImpl(Process originatingSource, V value, Long timestamp) {
		super(MessageType.VALUE, originatingSource);
		this.value = value;
		this.timestamp = timestamp;
	}
	
	ValueMessageImpl(Process originatingSource, V value, Long timestamp, UUID uId) {
		super(MessageType.VALUE, originatingSource, uId);
		this.value = value;
		this.timestamp = timestamp;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -7347803406651647781L;


	@Override
	public V getValue() {
		return this.value;
	}

	@Override
	public Long getTimestamp() {
		return this.timestamp;
	}
}
