package edu.illinois.uiuc.sp17.cs425.team4.MP2;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class KVCommandLineInterface {
	
	private final KVDataManager<String, String> dataManager;
	private final KVDataPartitioner<String> dataPartitioner;
	private final int batchSize;

	public KVCommandLineInterface(KVDataManager<String, String> dataManager, 
			KVDataPartitioner<String> dataPartitioner,int batchSize) {
		this.dataManager = dataManager;
		this.dataPartitioner = dataPartitioner;
		this.batchSize = batchSize;
	}
	
	
	public void startInterface() {
		
		//Display help messages, feel free to modify later 
		displayHelpMsg();
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
	
	private void displayHelpMsg() {
		System.out.println("------ There are five operations allowed ------");
		System.out.println("------ SET, GET, OWNER, LIST_LOCAL, BATCH ------");
		System.out.println("------ Each parameter is separated by space ------");
		System.out.println("------ SET KEY VALUE  ------");
		System.out.println("------ GET KEY ------");
		System.out.println("------ OWNERS KEY ------");
		System.out.println("------ LIST_LOCAL ------");
		System.out.println("------ BATCH COMMAND_FILE OUTPUT_FILE ------");
		System.out.println("------------------------------------------------");
		System.out.println("------ Use keyword EXIT to exit the program ------");		
	}
	
	private void readUserInput(String userInput) {

			String[] parameters = userInput.split(" ");
			if (parameters.length < 1) return;
			if(parameters[0].equals("SET")) {
				if(parameters.length >= 3) {
					//The value of Set could contain spaces
					String key = parameters[1];
					int valueIndex = userInput.indexOf(" ", userInput.indexOf(" ") + 1) + 1;
					String value = userInput.substring(valueIndex);
					handleSetOperation(key,value);
				}else {
					System.err.println("Invalid input: " + userInput);
					System.err.println("Invalid argument numbers, use --help to check for valid input");
				} 
			}else if(parameters[0].equals("GET")) {
				if(parameters.length == 2) {
					handleGetOperation(parameters[1]);
				}else {
					System.err.println("Invalid input: " + userInput);
					System.err.println("Invalid argument numbers, use --help to check for valid input");
				}
			}else if(parameters[0].equals("OWNERS")) {
				if(parameters.length == 2) {
					handleOwnersOperation(parameters[1]);
				}else {
					System.err.println("Invalid input: " + userInput);
					System.err.println("Invalid argument numbers, use --help to check for valid input");
				}
			}else if(parameters[0].equals("LIST_LOCAL")) {
				if(parameters.length == 1) {
					handleListLocalOperation();
				}else {
					System.err.println("Invalid input: " + userInput);
					System.err.println("Invalid argument numbers, use --help to check for valid input");
				}
			}else if(parameters[0].equals("BATCH")) {
				if(parameters.length == 3) {
					handleBatchOperation(parameters[1],parameters[2]);
				}else {
					System.err.println("Invalid input: " + userInput);
					System.err.println("Invalid argument numbers, use --help to check for valid input");
				}	
			}else if(parameters[0].equals("--help")) {
				if(parameters.length == 1) {
					displayHelpMsg();
				}else {
					System.err.println("Invalid input: " + userInput);
					System.err.println("Invalid argument numbers, use --help to check for valid input");
				}
			}else {
				System.err.println("Invalid key words, use --help to check for valid input");
			}
	}
	
	private void handleSetOperation(String key, String value) {
		Long t = System.currentTimeMillis();
		boolean success = this.dataManager.write(key, value, t);
		if(success) {
			System.out.println("SET OK");
		}else {
			System.err.println("Failed to write key: '" + key + "' and value: '" + value + "'");
		}
	}
	
	private void handleGetOperation(String key) {
		Pair<Long, String> readResult = this.dataManager.read(key);
		if(readResult != null) {
			System.out.println("Found: "+ readResult.getRight());
		}else {
			System.out.println("Not found");
		}
	}
	
	private void handleOwnersOperation(String key) {
		Pair<Long, String> result = this.dataManager.read(key);
		if(result != null) {
			Process primaryPartition = this.dataPartitioner.getPrimaryPartition(key);
			Set<Process> replicas = this.dataPartitioner.getReplicas(primaryPartition);
			replicas.add(primaryPartition);
			for(Process p : replicas) {
				System.out.print(extractVMId(p));
				System.out.print(" ");
			}
			System.out.print("\n");			
		} else {
			System.out.println("Not found"); // TODO not in MP interface but still useful.
		}
	}
	
	private String extractVMId(Process process) {
		String hostname = process.getInetAddress().getHostName();
		if (hostname.length() < 17) {
			// This case is for when running on localhost.
			return process.getDisplayName();
		} else {
			return hostname.substring(15, 17);
		}
	}
	
	private void handleListLocalOperation() {
		for(String key : this.dataManager.getLocalSnapshot().keySet()) {
			System.out.println(key);
		}
		System.out.println("END LIST");
	}
	
	private void handleBatchOperation(String commandFile,String outputFile) {
		
		//Read command file 
        PrintStream stdout = System.out;
		try {
            FileReader fileReader = new FileReader(commandFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            //Redirect stdout to the output file
            PrintStream outputStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile)), true);
            System.setOut(outputStream);
            batchOperationHelper(bufferedReader,commandFile);
        	bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            System.err.println("Unable to open file '" + commandFile + "'");                
        }
        catch(IOException ex) {
            System.err.println( "Error reading file '" + commandFile + "'");
            ex.printStackTrace();
        } finally {
        	//Restore stdout 
            System.setOut(stdout);
            System.err.println("Done with BATCH operation."); // TODO not part of interface but still useful.
        }	
	}
	
	private void batchOperationHelper(BufferedReader bufferedReader,String commandFile) {
		
		try {
			String line = null;
			boolean eof = false;
			while(!eof) {		
				Map<String, NavigableMap<Long, String>> setData = new HashMap<String,NavigableMap<Long,String>>();
				Map<String, NavigableSet<Long>> readData = new HashMap<String,NavigableSet<Long>>();
				List<Long> listLocalTimes = new ArrayList<Long>();
				Map<String, NavigableSet<Long>> ownersData = new HashMap<String,NavigableSet<Long>>();
				List<Pair<String,Pair<Long,String>>> commands = new ArrayList<Pair<String,Pair<Long,String>>>();
				for(int i = 0; i < this.batchSize; i++) {
					eof = (line = bufferedReader.readLine()) == null;
					if(eof) {break;}
					readBatchInput(line,setData,readData,listLocalTimes,ownersData,commands);	
				}
				printBatchOutput(setData,readData,listLocalTimes,ownersData,commands);
			}
		} catch (IOException e) {
			System.err.println( "Error reading file '" + commandFile + "'");
			e.printStackTrace();
		}   
		
	}
	
	private void printBatchOutput(Map<String, NavigableMap<Long, String>> setData,
			Map<String, NavigableSet<Long>> readData,List<Long> listLocalTimes,
			Map<String, NavigableSet<Long>> ownersData,List<Pair<String,Pair<Long,String>>> commands) {
		//Do the batch operation in the order of set get owners list_locals 
		//Note list_locals does not have a batch version
		boolean successWrite = this.dataManager.writeBatch(setData);
		if(!successWrite) {
			System.err.println("Fail to perform batch SET operation, some of the keys might not be set successfully");
		}
		Map<String, NavigableMap<Long, String>> readResult = this.dataManager.readBatch(readData);
		Map<String, NavigableMap<Long, String>> ownerResult = this.dataManager.readBatch(readData);
		for(int i = 0; i< commands.size(); i++) {
			Pair<String,Pair<Long,String>> line = commands.get(i);
			String oper = line.getLeft();
			if(oper.equals("SET")) {
				System.out.println("SET OK");
			}else if(oper.equals("GET")) {
				Pair<Long,String> timeAndKey = line.getRight();
				Long t = timeAndKey.getLeft();
				String key = timeAndKey.getRight();
				if(readResult.containsKey(key)) {
					if(readResult.get(key).containsKey(t)) {
						System.out.println("Found: "+ readResult.get(key).get(t));
					}else {
						System.out.println("Not found");
					}
				}else {
					System.out.println("Not found");
				}
			}else if(oper.equals("OWNERS")) {
				Pair<Long,String> timeAndKey = line.getRight();
				Long t = timeAndKey.getLeft();
				String key = timeAndKey.getRight();
				if(ownerResult.containsKey(key)) {
					if(ownerResult.get(key).containsKey(t)) {
						Process primaryPartition = this.dataPartitioner.getPrimaryPartition(key);
						Set<Process> replicas = this.dataPartitioner.getReplicas(primaryPartition);
						replicas.add(primaryPartition);
						for(Process p : replicas) {
							System.out.print(extractVMId(p));
							System.out.print(" ");
						}
						System.out.print("\n");	
					}else {
						System.out.println("Not found");
					}
				}else {
					System.out.println("Not found");
				}
			}else if(oper.equals("LIST_LOCAL")) {
				Pair<Long,String> timeAndKey = line.getRight();
				Long t = timeAndKey.getLeft();
				Map<String, NavigableMap<Long, String>> localKeys = this.dataManager.getLocalSnapshot(t);
				for(String key : localKeys.keySet()) {
					System.out.println(key);
				}
				System.out.println("END LIST");	
			}
		}
		
	}
	
	private void readBatchInput(String line, Map<String, NavigableMap<Long, String>> setData,
			Map<String, NavigableSet<Long>> readData,List<Long> listLocalTimes,
			Map<String, NavigableSet<Long>> ownersData,List<Pair<String,Pair<Long,String>>> commands) {
		String[] parameters = line.split(" ");
		if (parameters.length < 1) return;
		if(parameters[0].equals("SET")) {
			if(parameters.length >= 3) {
				//The value of Set could contain spaces
				String key = parameters[1];
				int valueIndex = line.indexOf(" ", line.indexOf(" ") + 1) + 1;
				String value = line.substring(valueIndex);
				NavigableMap<Long,String> valueMap = null;
				Long t = System.currentTimeMillis();
				if(setData.containsKey(key)) {
					valueMap = setData.get(key);
					valueMap.put(t, value);
				}else {
					valueMap = new TreeMap<Long,String>();
					valueMap.put(t, value);
					setData.put(key, valueMap);
				}
				commands.add(Pair.of("SET", null));
			}else {
				System.err.println("Invalid input: " + line);
				System.err.println("Invalid argument numbers, use --help to check for valid input");
			} 
		}else if(parameters[0].equals("GET")) {
			if(parameters.length == 2) {
				NavigableSet<Long> valueSet = null;
				Long t = System.currentTimeMillis();
				if(readData.containsKey(parameters[1])) {
					valueSet = readData.get(parameters[1]);
					valueSet.add(t);
				}else {
					valueSet = new TreeSet<Long>();
					valueSet.add(t);
					readData.put(parameters[1], valueSet);
				}
				commands.add(Pair.of("GET", Pair.of(t,parameters[1])));
			}else {
				System.err.println("Invalid input: " + line);
				System.err.println("Invalid argument numbers, use --help to check for valid input");
			}
		}else if(parameters[0].equals("OWNERS")) {
			if(parameters.length == 2) {
				NavigableSet<Long> valueSet = null;
				Long t = System.currentTimeMillis();
				if(ownersData.containsKey(parameters[1])) {
					valueSet = ownersData.get(parameters[1]);
					valueSet.add(t);
				}else {
					valueSet = new TreeSet<Long>();
					valueSet.add(t);
					ownersData.put(parameters[1], valueSet);
				}
				commands.add(Pair.of("OWNERS",Pair.of(t,parameters[1])));
			}else {
				System.err.println("Invalid input: " + line);
				System.err.println("Invalid argument numbers, use --help to check for valid input");
			}
		}else if(parameters[0].equals("LIST_LOCAL")) {
			if(parameters.length == 1) {
				Long t = System.currentTimeMillis();
				listLocalTimes.add(t);
				commands.add(Pair.of("LIST_LOCAL",Pair.of(t, null)));
			}else {
				System.err.println("Invalid input: " + line);
				System.err.println("Invalid argument numbers, use --help to check for valid input");
			}
		}
	}
}
