package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.KeyDeleteMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

class KeyDeleteMessageImpl<K> extends MessageBaseImpl implements KeyDeleteMessage<K> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2619090766950802416L;
	private K key;

	KeyDeleteMessageImpl(Process originatingSource, K key) {
		super(MessageType.KEY_DELETE, originatingSource);
		this.key = key;
	}
	
	KeyDeleteMessageImpl(Process originatingSource, K key, UUID uId) {
		super(MessageType.KEY_DELETE, originatingSource, uId);
		this.key = key;
	}

	@Override
	public K getKey() {
		return this.key;
	}

}
