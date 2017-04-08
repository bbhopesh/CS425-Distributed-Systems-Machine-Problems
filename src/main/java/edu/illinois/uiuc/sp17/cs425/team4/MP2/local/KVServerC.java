package edu.illinois.uiuc.sp17.cs425.team4.MP2.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.cli.ParseException;
import edu.illinois.uiuc.sp17.cs425.team4.MP1.CP2FailureDetector;
import edu.illinois.uiuc.sp17.cs425.team4.MP2.KVSystemInitializer;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;


public class KVServerC {
	private static String myName = "C";
	
	/** All members of the group. */
	private static Set<Process> gatewayProcesses = new HashSet<Process>();
	/** Myself. */
	private static Process myIdentity;
	
	// kv parameters.
	private static int numFailures;
	private static int mBytes;
	private static int kvRequestTimeout;
	private static int kvtryCount;
	
	// Swim parameters.
	private static int swimAckTimeout;
	private static int swimProtocolPeriod;
	private static double swimMinProtocolPeriod;
	private static int swimNumPingTargets;
	
	/** Config file. */
	private static String configFile = "kvstore.properties";
	/** Configuration. */
	private static Properties config;
	/** Model. */
	private static final Model MODEL = new ModelImpl();
	private static final String SWIM2_PROTOCOL_PERIOD = "swimV2.protocol.period";
	private static final String SWIM2_PROTOCOL_MIN_PERIOD = "swimV2.protocol.min.period";
	private static final String SWIM2_PING_TARGETS = "swimV2.ping.targets";
	private static final String SWIM2_ACK_TIMEOUT = "swimV2.ack.timeout";
	private static final String KV_MAX_FAILURES = "kv.max.failures";
	private static final String KV_RING_mBYTES = "kv.ring.mBytes";
	private static final String KV_REQUEST_TIMEOUT = "kv.request.timeout";
	private static final String KV_TRY_COUNT = "kv.try.count";
	
	
	
	
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
		// initialize configuration
		initializeConfiguration();
		// initialize gateways.
		initializeGateways();
		// initialize myself
		initializeMyself();
		
		KVSystemInitializer systemInit = createKVSystemInitializer();
		systemInit.initialize();
		// App is done. exit system.
		System.exit(0);
	}
	
	private static KVSystemInitializer createKVSystemInitializer() throws IOException {
		return new KVSystemInitializer(gatewayProcesses, myIdentity, MODEL, mBytes, numFailures, kvRequestTimeout, kvtryCount, swimAckTimeout, swimProtocolPeriod, swimMinProtocolPeriod, swimNumPingTargets);
	}

	private static void initializeConfiguration() throws IOException {
		File f = new File(configFile);
		InputStream is = null;
		if(f.exists() && !f.isDirectory()) { 
		    // do something
			is = new FileInputStream(f);
		} else {
			is = CP2FailureDetector.class.getClassLoader().getResourceAsStream(configFile);
		}
		config = new Properties();
		config.load(is);
		
		// kv parameters
		numFailures = Integer.valueOf(config.getProperty(KV_MAX_FAILURES));
		mBytes = Integer.valueOf(config.getProperty(KV_RING_mBYTES));
		kvRequestTimeout = Integer.valueOf(config.getProperty(KV_REQUEST_TIMEOUT));
		kvtryCount = Integer.valueOf(config.getProperty(KV_TRY_COUNT));
		
		// swim paramters
		swimAckTimeout = Integer.valueOf(config.getProperty(SWIM2_ACK_TIMEOUT));
		swimProtocolPeriod = Integer.valueOf(config.getProperty(SWIM2_PROTOCOL_PERIOD));
		swimMinProtocolPeriod = Double.valueOf(config.getProperty(SWIM2_PROTOCOL_MIN_PERIOD));
		swimNumPingTargets = Integer.valueOf(config.getProperty(SWIM2_PING_TARGETS));
	}



	private static void initializeGateways() throws UnknownHostException {
		char startName = 'A';
		int port = 10005;
		for (int i = 1; i <= 10; i++) {
			String name = new StringBuilder().append(startName).toString();
			Process m = MODEL.createProcess(
					InetAddress.getByName("localhost"),
					port, name, new UUID(new Long(i), 02));
			gatewayProcesses.add(m);
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
		for (Process p: gatewayProcesses) {
			if (p.getDisplayName().equals(myName)) {
				myIdentity = p;
			}
		}
	}
	
	
	
}
