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

/**
 * SWIM style failure detector without indirect pings but k-random direct pings.
 * 
 * @author bbassi2
 */
public class SWIMFailureDetectorV2 implements GroupManager, MessageListener, Callable<Void> {
	/**  Logger. */
	private final static Logger LOG = Logger.getLogger(SWIMFailureDetectorV2.class.getName());
	/** Listener Identifier. */
	private static final String S_IDENTIFIER = "SwimV2FailureDetector";
	/** Listener Identifier. */
	private static final MessageListenerIdentifier IDENTIFIER = 
			new MessageListenerIdentifierImpl(S_IDENTIFIER);
	/** Message prop indicating the failed process. */
	private static final String FAILED_PROCESSES_PROP = S_IDENTIFIER  + " FailedProcesses";
	/**  My identity. */
	private final Process myIdentity;
	/** Set of group members. */
	private final Set<Process> groupMembers;
	/** Messenger. */
	private final Messenger messenger;
	/** List of listeners that are should be notified on group change. */
	private final List<GroupChangeListener> groupChangeListeners;
	/** Mapping of failed processes to the protocol period in which the failure was detected. */
	private final ConcurrentMap<Process,Integer> failures;
	/** Wait time in ms for acknowledgement. */
	private final int ackTimeout;
	/** Model. */
	private final Model model;
	/** Thread pool. */
	private final ExecutorService threadPool;
	/** Length of protocol period is ms. */
	private final int protocolPeriod;
	/** 
	 * Minimum length of protocol period as fraction of protocol period.
	 * If all tasks are done before this time, we still wait for this much time before proceeding to next round.
	 */
	private final double minProtocolPeriod;
	/** Minimum length of protocol period in ms. */
	private final int minTimeForProtocolPeriod;
	/** Number of pings. */
	private final int pingTargets;
	/** Number of protocol periods elapsed. */
	@GuardedBy("this")
	private int protocolPeriodsElapsed;
	/** Scheduler. */
	private ScheduledFuture<?> gcService;
	/** Future representing status of protocol. */
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
	
	// The commented out multi-threaded version is not working with delays in the network.
	// Some it takes many seconds for pings sent here(if started on diff threads.) to receive
	// at other end. Same problem was occurring with v1 swim failure detector for indirect messages.
	/*private void protocolPeriod() {
		// Action are implemented only for a single protocol period.
		// This function should be called periodically.
		Message pingMessage = createPingMessage();
		List<Process> kPingTargets = kRandomPeers(this.pingTargets);
		LOG.debug(String.format("[Picked %s as ping targets.]", kPingTargets));
		List<Future<Void>> pingResults = new ArrayList<Future<Void>>(kPingTargets.size());
		
		for (Process pingTarget: kPingTargets) {
			Callable<Void> pingTask = new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					try {
						Message ack = pingProcess(pingMessage, pingTarget, directPingTimeout());
						if (ack != null) {
							// Protocol period satisfied.
							LOG.debug(String.format("[Ack %s received from %s for ping %s.]",
									ack.getUUID(), pingTarget, pingMessage.getUUID()));
						} else {
							LOG.debug(String.format("[Didn't receive ack from %s for ping %s, declaring it as failed.]", 
									pingTarget, pingMessage.getUUID()));
							// Method should have returned by now, if it didn't then ping target has failed.
							markAsFailed(pingTarget);
						}
					} catch (Exception e) {
						// ignore. We can try pinging later.
					}
					return null;
				}
			};
			pingResults.add(this.threadPool.submit(pingTask));
		}
		// Wait for all pings to complete.
		int completed = 0;
		while (completed < pingResults.size()) {
			Iterator<Future<Void>> it = pingResults.iterator();
			while(it.hasNext()) {
				Future<Void> nxt = it.next();
				if (nxt.isDone()) {
					it.remove();
					completed++;
				}
			}
		}
	}*/
	
	/**
	 * Performs task of one protocol period.
	 * Picks k processes at random and pings.
	 * If ack is received from any process, that is marked a failed.
	 */
	private void protocolPeriod() {
		// Action are implemented only for a single protocol period.
		// This function should be called periodically.
		Message pingMessage = createPingMessage();
		List<Process> kPingTargets = kRandomPeers(this.pingTargets);
		LOG.debug(String.format("[Picked %s as ping targets.]", kPingTargets));
		
		for (Process pingTarget: kPingTargets) {
			Message ack = pingProcess(pingMessage, pingTarget, directPingTimeout());
			if (ack != null) {
				// Protocol period satisfied.
				LOG.debug(String.format("[Ack %s received from %s for ping %s.]",
						ack.getUUID(), pingTarget, pingMessage.getUUID()));
			} else {
				LOG.debug(String.format("[Didn't receive ack from %s for ping %s, declaring it as failed.]", 
						pingTarget, pingMessage.getUUID()));
				// Method should have returned by now, if it didn't then ping target has failed.
				markAsFailed(pingTarget);
			}
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
			// TODO this response should ideally be returned as null only when there was error in sendin message
			// and not some place in my code.
			// But assuming my code doesn't have such a bug, leaving it as it is for now.
			LOG.debug(e);
		}
		return response;
	}
	
	private List<Process> kRandomPeers(int k) {
		// gets k random targets from the group.
		// Randomly shuffle and pick k members.
		List<Process> groupMembersList = new ArrayList<Process>(this.groupMembers);
		Collections.shuffle(groupMembersList, new Random(this.myIdentity.hashCode() + System.currentTimeMillis()));
		int k1 = Math.min(k+1, groupMembersList.size());
		List<Process> groupMembers = groupMembersList.subList(0, k1);
		
		Iterator<Process> it = groupMembers.iterator();
		while(it.hasNext()) {
			Process nxt = it.next();
			if (nxt.equals(this.myIdentity)) {
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
