package edu.illinois.uiuc.sp17.cs425.team4.component.impl;



import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.Application;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupChangeListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
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
	/** Listener Identifier. */
	private static final String S_IDENTIFIER = "BasicMulticast";
	/** Listener Identifier. */
	private static final MessageListenerIdentifier IDENTIFIER = 
			new MessageListenerIdentifierImpl(S_IDENTIFIER);

	
	public BasicMulticast(GroupManager groupManager, Messenger mesenger) {
		this.groupManager = groupManager;
		this.mesenger = mesenger;
		this.mesenger.registerListener(this);
		this.mesenger.initialize();
		this.model = new ModelImpl();
	}
	
	@Override
	public void messageReceived(Pair<Process, Message> sourceAndMsg, ResponseWriter responseWriter) {
		Message response = this.registeredApplication.deliver(sourceAndMsg);
		responseWriter.writeResponse(response);
		responseWriter.close();
	}

	@Override
	public void multicast(Message m) {
		// TODO Semantics of what happens when group changes while iterating are not clear.
		// We might have to change code according to requirements there.
		// Because it doesn't matter for MP1, leaving it as it is for now.
		
		// Stamp message listener id.
		m.setMessageListenerId(IDENTIFIER);
		for (Process p: this.groupManager.getGroupMembers()) {
			// sending message to group members in a loop.
			// TODO Might want to consider sending asynchronously on different threads...
			// ...Just an idea, didn't give it much thought yet.
			try {
				this.mesenger.send(Pair.of(p, m));
			} catch (ContextedRuntimeException e) {
				// ignore.
			}
		}
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
		// Ignores the message replied by application on receive of a process joined message.
		Message message = model.createProcessJoinedMessage(this.groupManager.getMyIdentity());
		message.setMessageListenerId(IDENTIFIER);
		this.registeredApplication.deliver(
				Pair.of(j, message));
	}

	@Override
	public void processLeft(Process l) {
		// Ignores the message replied by application on receive of a process left message.
		Message message = model.createProcessLeftMessage(this.groupManager.getMyIdentity());
		message.setMessageListenerId(IDENTIFIER);
		this.registeredApplication.deliver(
				Pair.of(l, message));
		
	}

	@Override
	public MessageListenerIdentifier getIdentifier() {
		return IDENTIFIER;
	}
}
