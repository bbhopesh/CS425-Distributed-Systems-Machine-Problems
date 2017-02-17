package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.PointToPointMessenger;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class PointToPointMessengerImpl implements PointToPointMessenger {

	private final Messenger messenger;

	public PointToPointMessengerImpl(Messenger messenger) {
		this.messenger = messenger;
	}
	
	@Override
	public Message send(Pair<Process, Message> dstnAndMsg) throws ContextedRuntimeException {
		return this.messenger.send(dstnAndMsg);
	}

}
