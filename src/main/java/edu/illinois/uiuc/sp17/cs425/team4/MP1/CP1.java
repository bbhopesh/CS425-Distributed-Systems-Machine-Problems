package edu.illinois.uiuc.sp17.cs425.team4.MP1;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
	/** Number of peers in the group chat. */
	private static int peerCount;
	/** Chat transcript file. */
	private static String outputFile;
	/** Model. */
	private static final Model MODEL = new ModelImpl();
	/** Template of hostnames. */
	private static final String HOSTNAME_TEMPLATE = "sp17-cs425-g04-%02d.cs.illinois.edu";
	
	/** Short and long name of peer count option. */
	private static Pair<String,String> peerCountOption = Pair.of("c", "peerCount");
	/** Short and long name of output file option. */
	private static Pair<String,String> outputFileOption = Pair.of("o", "outputFile");
	/** Short and long name of myName option. */
	private static Pair<String,String> myNameOption = Pair.of("m", "myName");
	
	/**
	 * Driver code.
	 * @param args command line args.
	 * @throws IOException if cannot start chat app.
	 * @throws ParseException if cannot parse input.
	 */
	public static void main(String[] args) throws IOException, ParseException {
		// Initialization.
		// reads from command line and set various parameters.
		readCommandLine(args);
		// intialize grop.
		initializeGroupMembers();
		// initialize myself
		initializeMyself();
		
		// Create chat application.
		// Create a group manager.
		GroupManager groupManager = createGroupManager();
		// Create a messenger to communicate using TCP
		Messenger messenger = createTCPMessenger();
		// Create B-multicast.
		Multicast basicMulticast = createBasicMulticast(groupManager, messenger);
		// Create Reliable multicast.
		Multicast reliableMulticast = createReliableMulticast(basicMulticast,groupManager);
		// Create and start app.
		ChatApplication app =  createChatApplication(reliableMulticast);
		app.startChat();
		// App is done. exit system.
		System.exit(0);
	}
	
	/**
	 * Create chat application.
	 * @param multicast multicast to be used for chat.
	 * @return chat application.
	 * @throws FileNotFoundException if cannot find the file to store chat transcript.s
	 */
	private static ChatApplication createChatApplication(Multicast multicast) throws FileNotFoundException {
		if (outputFile == null) {
			return new SimpleChatApplication(multicast, mySelf);
		} else {
			FileOutputStream fos = new FileOutputStream(outputFile, false);
			PrintStream printStream = new PrintStream(fos);
			return new SimpleChatApplication(multicast, mySelf, printStream);
		}
	}

	
	/**
	 * Read command line args and extract values.
	 * @param args cmd line args.
	 * @throws ParseException if cannot parse cmd line args.
	 */
	private static void readCommandLine(String[] args) throws ParseException {
		// Initialize options.
		Options options = new Options();
		options.addOption(peerCountOption.getLeft(), peerCountOption.getRight(), true, "Number of members in the group");
		options.addOption(outputFileOption.getLeft(), outputFileOption.getRight(), true, "File to which output should be written.");
		options.addOption(myNameOption.getLeft(), myNameOption.getRight(), true, "Name of this current process.");
		
		// parse
		CommandLineParser parse = new DefaultParser();
		
		// Read
		CommandLine cmd  = parse.parse(options, args);
		if (cmd.hasOption(peerCountOption.getLeft())) {
			peerCount = Integer.valueOf(cmd.getOptionValue(peerCountOption.getLeft()));
		}
		
		if (cmd.hasOption(outputFileOption.getLeft())) {
			outputFile = cmd.getOptionValue(outputFileOption.getLeft());
		}
		
		if (cmd.hasOption(myNameOption.getLeft())) {
			myName = cmd.getOptionValue(myNameOption.getLeft());
		}
		
	}


	/**
	 * Initialize five members of the group.
	 * @param model Model
	 * @throws UnknownHostException
	 */
	private static void initializeGroupMembers() throws UnknownHostException {
		char startName = 'A';
		for (int i = 1; i <= peerCount; i++) {
			String name = new StringBuilder().append(startName).toString();
			Process m = MODEL.createProcess(
					InetAddress.getByName(String.format(HOSTNAME_TEMPLATE, i)),
					10005, name, new UUID(new Long(i), 02));
			groupMembers.add(m);
			// Increment name
			startName += 1;
		}
	}
	
	/**
	 * Initializse myself.
	 */
	private static void initializeMyself() {
		for (Process p: groupMembers) {
			if (p.getDisplayName().equals(myName)) {
				mySelf = p;
			}
		}
	}
	
	/**
	 * Create a group manager.
	 * @return group manager.
	 */
	public static GroupManager createGroupManager() {
		return new StaticGroupManagerImpl(mySelf, groupMembers);
	}
	
	/**
	 * Create TCP messenger.
	 * @return Messenger.
	 * @throws IOException if cannot create messenger.
	 */
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
	
	/**
	 * Create basic multicast.
	 * @param groupManager Group Manager.
	 * @param messenger Messenger
	 * @return Multicast.
	 */
	private static Multicast createBasicMulticast(GroupManager groupManager, Messenger messenger) {
		return new BasicMulticast(groupManager, messenger);
	}
	
	/**
	 * Create reliable multicast.
	 * @param basicMulticast basic multicast.
	 * @param groupManager Group Manager.
	 * @return Reliable multicast.
	 */
	private static Multicast createReliableMulticast(Multicast basicMulticast, GroupManager groupManager) {
		return new AllToAllReliableMulticast(basicMulticast, groupManager);
	}
}
