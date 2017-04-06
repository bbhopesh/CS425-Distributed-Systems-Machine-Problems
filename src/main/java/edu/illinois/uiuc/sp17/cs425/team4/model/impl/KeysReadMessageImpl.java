package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.util.Set;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.KeysReadMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

class KeysReadMessageImpl<K> extends MessageBaseImpl implements KeysReadMessage<K> {


	private Set<K> keys;

	KeysReadMessageImpl(Process originatingSource, Set<K> keys) {
		super(MessageType.KEYS_READ, originatingSource);
		this.keys = keys;
	}
	
	KeysReadMessageImpl(Process originatingSource, Set<K> keys, UUID uId) {
		super(MessageType.KEYS_READ, originatingSource, uId);
		this.keys = keys;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -7347803406651647781L;

	@Override
	public Set<K> readKeys() {
		return keys;
	}


}
