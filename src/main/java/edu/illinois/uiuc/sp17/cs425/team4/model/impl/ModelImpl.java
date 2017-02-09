package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.net.InetAddress;
import java.util.UUID;


import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.TextMessage;
import net.jcip.annotations.NotThreadSafe;

/**
 * An implementation of Model factory.
 * 
 * @author bbassi2
 */
@NotThreadSafe
public class ModelImpl implements Model {
	
	@Override
	public Message createNoOpMessage(Process originatingProcess) {
		return new NoOpMessageImpl(originatingProcess);
	}

	@Override
	public Message createNoOpMessage(Process originatingProcess, UUID uId) {
		return new NoOpMessageImpl(originatingProcess, uId);
	}

	@Override
	public TextMessage createTextMessage(String text, Process originatingProcess) {
		return new TextMessageImpl(originatingProcess, text);
	}

	@Override
	public TextMessage createTextMessage(String text, Process originatingProcess, UUID uId) {
		return new TextMessageImpl(originatingProcess, text, uId);
	}

	@Override
	public Message createProcessJoinedMessage(Process originatingProcess) {
		return new ProcessJoinedMessageImpl(originatingProcess);
	}

	@Override
	public Message createProcessJoinedMessage(Process originatingProcess, UUID uId) {
		return new ProcessJoinedMessageImpl(originatingProcess, uId);
	}

	@Override
	public Message createProcessLeftMessage(Process originatingProcess) {
		return new ProcessLeftMessageImpl(originatingProcess);
	}

	@Override
	public Message createProcessLeftMessage(Process originatingProcess, UUID uId) {
		return new ProcessLeftMessageImpl(originatingProcess, uId);
	}

	@Override
	public Process createProcess(InetAddress inetAddress, int port, String displayName) {
		return new ProcessImpl(inetAddress, port, displayName);
	}

	@Override
	public Process createProcess(InetAddress inetAddress, int port, String displayName, UUID uId) {
		return new ProcessImpl(inetAddress, port, displayName, uId);
	}

}