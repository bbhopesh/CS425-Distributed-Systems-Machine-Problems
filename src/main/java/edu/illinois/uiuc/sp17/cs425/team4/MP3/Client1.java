package edu.illinois.uiuc.sp17.cs425.team4.MP3;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.illinois.uiuc.sp17.cs425.team4.component.KeyLockManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.KeyLockManagerClient;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessengerBuilder;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;

public class Client1 {

	private static Process myIdentity;
	private static Process lockService;
	
	private static int port = 10010;
	private static String name = "C1";
	
	private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
	
	private static int lockServicePort = 10005;
	private static String lockServiceName = "LockService";
	
	/** Model. */
	private static final Model MODEL = new ModelImpl();
	
	public static void main(String[] args) throws IOException, InterruptedException {
		initializeMyself();
		initializeLockService();
		Messenger messenger = createTCPMessenger();
		messenger.initialize();
		KeyLockManager<String> lockManager = new KeyLockManagerClient<String>(messenger, myIdentity, MODEL, lockService);
		TransactionCommandLineInterface cmd = new TransactionCommandLineInterface(lockManager);
		cmd.startInterface();
	}
	
	/**
	 * Initialize myself.
	 * @throws UnknownHostException 
	 */
	private static void initializeMyself() throws UnknownHostException {
		myIdentity = MODEL.createProcess(
								InetAddress.getByName("localhost"),
								port, name, new UUID(new Long(1), 02));
	}
	
	private static void initializeLockService() throws UnknownHostException {
		lockService = MODEL.createProcess(
								InetAddress.getByName("localhost"),
								lockServicePort, lockServiceName, new UUID(new Long(1), 02));
	}
	
	private static Messenger createTCPMessenger() throws IOException {
		TCPMessengerBuilder builder = new TCPMessengerBuilder()
				.setThreadPool(threadPool)
				.setBindAddr(myIdentity.getInetAddress())
				.setPort(myIdentity.getPort())
				.setMyIdentity(myIdentity)
				.setBacklog(50)
				.setMessageAdaptor(new TCPMessageAdaptor());
		return builder.build();
	}

}
