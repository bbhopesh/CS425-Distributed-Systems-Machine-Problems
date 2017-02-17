package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.Application;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.Multicast;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import net.jcip.annotations.NotThreadSafe;

/**
 * All to all reliable multicast. Uses basic multicast as building block.
 * Reliable only means that it can deal with process failure, network failures are
 * not handled and totally depend on the underlying basic multicast used.
 * 
 * @author bbassi2
 */
@NotThreadSafe
public class AllToAllReliableMulticast implements Multicast, Application {

	/** Group Manager. Used for staying up to date about group members.*/
	private final GroupManager groupManager;
	/** Basic (unreliable) multicast on which this multicast is built. */
	private Multicast basicMulticast;
	/** Set of messages that are already received. */
	private Set<Message> receivedMessages;
	/** Applications registered with this multicast. */
	private Application registeredApplication;
	
	public AllToAllReliableMulticast(Multicast basicMulticast, GroupManager groupManager) {
		this.basicMulticast = basicMulticast;
		this.basicMulticast.registerApplication(this);
		this.groupManager = groupManager;
		// TODO As of now, not sure when to clear the set or if algorithm can
		// even work without us storing all received messages forever.
		// Think more and figure out a way later.
		this.receivedMessages = new HashSet<Message>();
	}
	
	@Override
	public Message deliver(Pair<Process, Message> incomingMessage) {
		// Algorithm in this method is copy of Fig 15.9 of Coulouris book.
		
		// B-Deliver.
		// Got message from the underlying basic multicast.
		// Forward to everyone so that the protocol behaves reliably.
		Process sender = incomingMessage.getLeft();
		Message message = incomingMessage.getRight();
		if(!this.receivedMessages.contains(message)) {
			// didn't receive this message already.
			// Add to received messages.
			this.receivedMessages.add(message);
			// B-Multicast it to rest of the group if I am not original sender.
			if (!this.groupManager.getMyIdentity()
					.equals(message.getOriginatingSource())) {
				this.basicMulticast.multicast(message);
			} else {
				// Do nothing because If I was the original sender, multicast originated from this.multicast.
			}
			// R-deliver to application.
			return this.registeredApplication.deliver(Pair.of(sender, message));
		}
		return null;
	}

	@Override
	public void multicast(Message m) {
		// Delegate.
		// This method is same as base multicast, reliability comes from the fact
		// that everyone will forward to everyone upon delivery.
		this.basicMulticast.multicast(m); // B-Multicast.
	}

	@Override
	public boolean registerApplication(Application application) {
		if (this.registeredApplication != null) {
			throw new ContextedRuntimeException("Application can only be registered once.");
		}
		this.registeredApplication = application;
		return true;
	}

}
