package edu.illinois.uiuc.sp17.cs425.team4.MP3;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.KVLocalDataStore;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SequentialKVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessengerBuilder;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class ServerInitializer {
	private static final int THREAD_POOL_SIZE = 100;

	private final Process myIdentity;
	private final ExecutorService threadPool;
	private final Messenger messenger;
	private final Model model;
	
	private KVLocalDataStore<String, String> localDataStore;
	@SuppressWarnings("unused")
	private KVRawDataManager<String, String> rawDataManager;
	// Just initializing messenger and creating raw data manager is enough for server.
	// Messenger will listen for requests and send to raw data manager.

	
	public ServerInitializer(Process myIdentity, Model model) throws IOException {
		this.myIdentity = myIdentity;
		this.model = model;
		
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
	
	public void initialize() {
		// Initialize/Create various objects needed for system.
		initializeState();
	}
	
	private void initializeState() {
		createLocalDataStore();
		createRawDataManager();
	}
	
	
	private void createLocalDataStore() {
		this.localDataStore = new KVLocalDataStore<String, String>();
	}
	
	private void createRawDataManager() {
		this.rawDataManager =  new SequentialKVRawDataManager<String, String>(this.localDataStore, 
																this.messenger,
																this.model, this.myIdentity);
	}
}

