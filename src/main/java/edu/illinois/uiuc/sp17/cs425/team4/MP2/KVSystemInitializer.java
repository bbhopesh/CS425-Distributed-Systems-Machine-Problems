package edu.illinois.uiuc.sp17.cs425.team4.MP2;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.Codec;
import edu.illinois.uiuc.sp17.cs425.team4.component.HashFunction;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.RingTopology;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.CassandraLikeRing;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.KVLocalDataStore;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.KVRingDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.KVSystemStabilizer;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.PlainVanillaStringCodec;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.ProcessCodec;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SHA1HashFunction;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SWIMFailureDetectorV2;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SequentialKVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SimpleKVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessengerBuilder;
import edu.illinois.uiuc.sp17.cs425.team4.exceptions.MessengerException;
import edu.illinois.uiuc.sp17.cs425.team4.model.GroupMembershipMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class KVSystemInitializer {
	private static final int THREAD_POOL_SIZE = 100;
	
	private final Set<Process> gatewayProcesses;
	private final int mBytes;
	private final Process myIdentity;
	private final int numFailures;
	private final ExecutorService threadPool;
	private final Messenger messenger;
	private final int requestTimeout;
	private final int tryCount;
	private final int batchSize;
	private final Model model;
	
	private Set<Process> initialGroupMembers;
	private KVDataManager<String, String> dataManager;
	private KVLocalDataStore<String, String> localDataStore;
	private KVRawDataManager<String, String> rawDataManager;
	private RingTopology<String> ringTopology;
	private KVRingDataPartitioner<String> ringDataPartitioner;
	private KVSystemStabilizer<String, String> sytemStabilizer;
	private KVCommandLineInterface cmd;
	private SWIMFailureDetectorV2 groupManager;
	
	// Swim parameters.
	private final int swimAckTimeout;
	private final int swimProtocolPeriod;
	private final double swimMinProtocolPeriod;
	private final int swimNumPingTargets;
	
	
	public KVSystemInitializer(Set<Process> gatewayProcesses,
			Process myIdentity,
			Model model,
			
			// KV parameters.
			int mBytes,
			int numFailures,
			int requestTimeout,
			int tryCount,
			int batchSize,
			
			// SWIM parameters below
			int swimAckTimeout,
			int swimProtocolPeriod,
			double swimMinProtocolPeriod,
			int swimNumPingTargets) throws IOException {
		this.gatewayProcesses = gatewayProcesses;
		this.myIdentity = myIdentity;
		this.model = model;
		
		// Messenger.
		this.threadPool = createThreadPool();
		this.messenger = createTCPMessenger();
		this.messenger.initialize();
				
		// KV parameters.
		this.mBytes = mBytes;
		this.numFailures = numFailures;
		this.requestTimeout = requestTimeout;
		this.tryCount = tryCount;
		this.batchSize = batchSize;
		
		// Swim parameters.
		this.swimAckTimeout = swimAckTimeout;
		this.swimProtocolPeriod = swimProtocolPeriod;
		this.swimMinProtocolPeriod = swimMinProtocolPeriod;
		this.swimNumPingTargets = swimNumPingTargets;
	}
	
	
	private ExecutorService createThreadPool() {
		return Executors.newFixedThreadPool(THREAD_POOL_SIZE);
	}
	
	private Messenger createTCPMessenger() throws IOException {
		TCPMessengerBuilder builder = new TCPMessengerBuilder()
				.setThreadPool(this.threadPool)
				.setBindAddr(this.myIdentity.getInetAddress())
				.setPort(this.myIdentity.getPort())
				.setMyIdentity(this.myIdentity)
				.setBacklog(50)
				.setMessageAdaptor(new TCPMessageAdaptor());
		return builder.build();
	}

	public void initialize() throws InterruptedException {
		// Initialize/Create various objects needed for system.
		initializeState();
		// Pull data from relevant peers.
		pullData();
		// I have all required objects and data by now, join the system.
		joinSystem();
	}
	
	private void initializeState() {
		initializeGroupMembers();
		createRing();
		createDataPartitioner();
		createLocalDataStore();
		createRawDataManager();
		createDataManager();
		createGroupManager();
		createSystemStabilizer();
		createKVCmd();
	}
	
	private void pullData() {
		Map<String, NavigableMap<Long, String>> myData = this.dataManager.readMyData();
		this.localDataStore.write(myData);
	}
	
	private void joinSystem() throws InterruptedException {
		// Register with group manager to be up to date with group membership changes.
		this.groupManager.registerGroupChangeListener(this.sytemStabilizer);
		// Initialize group manager.
		this.groupManager.initialize();
		// Inform others.
		informGroup();
		// wait for minimum number of processes to join the system.
		waitForMinProcessesToJoinSystem();
		// Initialize command line interface.
		this.cmd.startInterface();
	}

	private void initializeGroupMembers() {
		this.initialGroupMembers = new HashSet<Process>();
		// Try to fetch group membership list.
		Message processJoiningMsg = this.model.createProcessJoiningMessage(this.myIdentity);
		stampIdentifier(processJoiningMsg);
		for (Process p: this.gatewayProcesses) {
			if (p.equals(this.myIdentity)) continue;
			try {
				Message response = this.messenger.send(Pair.of(p, processJoiningMsg), this.requestTimeout);
				GroupMembershipMessage groupMembershipMessage = (GroupMembershipMessage) response;
				this.initialGroupMembers = groupMembershipMessage.getGroupMembers();
			} catch (MessengerException e) {
			}
		}
		// Add yourself to group membership.
		this.initialGroupMembers.add(this.myIdentity);
		
	}

	private void createRing() {
		HashFunction hashFunction = new SHA1HashFunction();
		Codec<String> strCodec = new PlainVanillaStringCodec();
		Codec<Process> processCodec = new ProcessCodec();
		this.ringTopology =  new CassandraLikeRing<String>
									(this.initialGroupMembers, hashFunction, this.mBytes, strCodec, processCodec);
	}
	
	private void createDataPartitioner() {
		this.ringDataPartitioner = new KVRingDataPartitioner<String>(this.ringTopology, this.numFailures);
	}
	
	private void createLocalDataStore() {
		this.localDataStore = new KVLocalDataStore<String, String>();
	}
	
	private void createRawDataManager() {
		this.rawDataManager =  new SequentialKVRawDataManager<String, String>(this.localDataStore, 
																this.messenger,
																this.model, this.myIdentity);
	}
	
	private void createDataManager() {
		this.dataManager = new SimpleKVDataManager<String, String>
									(this.myIdentity, this.rawDataManager, this.ringDataPartitioner,
											this.requestTimeout, this.tryCount);
	}
	
	private void createGroupManager() {
		this.groupManager = new SWIMFailureDetectorV2
				(this.myIdentity, this.initialGroupMembers, this.messenger, 
						this.swimAckTimeout, this.swimProtocolPeriod, 
						this.swimMinProtocolPeriod, this.swimNumPingTargets, this.threadPool);
	}
	
	private void createSystemStabilizer() {
		this.sytemStabilizer = new KVSystemStabilizer<String, String>
						(this.ringTopology, this.numFailures, this.rawDataManager,
								this.myIdentity, this.requestTimeout, this.tryCount);
	}

	private void createKVCmd() {
		this.cmd = new KVCommandLineInterface(this.dataManager, this.ringDataPartitioner,this.batchSize);
	}
	
	private void informGroup() {
		// TODO Maybe, I should use reliable multicast to inform the group that I have joined the system.
		// but for MP2, we can't have failures while a node is joining, so it's okay for now.
		Message processJoinedMsg = this.model.createProcessJoinedMessage(this.myIdentity);
		stampIdentifier(processJoinedMsg);
		for (Process p: this.initialGroupMembers) {
			if (p.equals(this.myIdentity)) continue;
			// TODO if remote peer has failed since we started iterating, following call will fail.
			// However, during MP demo, when a process is joining we won't have failures, so we are good for now.
			this.messenger.send(Pair.of(p, processJoinedMsg), this.requestTimeout);
		}
	}
	
	private void waitForMinProcessesToJoinSystem() throws InterruptedException {
		int minProcesses = this.numFailures + 1;
		
		if (this.groupManager.getGroupMembers().size() < minProcesses) {
			System.err.println(
					String.format("Waiting for atleast %s processes to come online. Current peer count: %s",
							minProcesses, this.groupManager.getGroupMembers().size()));
		}
		
		while (this.groupManager.getGroupMembers().size() < minProcesses) {
			Thread.sleep(100);
		}
		
	}
	
	private void stampIdentifier(Message msg) {
		msg.setMessageListenerId(SWIMFailureDetectorV2.IDENTIFIER);
	}
	
}
