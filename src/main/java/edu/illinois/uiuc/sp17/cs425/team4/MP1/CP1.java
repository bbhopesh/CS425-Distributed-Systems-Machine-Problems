package edu.illinois.uiuc.sp17.cs425.team4.MP1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.illinois.uiuc.sp17.cs425.team4.component.ChatApplication;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.Multicast;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.AllToAllReliableMulticast;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.BasicMulticast;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SimpleChatApplication;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.StaticGroupManagerImpl;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessengerBuilder;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;

/**
 * Main class to demo CP1.
 * 
 * @author bbassi2
 */
public class CP1 {
	/** My name in the group. */
	private static String myName;
	/** All members of the group. */
	private static Set<Process> groupMembers = new HashSet<Process>();
	/** Myself. */
	private static Process mySelf;
	
	public static void main(String[] args) throws IOException {
		// First argument sets name.
		myName = args[0];
		Model model = createModel();
		// Initialize the group.
		initializeGroupMembers(model);
		// Initialize yourself
		intializeMyself();
		model.setMyIdentity(mySelf);
		
		// Create a group manager.
		GroupManager groupManager = createGroupManager();

		// Create a messenger to communicate using TCP
		Messenger messenger = createTCPMessenger();
		// Create B-multicast.
		Multicast basicMulticast = createBasicMulticast(groupManager, messenger);
		// Create Reliable multicast.
		Multicast reliableMulticast = createReliableMulticast(basicMulticast,groupManager);
		
		// Create and start app.
		ChatApplication app =  new SimpleChatApplication(reliableMulticast, model);
		app.startChat();
		// App is done. exit system.
		System.exit(0);
	}
	
	/**
	 * Initialize various members of the group.
	 * @param model Model
	 * @throws UnknownHostException
	 */
	private static void initializeGroupMembers(Model model) throws UnknownHostException {
		Process m1 = model.createProcess(
				InetAddress.getByName("sp17-cs425-g04-01.cs.illinois.edu"),
				10005, "A", new UUID(01, 02));
		
		Process m2 = model.createProcess(
				InetAddress.getByName("sp17-cs425-g04-02.cs.illinois.edu"),
				10005, "B", new UUID(02, 02));
		
		Process m3 = model.createProcess(
				InetAddress.getByName("sp17-cs425-g04-03.cs.illinois.edu"),
				10005, "C", new UUID(03, 02));
		
		Process m4 = model.createProcess(
				InetAddress.getByName("sp17-cs425-g04-04.cs.illinois.edu"),
				10005, "D", new UUID(04, 02));
		
		Process m5 = model.createProcess(
				InetAddress.getByName("sp17-cs425-g04-05.cs.illinois.edu"),
				10005, "E", new UUID(05, 02));
		
		// If you copy to create more processes, don't forget to change UUID
		// UUID should be unique for each.
		
		groupMembers.addAll(Arrays.asList(
				m1
				,m2
				,m3
				,m4
				,m5
				));
	}
	
	private static void intializeMyself() {
		for (Process p: groupMembers) {
			if (p.getDisplayName().equals(myName)) {
				mySelf = p;
			}
		}
	}
	
	private static Model createModel() {
		return new ModelImpl();	
	}
	
	public static GroupManager createGroupManager() {
		return new StaticGroupManagerImpl(mySelf, groupMembers);
	}
	
	private static Messenger createTCPMessenger() throws IOException {
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		TCPMessengerBuilder builder = new TCPMessengerBuilder()
									.setThreadPool(threadPool)
									.setBindAddr(mySelf.getInetAddress())
									.setPort(mySelf.getPort())
									.setMyIdentity(mySelf)
									.setBacklog(50)
									.setMessageAdaptor(new TCPMessageAdaptor());
		return builder.build();
	}
	
	
	private static Multicast createBasicMulticast(GroupManager groupManager, Messenger messenger) {
		return new BasicMulticast(groupManager, messenger);
	}
	
	private static Multicast createReliableMulticast(Multicast basicMulticast, GroupManager groupManager) {
		return new AllToAllReliableMulticast(basicMulticast, groupManager);
	}
}
