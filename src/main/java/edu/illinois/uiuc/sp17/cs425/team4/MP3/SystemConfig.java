package edu.illinois.uiuc.sp17.cs425.team4.MP3;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;

class SystemConfig {
	/** Model. */
	public static final Model MODEL = new ModelImpl();
	
	// Hard codes servers on machines 1-5, clients on machines 6-8, lockservice on machine 9.
	// That's how processes should be started.
	public static final Process LOCK_SERVICE = initializeLockService();
	public static final Set<Process> SERVERS = initializeServers();
	public static final Set<Process> CLIENTS = initializeClients();
	public static final Process MY_IDENTITY = myIdentity();
	public static final int READ_WRITE_REQUEST_TIMEOUT = 500; 
	
	
	/** Template of hostnames. */
	private static final String HOSTNAME_TEMPLATE = "sp17-cs425-g04-%02d.cs.illinois.edu";
	
	
	private static Set<Process> initializeServers() {
		Set<Process> servers = new HashSet<Process>(5);
		char startName = 'A';
		for (int i = 1; i <= 5; i++) {
			String name = new StringBuilder().append(startName).toString();
			Process m;
			try {
				m = MODEL.createProcess(
						InetAddress.getByName(String.format(HOSTNAME_TEMPLATE, i)),
						10005, name, new UUID(new Long(i), 02));
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
			servers.add(m);
			// Increment name
			startName += 1;
		}
		return servers;
	}
	
	private static Set<Process> initializeClients() {
		Set<Process> clients = new HashSet<Process>(5);
		String nameFormat = "C%s";
		for (int i = 6; i <= 8; i++) {
			String name = String.format(nameFormat, i-5);
			Process m;
			try {
				m = MODEL.createProcess(
						InetAddress.getByName(String.format(HOSTNAME_TEMPLATE, i)),
						10005, name, new UUID(new Long(i), 02));
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
			clients.add(m);
		}
		return clients;		
	}
	
	private static Process myIdentity() {
		Process myIdentity = null;
		
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		for (Process p: SERVERS) {
			if (p.getInetAddress().getHostName().equals(hostname)) {
				myIdentity = p;
			}
		}
		
		for (Process p: CLIENTS) {
			if (p.getInetAddress().getHostName().equals(hostname)) {
				myIdentity = p;
			}
		}
		
		if (LOCK_SERVICE.getInetAddress().getHostName().equals(hostname)) {
			myIdentity = LOCK_SERVICE;
		}
		
		return myIdentity;
	}
	
	private static Process initializeLockService() {
		Process m;
		try {
			String name = "LockService";
			m = MODEL.createProcess(
					InetAddress.getByName(String.format(HOSTNAME_TEMPLATE, 9)),
					10005, name, new UUID(new Long(9), 02));
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		return m;
	}
}
