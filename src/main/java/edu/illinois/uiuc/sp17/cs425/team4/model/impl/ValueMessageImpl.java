package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.ValueMessage;

class ValueMessageImpl<V> extends MessageBaseImpl implements ValueMessage<V> {

	private V value;
	
	private Long timestamp;
	
	private boolean isNull;

	ValueMessageImpl(Process originatingSource, V value, Long timestamp) {
		super(MessageType.VALUE, originatingSource);
		this.value = checkForNull(value, "Value can't be null");
		this.timestamp = checkForNull(timestamp, "Timestamp can't be null.");
		this.isNull = false;
	}

	ValueMessageImpl(Process originatingSource, V value, Long timestamp, UUID uId) {
		super(MessageType.VALUE, originatingSource, uId);
		this.value = checkForNull(value, "Value can't be null");
		this.timestamp = checkForNull(timestamp, "Timestamp can't be null.");
		this.isNull = false;
	}
	
	ValueMessageImpl(Process originatingSource) {
		super(MessageType.VALUE, originatingSource);
		this.isNull = true;
	}
	
	ValueMessageImpl(Process originatingSource, UUID uId) {
		super(MessageType.VALUE, originatingSource, uId);
		this.isNull = true;
	}
	
	
	private <T> T checkForNull(T t, String string) {
		if (t == null) {
			throw new IllegalArgumentException(string);
		}
		return t;
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

	@Override
	public boolean isNull() {
		return this.isNull;
	}
}
