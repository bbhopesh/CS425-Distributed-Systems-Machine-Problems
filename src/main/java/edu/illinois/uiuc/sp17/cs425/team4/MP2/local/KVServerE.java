package edu.illinois.uiuc.sp17.cs425.team4.MP2.local;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Properties;
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
import edu.illinois.uiuc.sp17.cs425.team4.component.Codec;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.HashFunction;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.Multicast;
import edu.illinois.uiuc.sp17.cs425.team4.component.RingTopology;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.AllToAllReliableMulticast;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.BasicMulticast;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.CassandraLikeRing;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.IsisTotallyOrderedMC;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.KVCommandLineInterface;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.KVRingDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.LocalKVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.MessageListenerIdentifierImpl;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.PlainVanillaStringCodec;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.ProcessCodec;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SHA1HashFunction;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SWIMFailureDetectorV2;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SimpleChatApplication;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SimpleKVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.SimpleRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessengerBuilder;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;

@SuppressWarnings("unused")
public class KVServerE {
	/** All members of the group. */
	private static Set<Process> groupMembers = new HashSet<Process>();
	/** Myself. */
	private static Process mySelf;
	/** Number of peers in the group chat. */
	private static int peerCount;
	private static int numFailures;
	private static int mBytes;
	private static int kvRequestTimeout;
	private static int kvRetryCount;
	
	private static String myName = "E";
	/** Chat transcript file. */
	private static String outputFile;
	/** Config file. */
	private static String configFile = "chatapp.properties";
	/** Configuration. */
	private static Properties config;
	/** Model. */
	private static final Model MODEL = new ModelImpl();
	/** Template of hostnames. */
	private static final String HOSTNAME_TEMPLATE = "sp17-cs425-g04-%02d.cs.illinois.edu";
	private static final String SWIM2_PROTOCOL_PERIOD = "swimV2.protocol.period";
	private static final String SWIM2_PROTOCOL_MIN_PERIOD = "swimV2.protocol.min.period";
	private static final String SWIM2_PING_TARGETS = "swimV2.ping.targets";
	private static final String SWIM2_ACK_TIMEOUT = "swimV2.ack.timeout";
	private static final String ISIS_FIRST_ROUND_TIMEOUT = "isis.first.round.timeout";
	
	/** Short and long name of peer count option. */
	private static Pair<String,String> peerCountOption = Pair.of("c", "peerCount");
	/** Short and long name of output file option. */
	private static Pair<String,String> outputFileOption = Pair.of("o", "outputFile");
	private static Pair<String,String> configFileOption = Pair.of("f", "config");
	
	
	
	
	/**
	 * Driver code.
	 * @param args command line args.
	 * @throws IOException if cannot start chat app.
	 * @throws ParseException if cannot parse input.
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, ParseException, InterruptedException {
		// Initialization.
		// reads from command line and set various parameters.
		//readCommandLine(args);
		// initialize configuration
		initializeConfiguration();
		// intialize grop.
		initializeGroupMembers();
		// initialize myself
		initializeMyself();
		
		// Create chat application.
		// Create a group manager.
		ExecutorService threadPool = Executors.newFixedThreadPool(100);
		// Create a messenger to communicate using TCP
		Messenger messenger = createTCPMessenger(threadPool);
		messenger.initialize();
		/*GroupManager groupManager = createGroupManager(messenger, threadPool);
		waitForEveryoneToComeOnline(messenger, MODEL);
		Thread.sleep(2000);
		groupManager.initialize();*/
		
		// Create Data partitioner.
		KVDataPartitioner<String> ringDataPartitioner =  createKVRingDataPartitioner();
		// create data manager using data partitioner.
		KVDataManager<String, String> mainDataManager = createMainDataManager(messenger, threadPool, ringDataPartitioner);
		// Start user interface.
		KVCommandLineInterface kvCmd = createKVCmd(mainDataManager, ringDataPartitioner);
		kvCmd.startInterface();
		// App is done. exit system.
		System.exit(0);
	}
	
	private static KVCommandLineInterface createKVCmd(KVDataManager<String, String> mainDataManager,
			KVDataPartitioner<String> ringDataPartitioner) {
		return new KVCommandLineInterface(mainDataManager, ringDataPartitioner);
	}

	private static KVDataManager<String, String> createMainDataManager(Messenger messenger, 
			ExecutorService threadPool, KVDataPartitioner<String> ringDataPartitioner) {
		// Create local data manager.
		KVDataManager<String, String> localDataManager = createLocalDataManager();
		// Create raw data manager using local data manager.
		KVRawDataManager<String, String> rawDataManager = createRawDataManager(localDataManager, messenger, threadPool);
		// Create main data manager using a raw manager.
		return new SimpleKVDataManager<String, String>(rawDataManager, ringDataPartitioner, kvRequestTimeout, kvRetryCount);
	}
	
	private static KVDataPartitioner<String> createKVRingDataPartitioner() {
		// Create Ring topology.
		RingTopology<String> ringTopology = createCassandraLikeRingTopology();
		return new KVRingDataPartitioner<String>(ringTopology, numFailures);
	}

	private static RingTopology<String> createCassandraLikeRingTopology() {
		HashFunction hashFunction = new SHA1HashFunction();
		Codec<String> strCodec = new PlainVanillaStringCodec();
		Codec<Process> processCodec = new ProcessCodec();
		return new CassandraLikeRing<String>(groupMembers, hashFunction, mBytes, strCodec, processCodec);
	}
	private static KVDataManager<String, String> createLocalDataManager() {
		return new LocalKVDataManager<String, String>();
	}
	
	private static KVRawDataManager<String, String> createRawDataManager(KVDataManager<String, String> localDataManager,
			Messenger messenger, ExecutorService threadPool) {
		return new SimpleRawDataManager<String, String>(localDataManager, messenger, MODEL, mySelf, threadPool);
	}
	
	private static void initializeConfiguration() throws IOException {
		peerCount = 10;
		numFailures = 2;
		mBytes = 8;
		kvRequestTimeout = 1000;
		kvRetryCount = 1;
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
		options.addOption(configFileOption.getLeft(), configFileOption.getRight(), true, "Configuration file.");
		
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
		
		if(cmd.hasOption(configFileOption.getLeft())) {
			configFile = cmd.getOptionValue(configFileOption.getLeft());
		}
	}


	/**
	 * Initialize five members of the group.
	 * @param model Model
	 * @throws UnknownHostException
	 */
	private static void initializeGroupMembers() throws UnknownHostException {
		char startName = 'A';
		int port = 10005;
		for (int i = 1; i <= peerCount; i++) {
			String name = new StringBuilder().append(startName).toString();
			Process m = MODEL.createProcess(
					InetAddress.getByName("localhost"),
					port, name, new UUID(new Long(i), 02));
			groupMembers.add(m);
			// Increment name
			startName += 1;
			port += 5;
		}
	}
	
	/**
	 * Initialize myself.
	 * @throws UnknownHostException 
	 */
	private static void initializeMyself() throws UnknownHostException {
		/*String hostname = InetAddress.getLocalHost().getHostName();
		for (Process p: groupMembers) {
			if (p.getInetAddress().getHostName().equals(hostname)) {
				mySelf = p;
			}
		}*/
		
		for (Process p: groupMembers) {
			if (p.getDisplayName().equals(myName)) {
				mySelf = p;
			}
		}
	}
	
	/**
	 * Create a group manager.
	 * @return group manager.
	 * @throws FileNotFoundException 
	 */
	public static GroupManager createGroupManager(Messenger messenger, ExecutorService threadPool) throws FileNotFoundException {
		int ackTimeout = Integer.valueOf(config.getProperty(SWIM2_ACK_TIMEOUT));
		int protocolPeriod = Integer.valueOf(config.getProperty(SWIM2_PROTOCOL_PERIOD));
		double minProtocolPeriod = Double.valueOf(config.getProperty(SWIM2_PROTOCOL_MIN_PERIOD));
		int pingTargets = Integer.valueOf(config.getProperty(SWIM2_PING_TARGETS));
		return new SWIMFailureDetectorV2(mySelf, 
				groupMembers,
				messenger, 
				ackTimeout, 
				protocolPeriod, 
				minProtocolPeriod,
				pingTargets,
				threadPool);
	}
	
	/**
	 * Create TCP messenger.
	 * @return Messenger.
	 * @throws IOException if cannot create messenger.
	 */
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
	
	/**
	 * Create basic multicast.
	 * @param groupManager Group Manager.
	 * @param messenger Messenger
	 * @return Multicast.
	 */
	private static Multicast createBasicMulticast(GroupManager groupManager, Messenger messenger) {
		BasicMulticast bmc = new BasicMulticast(groupManager, messenger);
		//groupManager.registerGroupChangeListener(bmc);
		return bmc;
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
	
	/**
	 * Create a multicast enforcing total-order
	 * @param reliableMulticast
	 * @param messenger
	 * @param groupManager
	 * @return Totally Ordered Multicast
	 */
	private static Multicast createTOMulticast(Multicast reliableMulticast,Messenger messenger, GroupManager groupManager) {
		int firstRoundTimeout = Integer.valueOf(config.getProperty(ISIS_FIRST_ROUND_TIMEOUT));
		IsisTotallyOrderedMC totalMC = new IsisTotallyOrderedMC(reliableMulticast,messenger,groupManager, firstRoundTimeout);
		groupManager.registerGroupChangeListener(totalMC);
		return totalMC;
	}
	
	private static void waitForEveryoneToComeOnline(Messenger messenger, Model model) {
		Set<Process> onlineProcesses = new HashSet<Process>();
		while (onlineProcesses.size() < groupMembers.size() - 1) {
			for(Process groupMem: groupMembers) {
				if(!groupMem.equals(mySelf) && !onlineProcesses.contains(groupMem)) {
					Message response = pingProcess(messenger, model, groupMem);
					if (response != null) {
						onlineProcesses.add(groupMem);
					}
				}
			}
		}
	}
	
	private static Message createPingMessage(Model model) {
		Message message = model.createPingMessage(mySelf);
		message.setMessageListenerId(new MessageListenerIdentifierImpl("SwimV2FailureDetector"));
		return message;
	}
	
	private static Message pingProcess(Messenger messenger, Model model, Process process) {
		Message pingMessage = createPingMessage(model);
		Message response = null;
		try {
			response = messenger.send(Pair.of(process, pingMessage),5000);
		} catch (Exception e) {
			// ignore
		}
		return response;
	}
}
