package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.ValuesMessage;

class ValuesMessageImpl<K, V> extends MessageBaseImpl implements ValuesMessage<K, V> {

	private final Map<K, NavigableMap<Long,V>> values;

	ValuesMessageImpl(Process originatingSource, Map<K, NavigableMap<Long,V>> values) {
		super(MessageType.VALUES, originatingSource);
		this.values = values;
	}

	ValuesMessageImpl(Process originatingSource, Map<K, NavigableMap<Long,V>> values, UUID uId) {
		super(MessageType.VALUES, originatingSource, uId);
		this.values = values;
	}
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -7347803406651647781L;

	@Override
	public Map<K, NavigableMap<Long, V>> getValues() {
		return this.values;
	}

}