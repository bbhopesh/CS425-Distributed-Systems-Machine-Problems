package edu.illinois.uiuc.sp17.cs425.team4.MP1;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.impl.HashBasedRingKVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.LocalKVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class MP2 {

	public static void main(String[] args) throws Exception {
		
		//Display help messages if prompted, feel free to modify later 
		if(args.length == 1) {
			if(args[0].equals("help")) {
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
		}
		
		//Initialize LocalKVDataManager
		LocalKVDataManager<String, String> localDataManager = new LocalKVDataManager<String, String>();
		HashBasedRingKVDataPartitioner<String> dataPartitioner = new HashBasedRingKVDataPartitioner<String>(null, 0, null, null, null, null);

		//Read and parse user input 
		Scanner scanner = new Scanner(System.in);
		while(true) {
			String userInput = scanner.nextLine();
			if(userInput.equals("EXIT")) {
				System.out.println("------ Exiting program ------");
				break;
			}
			readUserInput(localDataManager,dataPartitioner,userInput);
		}
		scanner.close();
		
		
	}
	
	public static void readUserInput(LocalKVDataManager<String, String> localDataManager,
			HashBasedRingKVDataPartitioner<String> dataPartitioner,String userInput ) throws Exception {

			String[] parameters = userInput.split(" ");
			if(parameters[0] == "SET") {
				if(parameters.length >= 3) {
					//The value of Set could contain spaces
					String key = parameters[1];
					int valueIndex = userInput.indexOf(" ", userInput.indexOf(" ") + 1) + 1;
					String value = userInput.substring(valueIndex);
					handleSetOperation(localDataManager,key,value);
				}else {
					System.err.println("Invalid input: " + userInput);
					System.err.println("Invalid argument numbers, use --help to check for valid input");
				} 
			}else if(parameters[0] == "GET") {
				if(parameters.length == 2) {
					handleGetOperation(localDataManager,parameters[1]);
				}else {
					System.err.println("Invalid input: " + userInput);
					System.err.println("Invalid argument numbers, use --help to check for valid input");
				}
			}else if(parameters[0] == "OWNERS") {
				if(parameters.length == 2) {
					handleOwnersOperation(dataPartitioner,parameters[1]);
				}else {
					System.err.println("Invalid input: " + userInput);
					System.err.println("Invalid argument numbers, use --help to check for valid input");
				}
			}else if(parameters[0] == "LIST_LOCAL") {
				if(parameters.length == 1) {
					handleListLocalOperation(localDataManager);
				}else {
					System.err.println("Invalid input: " + userInput);
					System.err.println("Invalid argument numbers, use --help to check for valid input");
				}
			}else if(parameters[0] == "BATCH") {
				if(parameters.length == 3) {
					handleBatchOperation(localDataManager,dataPartitioner,parameters[1],parameters[2]);
				}else {
					System.err.println("Invalid input: " + userInput);
					System.err.println("Invalid argument numbers, use --help to check for valid input");
				}	
			}else {
				System.err.println("Invalid key words, use --help to check for valid input");
			}
	}
	
	public static void handleSetOperation(LocalKVDataManager<String, String> localDataManager,String key, String value) {
		boolean success = localDataManager.write(key, value);
		if(success) {
			System.out.println("SET OK");
		}else {
			System.err.println("Failed to write key: '" + key + "' and value: '" + value + "'");
		}
	}
	
	public static void handleGetOperation(LocalKVDataManager<String, String> localDataManager,String key) {
		Pair<Long, String> readResult = localDataManager.read(key);
		if(readResult != null) {
			System.out.println("Found: "+ readResult.getRight());
		}else {
			System.out.println("Not found");
		}
	}
	
	public static void handleOwnersOperation(HashBasedRingKVDataPartitioner<String> dataPartitioner, String key) {
		Process primaryPartition = (Process) dataPartitioner.getPrimaryPartition(key);
		Set<Process> replicas = dataPartitioner.getReplicas(primaryPartition);
		for(Process p : replicas) {
			System.out.print(p.getUUID());
			System.out.print(" ");
		}
		System.out.print("\n");
	}
	
	public static void handleListLocalOperation(LocalKVDataManager<String, String> localDataManager) {
		Set<Pair<Long, String>> localData = localDataManager.list_local();
		for(Pair<Long, String> p : localData) {
			System.out.println(p.getRight());
		}
		System.out.println("END LIST");
	}
	
	public static void handleBatchOperation(LocalKVDataManager<String, String> localDataManager,
			HashBasedRingKVDataPartitioner<String> dataPartitioner,
			String commandFile,String outputFile) throws Exception {
		
		//Read command file 
		String line = null;
		try {
            FileReader fileReader = new FileReader(commandFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            //Redirect stdout to the output file
            PrintStream outputStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile)), true);
            System.setOut(outputStream);
            while((line = bufferedReader.readLine()) != null) {
            	readUserInput(localDataManager,dataPartitioner,line);
            }   
            bufferedReader.close();         
            //Restore stdout
            System.setOut(System.out);
        }
        catch(FileNotFoundException ex) {
            System.err.println("Unable to open file '" + commandFile + "'");                
        }
        catch(IOException ex) {
            System.err.println( "Error reading file '" + commandFile + "'");
            ex.printStackTrace();
        }
		
	}
	
}
