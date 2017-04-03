package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

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

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class KVCommandLineInterface {
	
	private final KVDataManager<String, String> dataManager;
	private final KVDataPartitioner<String> dataPartitioner;

	public KVCommandLineInterface(KVDataManager<String, String> dataManager, 
			KVDataPartitioner<String> dataPartitioner) {
		this.dataManager = dataManager;
		this.dataPartitioner = dataPartitioner;
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
		boolean success = this.dataManager.write(key, value);
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
		String line = null;
        PrintStream stdout = System.out;
		try {
            FileReader fileReader = new FileReader(commandFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            //Redirect stdout to the output file
            PrintStream outputStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile)), true);
            System.setOut(outputStream);
            while((line = bufferedReader.readLine()) != null) {
            	readUserInput(line);
            }   
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
}
