package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.net.InetAddress;
import java.util.UUID;


import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.TextMessage;

/**
 * An implementation of Model factory.
 * 
 * @author bbassi2
 */
public class ModelImpl implements Model {

	@Override
	public Message createNoOpMessage(Process originatingSource) {
		return new NoOpMessageImpl(originatingSource);
	}

	@Override
	public Message createNoOpMessage(Process originatingSource, UUID uId) {
		return new NoOpMessageImpl(originatingSource, uId);
	}

	@Override
	public TextMessage createTextMessage(Process originatingSource, String text) {
		return new TextMessageImpl(originatingSource, text);
	}

	@Override
	public TextMessage createTextMessage(Process originatingSource, String text, UUID uId) {
		return new TextMessageImpl(originatingSource, text, uId);
	}

	@Override
	public Message createProcessJoinedMessage(Process originatingSource) {
		return new ProcessJoinedMessageImpl(originatingSource);
	}

	@Override
	public Message createProcessJoinedMessage(Process originatingSource, UUID uId) {
		return new ProcessJoinedMessageImpl(originatingSource, uId);
	}

	@Override
	public Message createProcessLeftMessage(Process originatingSource) {
		return new ProcessLeftMessageImpl(originatingSource);
	}

	@Override
	public Message createProcessLeftMessage(Process originatingSource, UUID uId) {
		return new ProcessLeftMessageImpl(originatingSource, uId);
	}

	@Override
	public Process createProcess(InetAddress inetAddress, int port) {
		return new ProcessImpl(inetAddress, port);
	}

	@Override
	public Process createProcess(InetAddress inetAddress, int port, UUID uId) {
		return new ProcessImpl(inetAddress, port, uId);
	}

}