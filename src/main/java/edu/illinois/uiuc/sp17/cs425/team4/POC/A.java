package edu.illinois.uiuc.sp17.cs425.team4.POC;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.ChatApplication;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.Multicast;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.AllToAllReliableMulticast;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.BasicMulticast;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.MessageListenerIdentifierImpl;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SWIMFailureDetector;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SimpleChatApplication;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessengerBuilder;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;

public class A {
	
	private static final String MY_NAME = "A";
	private static Set<Process> groupMembers = new HashSet<Process>();
	private static Process mySelf;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		Model model = createModel();
		initializeGroupMembers(model);
		intializeMyself();
		
		ExecutorService threadPool = Executors.newFixedThreadPool(100);
		Messenger messenger = createTCPMessenger(threadPool);
		messenger.initialize();
		GroupManager groupManager = createGroupManager(messenger, threadPool);
		waitForEveryoneToComeOnline(messenger, model);
		Thread.sleep(2000);
		groupManager.initialize();
		Multicast basicMulticast = createBasicMulticast(groupManager, messenger);
		Multicast reliableMulticast = createReliableMulticast(basicMulticast,groupManager);
		//ChatApplication app =  new SimpleChatApplication(basicMulticast, model);
		ChatApplication app =  new SimpleChatApplication(reliableMulticast, mySelf);
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
	
	public static GroupManager createGroupManager(Messenger messenger, ExecutorService threadPool) {
		//return new StaticGroupManagerImpl(mySelf, groupMembers);
		return new SWIMFailureDetector(mySelf, groupMembers, messenger, 
				0, 3000, 1, threadPool);
	}
	
	private static Messenger createTCPMessenger(ExecutorService threadPool) throws IOException {
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
		//return new BasicMulticast(groupManager, messenger);
		BasicMulticast bmc = new BasicMulticast(groupManager, messenger);
		groupManager.registerGroupChangeListener(bmc);
		return bmc;
	}
	
	private static Multicast createReliableMulticast(Multicast basicMulticast, GroupManager groupManager) {
		return new AllToAllReliableMulticast(basicMulticast, groupManager);
	}
	
	private static void waitForEveryoneToComeOnline(Messenger messenger, Model model) {
		int numberOfOnline = 0;
		while (numberOfOnline != groupMembers.size()) {
			for(Process groupMem: groupMembers) {
				if(!groupMem.equals(mySelf)) {
					Message response = pingProcess(messenger, model, groupMem);
					if (response != null) {
						numberOfOnline++;
					}
				}
			}
		}
	}
	
	private static Message createPingMessage(Model model) {
		Message message = model.createPingMessage(mySelf);
		message.setMessageListenerId(new MessageListenerIdentifierImpl("SWIMFailureDetector"));
		return message;
	}
	
	private static Message pingProcess(Messenger messenger, Model model, Process process) {
		Message pingMessage = createPingMessage(model);
		Message response = null;
		try {
			response = messenger.send(Pair.of(process, pingMessage),2000);
		} catch (Exception e) {
			// ignore
		}
		return response;
	}
}
