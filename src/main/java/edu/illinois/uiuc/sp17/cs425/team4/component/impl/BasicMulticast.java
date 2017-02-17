package edu.illinois.uiuc.sp17.cs425.team4.component.impl;


import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.Application;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupChangeListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageReceiptListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.Multicast;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
/**
 * Basic unreliable multicast.
 * 
 * @author bbassi2
 */
public class BasicMulticast implements Multicast, MessageReceiptListener, GroupChangeListener {
	
	/** Group Manager. Used for staying up to date about group members.*/
	private final GroupManager groupManager;
	/** Messenger. Used to send and receive messages. */
	private final Messenger mesenger;
	/** Model. */
	private final Model model;
	/** Applications registered with this multicast. */
	private Application registeredApplication;

	
	public BasicMulticast(GroupManager groupManager, Messenger mesenger) {
		this.groupManager = groupManager;
		this.mesenger = mesenger;
		this.mesenger.registerListener(this);
		this.mesenger.initialize();
		this.model = new ModelImpl();
	}
	
	@Override
	public void messageReceived(Pair<Process, Message> sourceAndMsg, ResponseWriter responseWriter) {
		this.registeredApplication.deliver(sourceAndMsg);
		// Don't have anything to respond. Just close the responsWriter.
		responseWriter.close();
	}

	@Override
	public List<Pair<Process, Exception>> multicast(Message m) {
		List<Pair<Process, Exception>> failedMessages = 
				new LinkedList<Pair<Process, Exception>>();
		// TODO Semantics of what happens when group changes while iterating are not clear.
		// We might have to change code according to requirements there.
		// Because it doesn't matter for MP1, leaving it as it is for now.
		
		for (Process p: this.groupManager.getGroupMembers()) {
			// sending message to group members in a loop.
			// TODO Might want to consider sending asynchronously on different threads...
			// ...Just an idea, didn't give it much thought yet.
			try {
				this.mesenger.send(Pair.of(p, m));
			} catch (ContextedRuntimeException e) {
				failedMessages.add(Pair.of(p, e));
			}
		}
		return failedMessages;
	}

	@Override
	public boolean registerApplication(Application application) {
		if (this.registeredApplication != null) {
			throw new ContextedRuntimeException("Application can only be registered once.");
		}
		this.registeredApplication = application;
		return true;
	}

	@Override
	public void processJoined(Process j) {
		this.registeredApplication.deliver(
				Pair.of(j, model.createProcessJoinedMessage(this.groupManager.getMyIdentity())));
	}

	@Override
	public void processLeft(Process l) {
		this.registeredApplication.deliver(
				Pair.of(l, model.createProcessLeftMessage(this.groupManager.getMyIdentity())));
		
	}

	@Override
	public void notifyFailure(Pair<Pair<Process, Message>, Message> failedMsg, Exception exception) {
		// No Op
	}

}
