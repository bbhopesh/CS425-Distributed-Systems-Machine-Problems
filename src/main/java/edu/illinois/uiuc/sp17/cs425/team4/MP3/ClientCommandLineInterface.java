package edu.illinois.uiuc.sp17.cs425.team4.MP3;

import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVRawDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KeyLockManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.TransactionManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.TransactionManagerImpl;
import edu.illinois.uiuc.sp17.cs425.team4.exceptions.TransactionAbortedException;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class ClientCommandLineInterface {
	
	private final PrintStream output = System.out;
	private final PrintStream otherFeedback = System.err;
	
	private static final String BEGIN = "BEGIN";
	private static final String BEGIN_RESPONSE = "OK";
	private static final String SET = "SET";
	private static final String GET = "GET";
	private static final String COMMIT = "COMMIT";
	private static final String COMMIT_RESPONSE = "COMMIT OK";
	private static final String ABORT = "ABORT";
	private static final String ABORT_RESPONSE = "ABORT";
	private static final String NOT_FOUND_RESPONSE = "NOT FOUND";
	private static final String FOUND_RESPONSE_FORMAT = "%s = %s";
	private static final String SET_RESPONSE = "OK";
	
	private final KeyLockManager<Pair<Process, String>> keyLockManager;
	private final KVRawDataManager<String, String> rawDataManager;
	private final int remoteReadWriteTimeout;
	private final Map<String, Process> keyServers;
	
	private TransactionManager<String, String> activeTransactionManager;
	
	
	public ClientCommandLineInterface(Set<Process> keyServers,
											KeyLockManager<Pair<Process, String>> keyLockManager,
											KVRawDataManager<String, String> rawDataManager,
											int remoteReadWriteTimeout) {
		this.keyServers = nameToProcessMap(keyServers);
		this.keyLockManager = keyLockManager;
		this.rawDataManager = rawDataManager;
		this.remoteReadWriteTimeout = remoteReadWriteTimeout;
		
		this.activeTransactionManager = null;
	}
	
	private Map<String, Process> nameToProcessMap(Set<Process> keyServers) {
		Map<String, Process> nameToProcessMap = new HashMap<>();
		for (Process p: keyServers) {
			nameToProcessMap.put(p.getDisplayName(), p);
		}
		return nameToProcessMap;
	}
	
	public void startInterface() throws InterruptedException {
		
		//Read and parse user input 
		Scanner scanner = new Scanner(System.in);
		while(true) {
			String userInput = scanner.nextLine();
			if(userInput.equals("EXIT")) {
				System.out.println("------ Exiting program ------");
				break;
			}
			readUserInput(userInput);
		}
		scanner.close();
	}

	private void readUserInput(String userInput) {
		String[] parameters = userInput.split(" ");
		if (parameters.length < 1) return;
		if (parameters[0].equalsIgnoreCase(BEGIN)) {
			handleBegin();
		} else if (this.activeTransactionManager == null) {
			otherFeedback.println("No active transaction. Start new with " + BEGIN);
		} else if (parameters[0].equalsIgnoreCase(SET)) {
			handleSet(parameters);
		} else if (parameters[0].equalsIgnoreCase(GET)) {
			handleGet(parameters);
		} else if (parameters[0].equalsIgnoreCase(COMMIT)) {
			handleCommit();
		} else if (parameters[0].equalsIgnoreCase(ABORT)) {
			handleAbort();
		} else {
			otherFeedback.println("Wrong command.");
		}
		
	}
	
	private void handleBegin() {
		if (this.activeTransactionManager == null) {
			this.activeTransactionManager = createTransactionManager();
			output.println(BEGIN_RESPONSE);
		} else {
			otherFeedback.println(String.format("Transaction %s already in progress.",
					this.activeTransactionManager.getTransaction().getDisplayName()));
		}
	}
	
	private void handleSet(String[] parameters) {
		if (parameters.length == 3) {
			String completeKey = parameters[1];
			String value = parameters[2];
			Pair<Process, String> parsedKey;
			try {
				parsedKey = parseKey(completeKey);
			} catch (UnknownHostException e1) {
				otherFeedback.println("Unknown server.");
				return;
			}
			if (parsedKey == null) {
				otherFeedback.println("Wrong key format.");
			} else {
				try {
					this.activeTransactionManager.set(parsedKey.getLeft(), parsedKey.getRight(), value);
					output.println(SET_RESPONSE);
				} catch(TransactionAbortedException e) {
					output.println(ABORT_RESPONSE);
					markClosed();
				}
			}
		} else {
			otherFeedback.println("Incorrect number of arguments.");
		}
	}
	
	private void handleGet(String[] parameters) {
		if (parameters.length == 2) {
			String completeKey = parameters[1];
			Pair<Process, String> parsedKey;
			try {
				parsedKey = parseKey(completeKey);
			} catch (UnknownHostException e1) {
				otherFeedback.println("Unknown server.");
				return;
			}
			if (parsedKey == null) {
				otherFeedback.println("Wrong key format.");
			} else {
				try {
					String value = this.activeTransactionManager.get(parsedKey.getLeft(), parsedKey.getRight());
					output.println(String.format(FOUND_RESPONSE_FORMAT, completeKey, value));
				} catch(TransactionAbortedException e) {
					if (e.getCause() instanceof NoSuchElementException) {
						output.println(NOT_FOUND_RESPONSE);
					} else {
						output.println(ABORT_RESPONSE);
					}
					markClosed();
				}
			}
		} else {
			otherFeedback.println("Incorrect number of arguments.");
		}
	}
	
	private void handleCommit() {
		if (this.activeTransactionManager == null) {
			otherFeedback.println("No active transaction.");
		} else {
			this.activeTransactionManager.commit();
			output.println(COMMIT_RESPONSE);
			markClosed();
		}
	}
	
	private void handleAbort() {
		if (this.activeTransactionManager == null) {
			otherFeedback.println("No active transaction.");
		} else {
			this.activeTransactionManager.abort();
			output.println(ABORT_RESPONSE);
			markClosed();
		}
	}
	
	private TransactionManager<String, String> createTransactionManager() {
		return new TransactionManagerImpl<>(this.keyLockManager,
											this.rawDataManager, 
											this.remoteReadWriteTimeout);
	}
	
	private Pair<Process, String> parseKey(String completeKey) throws UnknownHostException {
		String[] arr = completeKey.split("\\.");
		if (arr.length != 2) {
			return null;
		} else {
			Process server = this.keyServers.get(arr[0]);
			if (server == null) throw new UnknownHostException();
			return Pair.of(server, arr[1]);
		}
	}
	
	private void markClosed() {
		this.activeTransactionManager = null;
	}
}
