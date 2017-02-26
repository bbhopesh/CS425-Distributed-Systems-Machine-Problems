package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Comparator;
import java.util.PriorityQueue;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.Application;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.Multicast;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class IsisTotallyOrderedMC implements Multicast, Application, MessageListener {
	
	/** Listener Identifier. */
	private static final String S_IDENTIFIER = "IsisTotallyOrderedMC";
	/** Listener Identifier. */
	private static final MessageListenerIdentifier IDENTIFIER = 
			new MessageListenerIdentifierImpl(S_IDENTIFIER);
	
	/** Group Manager. Used for staying up to date about group members.*/
	private final GroupManager groupManager;
	/** Basic (unreliable) multicast on which this multicast is built. */
	private final Multicast reliableMulticast;
	/** Applications registered with this multicast. */
	private Application registeredApplication;
	/** messenger used in the first round of message multicasting in isis algorithm */
	private final Messenger messenger;
	/** a priority queue used to store the messages before deliver it */
	PriorityQueue<Message> holdBackQueue;
	/** largest agreed sequence number */
	private int largestAgreedNum;
	/** largest proposed sequence number*/
	private int largestProposedNum;
	/** Priority class type, use to query in metadate*/
	private static final Class<Pair<String, Integer>> clazz = (Class) Pair.class;
	/** message priority keyword */
	private static final String PRIORITY = "MESSAGE_PRIORITY";
	/** message agreement keyword */
	private static final String AGREED = "MESSAGE_AGREEMENT";
	private static final int FIRST_ROUND_TIMEOUT = 4000;
	
	/** Priority comparator anonymous class implementation */
	public static Comparator<Message> pComparator = new Comparator<Message>(){	
		@Override
		public int compare(Message c1, Message c2) {
			// TODO Auto generated.
			if(c1 == null || c2 == null) {
				throw new IllegalArgumentException("message to compare should not be null ");
			}
			Pair<String, Integer> c1p  =  c1.getMetadata().get(clazz, PRIORITY);
			Pair<String, Integer> c2p  =  c2.getMetadata().get(clazz, PRIORITY);
			if(c1p == null || c2p == null) {
				throw new IllegalArgumentException("message must contain priority property");
			}
			int order = c1p.getRight().compareTo(c2p.getRight());
			if(order == 0) {
				//If tie, use process name to break the tie
				return c1p.getLeft().compareTo(c2p.getLeft());
			}
            return order;
        }
	};
	
	
	public IsisTotallyOrderedMC(Multicast reliable,Messenger messenger ,GroupManager groupManager){
		this.groupManager = groupManager;
		this.reliableMulticast = reliable;
		this.reliableMulticast.registerApplication(this);
		this.messenger = messenger;
		this.messenger.registerListener(this);
		this.holdBackQueue =  new PriorityQueue<Message>(pComparator);
	}
	

	@Override
	public void multicast(Message m) {
		// TODO Auto-generated method stub
		m.setMessageListenerId(IDENTIFIER);
		m.getMetadata().setProperty(AGREED, new Boolean(false));
		m.getMetadata().setProperty(PRIORITY,Pair.of(this.groupManager.getMyIdentity().getDisplayName(),new Integer(-1)));	
		
		for(Process p : groupManager.getGroupMembers()) {
			try {
				// timeout added by Bhopesh, Randolph needs to think what to do in case message times out.
				Message replied = this.messenger.send(Pair.of(p, m), FIRST_ROUND_TIMEOUT);
				if(replied == null) {
					System.err.println("received null message");
				}
				int compareRes = pComparator.compare(m, replied);
				if(compareRes < 0) {
					Pair<String, Integer> newPriority  =  replied.getMetadata().get(clazz, PRIORITY);
					m.getMetadata().setProperty(PRIORITY, Pair.of(newPriority.getLeft(), newPriority.getRight()));	
				}
			} catch (ContextedRuntimeException e) {
				// the another end might failed, need to figure out a way to multicast this message
			}
		}
		//multicast the message with agreed priority
		reliableMulticast.multicast(m);
	}
	
	@Override
	public void deliver(Pair<Process, Message> incomingMessage) {
		//since the message sender is also one of groupMembers, the message should already be in holdBackQueue
		Message m = incomingMessage.getRight();
		Pair<String, Integer> finalPriority = m.getMetadata().get(clazz, PRIORITY);
		synchronized(this) {
			largestAgreedNum = finalPriority.getRight().intValue() > largestAgreedNum 
					? finalPriority.getRight().intValue() : largestAgreedNum;			
		}
		//Force to reorder in the priority queue
		if(holdBackQueue.remove(m)) {
			m.getMetadata().setProperty(AGREED, new Boolean(true));
			holdBackQueue.add(m);
		}else {
			throw new IllegalStateException("message should exist in holdback queue already");
		}
		
		//For every message stored,the newly agreed on priority could only be monotonically increasing
		//Therefore, change the priority of a hold-back message can only make it further back in the queue
		//Thus, every time the head of queue is agreed, it is safe to deliver the rest agreed messages  
		while(!holdBackQueue.isEmpty() && holdBackQueue.peek().getMetadata().getBoolean(AGREED)) {
			Message agreedMessage =  holdBackQueue.poll();
			this.registeredApplication.deliver(Pair.of(incomingMessage.getLeft(), agreedMessage));
		}
	}
	
	/**
	 * For any message received, reply with a proposed priority 
	 */
	@Override
	public void messageReceived(Pair<Process, Message> sourceAndMsg, ResponseWriter responseWriter) {
		int proposedNum = 0;
		synchronized(this) {
			proposedNum = largestAgreedNum >= largestProposedNum ? largestAgreedNum + 1 : largestProposedNum+1;
			largestProposedNum = proposedNum;
		}
		Configuration properties = sourceAndMsg.getRight().getMetadata();
		properties.setProperty(PRIORITY,Pair.of(this.groupManager.getMyIdentity().getDisplayName(),new Integer(proposedNum)));	
		responseWriter.writeResponse(sourceAndMsg.getRight());
		responseWriter.close();
		holdBackQueue.add(sourceAndMsg.getRight());
	}

	@Override
	public boolean registerApplication(Application application) {
		// TODO Auto-generated method stub
		if (this.registeredApplication != null) {
			throw new ContextedRuntimeException("Application can only be registered once.");
		}
		this.registeredApplication = application;
		return true;
	}

	@Override
	public MessageListenerIdentifier getIdentifier() {
		// TODO Auto-generated method stub
		return IDENTIFIER;
	}

}
