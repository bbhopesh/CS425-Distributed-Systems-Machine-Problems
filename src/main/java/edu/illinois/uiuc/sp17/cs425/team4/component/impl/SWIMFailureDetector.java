package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import net.jcip.annotations.GuardedBy;

/**
 * SWIM style failure detector.
 * 
 * @author bbassi2
 */
public class SWIMFailureDetector implements GroupManager, MessageListener, Callable<Void> {
	/**  Logger. */
	private final static Logger LOG = Logger.getLogger(SWIMFailureDetector.class.getName());
	/** Listener Identifier. */
	private static final String S_IDENTIFIER = "SWIMFailureDetector";
	/** Listener Identifier. */
	private static final MessageListenerIdentifier IDENTIFIER = 
			new MessageListenerIdentifierImpl(S_IDENTIFIER);
	/** Message prop indicating if the message is indirect ping. */
	private static final String INDIRECT_PING_PROP = S_IDENTIFIER + " IndirectPing";
	/** Message prop indicating the failed process. */
	private static final String FAILED_PROCESSES_PROP = S_IDENTIFIER  + " FailedProcesses";
	/**  My identity. */
	private final Process myIdentity;
	/** Set of group members. */
	private final Set<Process> groupMembers;
	/** Initial size of the group. */
	private final int initialSize;
	/** Messenger. */
	private final Messenger messenger;
	/** List of listeners that are should be notified on group change. */
	private final List<GroupChangeListener> groupChangeListeners;
	/** Mapping of failed processes to the protocol period in which the failure was detected. */
	private final ConcurrentMap<Process,Integer> failures;
	/** Wait time in ms for direct acknowledgement. This should be substantially smaller than protocol period. */
	private final int ackWaitTime = 50;
	/** Model. */
	private final Model model;
	/** Thread pool. */
	private final ExecutorService threadPool;
	/**
	 * If we use piggybacked messages on top of ping-ack to disseminate failure detections,
	 * then after (lambda)log(N) periods, N^(-(2*lambda-2)) wouldn't have heard of update
	 * of detected failures. Hence, lambda dictates the level of consistency and can be set
	 * experimentally after trying various values. 
	 */
	private final int lambda;
	/** Length of protocol period is ms. */
	private final int protocolPeriod;
	/** 
	 * Minimum length of protocol period as fraction of protocol period.
	 * If all tasks are done before this time, we still wait for this much time before proceeding to next round.
	 */
	private final double minProtocolPeriod;
	/** Minimum length of protocol period in ms. */
	private final int minTimeForProtocolPeriod;
	/** Number of indirect pings. */
	private final int indirectPingTargetCount;
	@GuardedBy("this")
	/** Number of protocol periods elapsed. */
	private int protocolPeriodsElapsed;
	/** Scheduler. */
	private ScheduledFuture<?> gcService;
	/** Future representing status of protocol. */
	private Future<Void> groupManagerService;
	

	public SWIMFailureDetector(Process myIdentity, 
									Set<Process> groupMembers,
									Messenger messenger,
									int lambda,
									int protocolPeriod,
									double minProtocolPeriod,
									int indirectPingTargetCount,
									ExecutorService threadPool) {
		this.myIdentity = myIdentity;
		this.groupMembers = initializeGroupMembers(groupMembers);
		this.initialSize = this.groupMembers.size();
		this.messenger = messenger;
		this.messenger.registerListener(this);
		this.lambda = lambda;
		this.protocolPeriod = protocolPeriod;
		this.minProtocolPeriod = minProtocolPeriod;
		this.minTimeForProtocolPeriod = calulateMinTimeForProtocolPeriod();
		this.indirectPingTargetCount = indirectPingTargetCount;
		this.threadPool = threadPool;
		this.protocolPeriodsElapsed = 0;
		this.groupChangeListeners =  new CopyOnWriteArrayList<GroupChangeListener>();
		this.failures = new ConcurrentHashMap<Process,Integer>(this.groupMembers.size());
		this.model = new ModelImpl();
	}
	
	/**
	 * Calculate minimum duration of protocol period.
	 * @return minimum duration of protocol period.
	 */
	private int calulateMinTimeForProtocolPeriod() {
		return (int) (this.minProtocolPeriod*this.protocolPeriod);
	}

	/**
	 * Initialize group members.
	 * @param groupMembers group members.
	 * @return initialized group members.
	 */
	private Set<Process> initializeGroupMembers(Set<Process> groupMembers) {
		// Thread safe group members because accessed on many threads.
		Map<Process, Boolean> groupMap =  new ConcurrentHashMap<>(groupMembers.size());
		Set<Process> groupMemsConcurrentSet = Collections.newSetFromMap(groupMap);
		groupMemsConcurrentSet.addAll(groupMembers);
		return groupMemsConcurrentSet;
	}
	
	@Override
	public void initialize() {
		this.groupManagerService = this.threadPool.submit(this);
		
		// Start service for garbage collecting old failures.
		ScheduledExecutorService scheduleExecutor = 
				Executors.newSingleThreadScheduledExecutor();
		Runnable gcOlderFailures = new OldFailuresGarbageCollector();
		int delay = removeOlderThan()*this.protocolPeriod;
		int interval = removeOlderThan()*this.protocolPeriod;
		this.gcService = 
				scheduleExecutor.scheduleAtFixedRate(gcOlderFailures, 
				delay, interval, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Runs protocol period in infinite loop which can be broken by interrupting.
	 */
	@Override
	public Void call() throws InterruptedException {
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
			incrementProtocolPeriodNumber();
			int elapsedTime = (int)(System.currentTimeMillis() - start);
			LOG.debug(String.format("[Protocol period: %s, Time elapsed: %s ms]",this.protocolPeriodsElapsed,elapsedTime));
			long sleepTime = this.minTimeForProtocolPeriod - elapsedTime;
			
			if (sleepTime > 0) {
				LOG.debug(String.format("[Sleeping for %s ms]",sleepTime));
				Thread.sleep(sleepTime);
			}
			LOG.debug("-----------------------End of a protocol period.--------------------");
		}
	}
	
	/**
	 * Increments protocol periods elapsed.
	 */
	@GuardedBy("this")
	private synchronized void incrementProtocolPeriodNumber() {
		this.protocolPeriodsElapsed++;
	}
	
	/**
	 * Get current protocol period number.
	 * @return Current protocol period number.
	 */
	private synchronized int getProtocolPeriodNumber() {
		return this.protocolPeriodsElapsed;
	}
	
	/**
	 * Performs task of one protocol period.
	 * Picks a process at random and pings.
	 * If ack is received then does nothing for remainder of protocol.
	 * If ack is not received, then pings the process indirectly via k-other processes.
	 * Even if an indirect ack is not received, then process is marked as failed.
	 */
	private void protocolPeriod() {
		// Action are implemented only for a single protocol period.
		// This function should be called periodically.
		Process pingTarget = pickPingTarget();
		LOG.debug(String.format("[Picked %s as ping target.]", pingTarget));
		Message ack = pingProcess(createPingMessage(), pingTarget, directPingTimeout());
		if (ack != null) {
			// Protocol period satisfied.
			LOG.debug(String.format("[Direct ack received from %s, protocol period satisfies.]", pingTarget));
		} else {
			LOG.debug(String.format("[Didn't receive direct ack from %s, trying indirect ping]", pingTarget));
			// Ping via indirect path.
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
	
	/**
	 * Mark the given process failed.
	 * @param failure Failed process.
	 */
	private void markAsFailed(Process failure) {
		// Add to queue of failed processes.
		Integer protocolPeriodOfFailure = this.failures.putIfAbsent(failure, getProtocolPeriodNumber());
		if (protocolPeriodOfFailure == null) {
			// This is a new failure.
			// Remove from group.
			this.groupMembers.remove(failure);
			// inform listeners.
			informListeners(failure);
		}
	}

	/**
	 * Inform listeners about the process failure.
	 * @param failure Failed process.
	 */
	private void informListeners(Process failure) {
		for (GroupChangeListener listener: this.groupChangeListeners) {
			listener.processLeft(failure);
		}
	}
	
	/**
	 * Uses the future to check if we received indirect ping.
	 * @param indirectPing Indirect ping.
	 * @return true if indirect ping is received.
	 */
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
	
	/**
	 * Picks ping target randomly.
	 * @return randomly chosen ping target.
	 */
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
	
	/**
	 * Ping the provided process with given timeout.
	 * @param pingMessage Ping message.
	 * @param process Process to be pinged.
	 * @param timeout time to wait for ack.
	 * @return Ack message.
	 */
	private Message pingProcess(Message pingMessage, Process process, int timeout) {
		Message response = null;
		try {
			response = messenger.send(Pair.of(process, pingMessage),timeout);
		} catch (Exception e) {
			// ignore
		}
		return response;
	}
	
	/**
	 * Performs k indirect pings and returns futures representing status of ping.
	 * @param target the target process for which indirect ping is intended.
	 * @param k k.
	 * @return Futures representing status of ping.
	 */
	private List<Future<Message>> kIndirectPings(Process target, int k) {
		// Pick peers who will send indirect ping to target
		List<Process> kRandomPeers = kRandomPeers(k,target);
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
							indirectPingTimeout());
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
	
	private List<Process> kRandomPeers(int k, Process target) {
		// gets k random targets from the group.
		// Randomly shuffle and pick k members.
		List<Process> groupMembersList = new ArrayList<Process>(this.groupMembers);
		Collections.shuffle(groupMembersList);
		int k1 = Math.min(k+2, groupMembersList.size());
		List<Process> groupMembers = groupMembersList.subList(0, k1);
		
		Iterator<Process> it = groupMembers.iterator();
		while(it.hasNext()) {
			Process nxt = it.next();
			if (nxt.equals(this.myIdentity) || nxt.equals(target)) {
				it.remove();
			}
		}
		
		return groupMembers;
	}
	
	
	private Message createPingMessage() {
		Message ping = this.model.createPingMessage(this.myIdentity);
		stampMetaData(ping);
		return ping;
	}
	
	private Message createAckMessage() {
		Message ack = this.model.createAckMessage(this.myIdentity);
		stampMetaData(ack);
		return ack;
	}
	
	private void stampMetaData(Message message) {
		message.setMessageListenerId(getIdentifier());
		writePiggyBackedFailureInformation(message);
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
	public void close() {
		this.gcService.cancel(true);
		this.groupManagerService.cancel(true);
	}

	@Override
	public void messageReceived(Pair<Process, Message> sourceAndMsg, ResponseWriter responseWriter) {
		Process sender = sourceAndMsg.getLeft();
		Message msg = sourceAndMsg.getRight();
		LOG.debug(String.format("Received message %s from %s", msg.getUUID(), sender.getDisplayName()));
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
		readPiggyBackedFailureInformation(msg);
	}
	
	private void handleDirectPing(Process sender, Message msg, ResponseWriter responseWriter) {
		/*long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start <= 5000);*/
		Message ack = createAckMessage();
		LOG.debug(String.format("D Responding to message %s from %s with message %s",msg.getUUID(), sender.getDisplayName(), ack.getUUID()));
		responseWriter.writeResponse(ack);
		responseWriter.close();
	}
	
	private void handleIndirectPing(Process sender, Message msg, ResponseWriter responseWriter) {
		// Who should I ping on sender's behalf?
		Process indirectPingTarget = (Process) msg.getMetadata().getProperty(INDIRECT_PING_PROP);
		// Remove indirect ping property before forwarding so we dont get stuck in loop.
		msg.getMetadata().setProperty(INDIRECT_PING_PROP, null);
		Message response = pingProcess(msg, indirectPingTarget, indirectPingTimeout());
		LOG.debug(String.format("I Responding to message %s from %s with message %s",msg.getUUID(), sender.getDisplayName(), response.getUUID()));
		if (response != null) {
			stampMetaData(response);
			responseWriter.writeResponse(response);
		}
		responseWriter.close();
	}
	
	private boolean isIndirectPing(Message msg) {
		return msg.getMetadata().getProperty(INDIRECT_PING_PROP) != null;
	}

	private int directPingTimeout() {
		return this.ackWaitTime;
	}
	
	private int indirectPingTimeout() {
		return this.protocolPeriod - this.ackWaitTime;
	}
	
	@SuppressWarnings("unchecked")
	private void readPiggyBackedFailureInformation(Message received) {
		Object failedProcesses = 
				received.getMetadata().getProperty(FAILED_PROCESSES_PROP);
		if(failedProcesses != null) {
			if (failedProcesses instanceof List) {
				@SuppressWarnings("rawtypes")
				List f = (List) failedProcesses;
				for (Object o: f) {
					markAllAsFailure((ConcurrentMap<Process, Integer>) o);
				}
			} else {
				markAllAsFailure((Map<Process, Integer>) failedProcesses);
			}
		}
	}
	
	
	
	private void markAllAsFailure(Map<Process, Integer> failedProcesses) {
		for (Process failure: failedProcesses.keySet()) {
			markAsFailed(failure);
		}
	}
	
	private void writePiggyBackedFailureInformation(Message toSend) {
		LOG.debug(String.format("Piggybacking these failures %s on message %s", this.failures.keySet(), toSend.getUUID()));
		toSend.getMetadata()
				.addProperty(FAILED_PROCESSES_PROP, 
						Collections.unmodifiableMap(this.failures));
	}
	
	
	private int removeOlderThan() {
		return (int) (Math.log(initialSize)*lambda);
	}
	
	@Override
	public MessageListenerIdentifier getIdentifier() {
		return IDENTIFIER;
	}
	
	private class OldFailuresGarbageCollector implements Runnable {

		@Override
		public void run() {
			int removeOlderThan = removeOlderThan();
			int currentProtcolPeriod = getProtocolPeriodNumber();
			Iterator<Entry<Process, Integer>> itr = failures.entrySet().iterator();
			while(itr.hasNext()) {
				if(Thread.currentThread().isInterrupted()) return;
				Entry<Process, Integer> ent = itr.next();
				if (currentProtcolPeriod - ent.getValue() > removeOlderThan) {
					itr.remove();
				}
			}
		}
	}
	
}
