package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

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

public class SWIMFailureDetectorV2 implements GroupManager, MessageListener, Callable<Void> {
	private final static Logger LOG = Logger.getLogger(SWIMFailureDetector.class.getName());
	/** Listener Identifier. */
	private static final String S_IDENTIFIER = "SwimV2FailureDetector";
	/** Listener Identifier. */
	private static final MessageListenerIdentifier IDENTIFIER = 
			new MessageListenerIdentifierImpl(S_IDENTIFIER);
	private static final String FAILED_PROCESSES_PROP = S_IDENTIFIER  + " FailedProcesses";
	private final Process myIdentity;
	private final Set<Process> groupMembers;
	private final Messenger messenger;
	private final List<GroupChangeListener> groupChangeListeners;
	private final ConcurrentMap<Process,Integer> failures;
	
	private final int ackTimeout;
	/** Model. */
	private final Model model;
	private final ExecutorService threadPool;
	 
	private final int protocolPeriod;
	private final double minProtocolPeriod;
	private final int minTimeForProtocolPeriod;
	private final int pingTargets;
	
	@GuardedBy("this")
	private int protocolPeriodsElapsed;
	private ScheduledFuture<?> gcService;
	private Future<Void> groupManagerService;
	
	

	public SWIMFailureDetectorV2(Process myIdentity, 
									Set<Process> groupMembers,
									Messenger messenger,
									int ackTimeout,
									int protocolPeriod,
									double minProtocolPeriod,
									int pingTargets,
									ExecutorService threadPool) throws FileNotFoundException {
		this.myIdentity = myIdentity;
		this.groupMembers = initializeGroupMembers(groupMembers);
		this.messenger = messenger;
		this.messenger.registerListener(this);
		this.ackTimeout = ackTimeout;
		this.protocolPeriod = protocolPeriod;
		this.minProtocolPeriod = minProtocolPeriod;
		this.minTimeForProtocolPeriod = calulateMinTimeForProtocolPeriod();
		this.pingTargets = pingTargets;
		this.threadPool = threadPool;
		this.protocolPeriodsElapsed = 0;
		this.groupChangeListeners =  new CopyOnWriteArrayList<GroupChangeListener>();
		this.failures = new ConcurrentHashMap<Process,Integer>(this.groupMembers.size());
		this.model = new ModelImpl();
	}
	
	private int calulateMinTimeForProtocolPeriod() {
		return (int) (this.minProtocolPeriod*this.protocolPeriod);
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
		this.groupManagerService = this.threadPool.submit(this);
	}
	
	
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
	
	
	private synchronized void incrementProtocolPeriodNumber() {
		this.protocolPeriodsElapsed++;
	}
	
	private synchronized int getProtocolPeriodNumber() {
		return this.protocolPeriodsElapsed;
	}
	
	
	private void protocolPeriod() {
		// Action are implemented only for a single protocol period.
		// This function should be called periodically.
		Process pingTarget = pickPingTarget();
		LOG.debug(String.format("[Picked %s as ping target.]", pingTarget));
		Message ack = pingProcess(createPingMessage(), pingTarget, directPingTimeout());
		if (ack != null) {
			// Protocol period satisfied.
			LOG.debug(String.format("[Ack received from %s.]", pingTarget));
		} else {
			LOG.debug(String.format("[Didn't receive ack from %s, declaring it as failed.]", pingTarget));
			// Method should have returned by now, if it didn't then ping target has failed.
			markAsFailed(pingTarget);
		}
	}
	
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

	private void informListeners(Process failure) {
		for (GroupChangeListener listener: this.groupChangeListeners) {
			listener.processLeft(failure);
		}
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
	
	private Message pingProcess(Message pingMessage, Process process, int timeout) {
		Message response = null;
		try {
			response = messenger.send(Pair.of(process, pingMessage),timeout);
		} catch (Exception e) {
			// TODO this response should ideally be returned as null only when there was error in sendin message
			// and not some place in my code.
			// But assuming my code doesn't have such a bug, leaving it as it is for now.
			LOG.debug(e);
		}
		return response;
	}
	
	private List<Process> kRandomPeers(int k, Process target) {
		// gets k random targets from the group.
		// Randomly shuffle and pick k members.
		List<Process> groupMembersList = new ArrayList<Process>(this.groupMembers);
		Collections.shuffle(groupMembersList, new Random(this.myIdentity.hashCode() + System.currentTimeMillis()));
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
		handleDirectPing(sender, msg, responseWriter);
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
	
	

	private int directPingTimeout() {
		return this.ackTimeout;
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
	
	@Override
	public MessageListenerIdentifier getIdentifier() {
		return IDENTIFIER;
	}
	
	
}
