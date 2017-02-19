package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.component.GroupChangeListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListenerIdentifier;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.MessageType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;

public class SWIMFailureDetector implements GroupManager, MessageListener, Callable<Void> {
	private final static Logger LOG = Logger.getLogger(SWIMFailureDetector.class.getName());
	/** Listener Identifier. */
	private static final String S_IDENTIFIER = "SWIMFailureDetector";
	/** Listener Identifier. */
	private static final MessageListenerIdentifier IDENTIFIER = 
			new MessageListenerIdentifierImpl(S_IDENTIFIER);
	private static final String INDIRECT_PING_PROP = "IndirectPing";
	private final Process myIdentity;
	private final Set<Process> groupMembers;
	private final Messenger messenger;
	private final List<GroupChangeListener> groupChangeListeners;
	/** Wait time for direct acknowledgement as a fraction of protocol period. */
	private final double ackWaitTime = 0.1;
	/** Model. */
	private final Model model;
	private final ExecutorService threadPool;
	/**
	 * If we use piggybacked messages on top of ping-ack to disseminate failure detections,
	 * then after (lambda)log(N) periods, N^(-(2*lambda-2)) wouldn't have heard of update
	 * of detected failures. Hence, lambda dictates the level of consistency and can be set
	 * experimentally after trying various values. 
	 */
	private final int lambda;
	private final int protocolPeriod;
	private final int indirectPingTargetCount;
	
	private int protocolPeriodsElapsed;

	public SWIMFailureDetector(Process myIdentity, 
									Set<Process> groupMembers,
									Messenger messenger,
									int lambda,
									int protocolPeriod,
									int indirectPingTargetCount,
									ExecutorService threadPool) {
		this.myIdentity = myIdentity;
		this.groupMembers = initializeGroupMembers(groupMembers);
		this.messenger = messenger;
		this.messenger.registerListener(this);
		this.lambda = lambda;
		this.protocolPeriod = protocolPeriod;
		this.indirectPingTargetCount = indirectPingTargetCount;
		this.threadPool = threadPool;
		this.protocolPeriodsElapsed = 0;
		this.groupChangeListeners =  new CopyOnWriteArrayList<GroupChangeListener>();
		this.model = new ModelImpl();
	}

	private Set<Process> initializeGroupMembers(Set<Process> groupMembers) {
		// Thread safe group members because accessed on many threads.
		Map<Process, Boolean> groupMap =  new ConcurrentHashMap<>(groupMembers.size());
		Set<Process> groupMemsConcurrentSet = Collections.newSetFromMap(groupMap);
		groupMemsConcurrentSet.addAll(groupMembers);
		return groupMemsConcurrentSet;
	}
	
	@Override
	public void initialize() {
		this.threadPool.submit(this);
	}
	
	
	@Override
	public Void call() {
		// Keep running protocol and stop when interrupted.
		while(true) {
			if (Thread.currentThread().isInterrupted()) {
				return null;
			}
			if (this.groupMembers.size() <= 1) {
				LOG.info("No one else left in the group. Stopping failure detection protocol.");
				return null;
			}
			long start = System.currentTimeMillis();
			LOG.debug(String.format("[Starting protocol period: %s]", (this.protocolPeriodsElapsed + 1)));
			protocolPeriod();
			this.protocolPeriodsElapsed++;
			LOG.debug(String.format("[Protocol period: %s, Time elapsed: %s ms]", 
					this.protocolPeriodsElapsed, (System.currentTimeMillis() - start)));
			LOG.debug("-----------------------End of a protocol period.--------------------");
		}
	}
	
	public void protocolPeriod() {
		// Action are implemented only for a single protocol period.
		// This function should be called periodically.
		Process pingTarget = pickPingTarget();
		LOG.debug(String.format("[Picked %s as ping target.]", pingTarget));
		// Following call is supposed to timeout after ackWait*protocolPeriod ms.
		Message ack = pingProcess(pingTarget, directPingTimeout());
		if (ack != null) {
			// Protocol period satisfied.
			LOG.debug(String.format("[Direct ack received from %s, protocol period satisfies.]", pingTarget));
		} else {
			LOG.debug(String.format("[Didn't receive direct ack from %s, trying indirect ping]", pingTarget));
			// Ping via indirect path.
			// Following code is supposed to timeout after (1-ackWait)*protocolPeriod ms.
			// Futures should have result(either actual reply or null in (1-ackWait)*protocolPeriod ms)
			List<Future<Message>> indirectPings 
								= kIndirectPings(pingTarget, this.indirectPingTargetCount);
			List<Future<Message>> completed = new ArrayList<Future<Message>>(indirectPings.size());
			
			while (completed.size() != indirectPings.size()) { // While everything is not completed, keep checkin
				for (Future<Message> fut: indirectPings) {
					if(fut.isDone() && !completed.contains(fut)) {
						completed.add(fut);
						if(receivedIndirectAck(fut)){
							LOG.debug(String.format("[Received indirect ack from %s. Protocol satisfied.]", pingTarget));
							return; // protocol satisfied.
						}
					}
				}
			}
			LOG.debug(String.format("[Didn't receive direct or indirect ack from %s, declaring it as failed.]", pingTarget));
			// Method should have returned by now, if it didn't then ping target has failed.
			markAsFailed(pingTarget);
		}
	}
	
	private void markAsFailed(Process failure) {
		// TODO Auto-generated method stub
		// add to a queue of failed processes, notify listeners
		this.groupMembers.remove(failure);
		informListeners(failure);
	}

	private void informListeners(Process failure) {
		for (GroupChangeListener listener: this.groupChangeListeners) {
			listener.processLeft(failure);
		}
	}

	private boolean receivedIndirectAck(Future<Message> indirectPing) {
		try {
			Message indirectPingReply = indirectPing.get();
			if (indirectPingReply == null) {
				// didn't receive indirect ack
				return false;
			} else {
				// Received indirect ack
				// Protocol period satisfied.
				return true;
			}
		} catch (Exception e) {return false;} // ignore. can never reach here. Our callable catches all exceptions and returns null instead.
	}
	
	private Process pickPingTarget() {
		// Randomly shuffle and pick one element to ping.
		List<Process> groupMembersList = new ArrayList<Process>(this.groupMembers);
		Collections.shuffle(groupMembersList);
		
		while(true) {
			int nextElement = new Random().nextInt(groupMembersList.size());
			Process pingTarget = groupMembersList.get(nextElement);
			if (!pingTarget.equals(this.myIdentity)) {
				return pingTarget;
			}
		}
	}
	
	private Message pingProcess(Process process, int timeout) {
		Message pingMessage = createPingMessage();
		Message response = null;
		try {
			response = messenger.send(Pair.of(process, pingMessage),timeout);
		} catch (Exception e) {
			// ignore
		}
		return response;
	}
	
	private List<Future<Message>> kIndirectPings(Process target, int k) {
		// Pick peers who will send indirect ping to target
		List<Process> kRandomPeers = kRandomPeers(k);
		LOG.debug(String.format("[Indirect ping targets %s]", kRandomPeers));
		// Futures representing result of indirect pings.
		List<Future<Message>> indirectPingFuts = 
				new ArrayList<Future<Message>>(kRandomPeers.size());

		// Ping
		for (int i = 0; i < kRandomPeers.size(); i++) {
			// Callable to ping asyncronously.
			Callable<Message> indirectPingCallable = 
					createIndirectPingCallable(target, kRandomPeers.get(i));
			// Submit to threadpool.
			Future<Message> fut = this.threadPool.submit(indirectPingCallable);
			indirectPingFuts.add(fut);
		}
		return indirectPingFuts;
	}
	
	private Callable<Message> createIndirectPingCallable(
			Process target, Process via) {
		// Create indirect ping message.
		Message indirectPingMessage = createIndirectPingMessage(target);
		// Callable that sends indirect ping message.
		return new Callable<Message>() {
			@Override
			public Message call() throws Exception {
				// ackWaitTime fraction of protocol period was given to direct ping.
				// Rest of it is given to indirect pings.
				
				// We don't care if we didn't get reply because of exception or because process didn't reply.
				// If we don't get a response, we return null.
				Message response = null;
				try {
					response = messenger.send(Pair.of(via, indirectPingMessage), 
							(int)((1-ackWaitTime)*protocolPeriod));
				} catch (Exception e) {
					// ignore
				}
				return response;
			}
		};
	}
	
	private Message createIndirectPingMessage(Process target) {
		// Create ping message.
		Message pingMessage = createPingMessage();
		// Stamp that it is indirect.
		pingMessage.getMetadata().setProperty(INDIRECT_PING_PROP, target);
		return pingMessage;
	}
	
	private List<Process> kRandomPeers(int k) {
		// gets k random targets from the group.
		// Randomly shuffle and pick k members.
		List<Process> groupMembersList = new ArrayList<Process>(this.groupMembers);
		Collections.shuffle(groupMembersList);
		int k1 = Math.min(k, groupMembersList.size());
		return groupMembersList.subList(0, k1);
	}
	
	
	private Message createPingMessage() {
		Message message = this.model.createPingMessage(this.myIdentity);
		message.setMessageListenerId(getIdentifier());
		return message;
	}
	
	@Override
	public Process getMyIdentity() {
		return this.myIdentity;
	}

	@Override
	public Set<Process> getGroupMembers() {
		// the following set should be thread safe because underlying set is thread-safe.
		return Collections.unmodifiableSet(this.groupMembers);
	}

	@Override
	public boolean registerGroupChangeListener(GroupChangeListener groupChangeListener) {
		this.groupChangeListeners.add(groupChangeListener);
		return true;
	}

	@Override
	public void messageReceived(Pair<Process, Message> sourceAndMsg, ResponseWriter responseWriter) {
		Process sender = sourceAndMsg.getLeft();
		Message msg = sourceAndMsg.getRight();
		MessageType msgType = msg.getMessageType();
		if (msgType == MessageType.PING) {
			handlePing(sender, msg, responseWriter);
		} else {
			throw new ContextedRuntimeException("Can only handle ping messages.");
		}
	}

	private void handlePing(Process sender, Message msg, ResponseWriter responseWriter) {
		if (isIndirectPing(msg)) {
			handleIndirectPing(sender, msg, responseWriter);
		} else {
			handleDirectPing(sender, msg, responseWriter);
		}
	}
	
	private void handleDirectPing(Process sender, Message msg, ResponseWriter responseWriter) {
		/*long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start <= 5000);*/
		Message ack = this.model.createAckMessage(this.myIdentity);
		responseWriter.writeResponse(ack);
		responseWriter.close();
	}
	
	private void handleIndirectPing(Process sender, Message msg, ResponseWriter responseWriter) {
		// Who should I ping on sender's behalf?
		Process indirectPingTarget = (Process) msg.getMetadata().getProperty(INDIRECT_PING_PROP);
		Message response = pingProcess(indirectPingTarget, indirectPingTimeout());
		if (response != null) {
			responseWriter.writeResponse(response);
		}
		responseWriter.close();
	}
	
	private boolean isIndirectPing(Message msg) {
		return msg.getMetadata().getProperty(INDIRECT_PING_PROP) != null;
	}

	private int directPingTimeout() {
		return (int)(this.ackWaitTime*this.protocolPeriod);
	}
	
	private int indirectPingTimeout() {
		return (int)((1-this.ackWaitTime)*this.protocolPeriod);
	}
	
	@Override
	public MessageListenerIdentifier getIdentifier() {
		return IDENTIFIER;
	}
	
}
