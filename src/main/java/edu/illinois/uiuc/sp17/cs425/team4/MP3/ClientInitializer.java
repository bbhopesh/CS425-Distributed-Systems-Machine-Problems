package edu.illinois.uiuc.sp17.cs425.team4.MP3;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KeyLockManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.KVLocalDataStore;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.KeyLockManagerClient;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SequentialKVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessengerBuilder;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class ClientInitializer {
	private static final int THREAD_POOL_SIZE = 100;

	private final Set<Process> servers;
	private final Process myIdentity;
	private final ExecutorService threadPool;
	private final Messenger messenger;
	private final Model model;
	private final Process lockService;
	private final int remoteReadWriteTimeout;
	
	private KVLocalDataStore<String, String> localDataStore;
	private KVRawDataManager<String, String> rawDataManager;
	private KeyLockManager<Pair<Process, String>> keyLockManager;
	private ClientCommandLineInterface cmd;

	
	
	public ClientInitializer(Set<Process> servers,
			Process myIdentity,
			Model model,
			Process lockService,
			int remoteReadWriteTimeout
			) throws IOException {
		this.servers = servers;
		this.myIdentity = myIdentity;
		this.model = model;
		this.lockService = lockService;
		this.remoteReadWriteTimeout = remoteReadWriteTimeout;
		
		// Messenger.
		this.threadPool = createThreadPool();
		this.messenger = createTCPMessenger();
		this.messenger.initialize();
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
		// Start interface.
		this.cmd.startInterface();
	}
	
	private void initializeState() {
		createLocalDataStore();
		createRawDataManager();
		createKeyLockManager();
		createClientCmd();
	}
	
	
	private void createLocalDataStore() {
		this.localDataStore = new KVLocalDataStore<String, String>();
	}
	
	private void createRawDataManager() {
		this.rawDataManager =  new SequentialKVRawDataManager<String, String>(this.localDataStore, 
																this.messenger,
																this.model, this.myIdentity);
	}
	
	private void createKeyLockManager() {
		this.keyLockManager = new KeyLockManagerClient<>(this.messenger, this.myIdentity,
															this.model, this.lockService);
	}
	private void createClientCmd() {
		this.cmd = new ClientCommandLineInterface(this.servers, this.keyLockManager, 
															this.rawDataManager, this.remoteReadWriteTimeout);
	}
}
