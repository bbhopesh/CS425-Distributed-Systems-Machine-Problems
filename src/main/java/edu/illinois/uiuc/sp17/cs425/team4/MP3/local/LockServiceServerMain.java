package edu.illinois.uiuc.sp17.cs425.team4.MP3.local;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.KeyLockManagerServer;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessengerBuilder;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;

public class LockServiceServerMain {
	private static Process myIdentity;
	private static int lockServicePort = 10005;
	private static String lockServiceName = "LockService";
	private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
	
	/** Model. */
	private static final Model MODEL = new ModelImpl();
	
	public static void main(String[] args) throws IOException {
		initializeMyself();
		Messenger messenger = createTCPMessenger();
		messenger.initialize();
		//KeyLockManagerServer<String> x = 
		new KeyLockManagerServer<String>(messenger, myIdentity, MODEL);
	}
	
	/**
	 * Initialize myself.
	 * @throws UnknownHostException 
	 */
	private static void initializeMyself() throws UnknownHostException {
		myIdentity = MODEL.createProcess(
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
