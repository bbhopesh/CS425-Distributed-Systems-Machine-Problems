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
	/** This process. */
	private Process myIdentity;
	
	public ModelImpl(Process myIdentity) {
		this.myIdentity = myIdentity;
	}
	
	public ModelImpl() {
		
	}
	
	@Override
	public Message createNoOpMessage() {
		checkMyIdentity();
		return new NoOpMessageImpl(this.myIdentity);
	}

	@Override
	public Message createNoOpMessage(UUID uId) {
		checkMyIdentity();
		return new NoOpMessageImpl(this.myIdentity, uId);
	}

	@Override
	public TextMessage createTextMessage(String text) {
		checkMyIdentity();
		return new TextMessageImpl(this.myIdentity, text);
	}

	@Override
	public TextMessage createTextMessage(String text, UUID uId) {
		checkMyIdentity();
		return new TextMessageImpl(this.myIdentity, text, uId);
	}

	@Override
	public Message createProcessJoinedMessage() {
		checkMyIdentity();
		return new ProcessJoinedMessageImpl(this.myIdentity);
	}

	@Override
	public Message createProcessJoinedMessage(UUID uId) {
		checkMyIdentity();
		return new ProcessJoinedMessageImpl(this.myIdentity, uId);
	}

	@Override
	public Message createProcessLeftMessage() {
		checkMyIdentity();
		return new ProcessLeftMessageImpl(this.myIdentity);
	}

	@Override
	public Message createProcessLeftMessage(UUID uId) {
		checkMyIdentity();
		return new ProcessLeftMessageImpl(this.myIdentity, uId);
	}

	@Override
	public Process createProcess(InetAddress inetAddress, int port, String displayName) {
		return new ProcessImpl(inetAddress, port, displayName);
	}

	@Override
	public Process createProcess(InetAddress inetAddress, int port, String displayName, UUID uId) {
		return new ProcessImpl(inetAddress, port, displayName, uId);
	}

	@Override
	public void setMyIdentity(Process myIdentity) {
		if(this.myIdentity != null) {
			throw new IllegalStateException("MyIdentity cannot be set multiple times.");
		}
		this.myIdentity = myIdentity;
	}

	@Override
	public boolean containsSameProcess(Process proc){
		if(this.myIdentity == null) {
			throw new IllegalStateException("MyIdentity must be set first by calling setMyIdentity.");
		}
		return myIdentity.equals(proc);
	}
	
	private void checkMyIdentity() {
		if(this.myIdentity == null) {
			throw new IllegalStateException("MyIdentity must be set first by calling setMyIdentity.");
		}
	}

}