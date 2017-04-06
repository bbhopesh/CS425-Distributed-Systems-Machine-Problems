package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.KeysWriteMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

class KeysWriteMessageImpl<K, V> extends MessageBaseImpl implements KeysWriteMessage<K, V> {

	private final Map<K, NavigableMap<Long, V>> data;

	KeysWriteMessageImpl(Process originatingSource, Map<K, NavigableMap<Long, V>> data) {
		super(MessageType.KEYS_WRITE, originatingSource);
		this.data = data;
	}
	
	KeysWriteMessageImpl(Process originatingSource, Map<K, NavigableMap<Long, V>> data, UUID uId) {
		super(MessageType.KEYS_WRITE, originatingSource, uId);
		this.data = data;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -7347803406651647781L;

	@Override
	public Map<K, NavigableMap<Long, V>> getData() {
		return this.data;
	}

	
}
