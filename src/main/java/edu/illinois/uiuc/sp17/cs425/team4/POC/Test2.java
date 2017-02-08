package edu.illinois.uiuc.sp17.cs425.team4.POC;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.tuple.Pair;

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

@SuppressWarnings("unused")
public class Test2 {
	
	public static void main(String[] args) throws IOException {
		Model model = createModel();
		Pair<InetAddress, Integer> addr = createAddressAndPort();
		Process mySelf = createProcess(model, addr);
		model.setMyIdentity(mySelf);
		GroupManager groupManager = createGroupManager(mySelf);
		Messenger messenger = createTCPMessenger(mySelf, addr.getRight());
		Multicast basicMulticast = createBasicMulticast(groupManager, messenger);
		Multicast reliableMulticast = createReliableMulticast(basicMulticast,groupManager);
		//ChatApplication app =  new SimpleChatApplication(basicMulticast, model);
		ChatApplication app =  new SimpleChatApplication(reliableMulticast, model);
		app.startChat();
		System.exit(0);
	}
	
	private static Model createModel() {
		return new ModelImpl();	
	}
	
	private static Pair<InetAddress, Integer> createAddressAndPort() throws UnknownHostException {
		return Pair.of(InetAddress.getLocalHost(), 10010);
	}
	private static Process createProcess(Model model, Pair<InetAddress, Integer> addr) {
		return model.createProcess(addr.getLeft(), addr.getRight(), "B");
	}
	
	private static Process createPeer(Model model, int port) throws UnknownHostException {
		return model.createProcess(InetAddress.getLocalHost(), port, "A");
	}
	
	public static GroupManager createGroupManager(Process mySelf) {
		return new StaticGroupManagerImpl(mySelf, 
				new HashSet<Process>(Arrays.asList(mySelf)));
	}
	
	private static Messenger createTCPMessenger(Process mySelf, int port) throws IOException {
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		TCPMessengerBuilder builder = new TCPMessengerBuilder()
									.setThreadPool(threadPool)
									.setPort(port)
									.setMyIdentity(mySelf)
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
