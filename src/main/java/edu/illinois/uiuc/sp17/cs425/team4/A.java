package edu.illinois.uiuc.sp17.cs425.team4;

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

public class A {
	
	private static final String MY_NAME = "A";
	private static Set<Process> groupMembers = new HashSet<Process>();
	private static Process mySelf;
	
	public static void main(String[] args) throws IOException {
		Model model = createModel();
		initializeGroupMembers(model);
		intializeMyself();
		model.setMyIdentity(mySelf);
		
		GroupManager groupManager = createGroupManager();
		Messenger messenger = createTCPMessenger();
		Multicast basicMulticast = createBasicMulticast(groupManager, messenger);
		Multicast reliableMulticast = createReliableMulticast(basicMulticast,groupManager);
		//ChatApplication app =  new SimpleChatApplication(basicMulticast, model);
		ChatApplication app =  new SimpleChatApplication(reliableMulticast, model);
		app.startChat();
		System.exit(0);
	}
	
	private static void initializeGroupMembers(Model model) throws UnknownHostException {
		Process m1 = model.createProcess(InetAddress.getByName("localhost"),
				10005, "A", new UUID(01, 02));
		
		Process m2 = model.createProcess(InetAddress.getByName("localhost"),
				10010, "B", new UUID(03, 04));
		// If you copy to create more processes, don't forget to change UUID
		// UUID should be unique for each.
		
		groupMembers.addAll(Arrays.asList(
				m1
				,m2
				));
	}
	
	private static void intializeMyself() {
		for (Process p: groupMembers) {
			if (p.getDisplayName().equals(MY_NAME)) {
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
