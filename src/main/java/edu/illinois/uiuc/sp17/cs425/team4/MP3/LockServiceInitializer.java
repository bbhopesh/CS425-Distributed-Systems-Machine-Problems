package edu.illinois.uiuc.sp17.cs425.team4.MP3;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.KeyLockManagerServer;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessengerBuilder;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class LockServiceInitializer {
	private static final int THREAD_POOL_SIZE = 100;

	private final ExecutorService threadPool;
	private final Messenger messenger;
	private final Process lockService;
	private final Model model;

	@SuppressWarnings("unused")
	private KeyLockManagerServer<Pair<Process, String>> keyLockServer;
	// Just creating messenger and instance of key lock server is enough to start it.
	
	public LockServiceInitializer(Process lockService, Model model) throws IOException {
		this.lockService = lockService;
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
				.setBindAddr(this.lockService.getInetAddress())
				.setPort(this.lockService.getPort())
				.setMyIdentity(this.lockService)
				.setBacklog(50)
				.setMessageAdaptor(new TCPMessageAdaptor());
		return builder.build();
	}
	
	public void initialize() {
		this.keyLockServer = new KeyLockManagerServer<Pair<Process, String>>
									(this.messenger, this.lockService, this.model);
	}
}
