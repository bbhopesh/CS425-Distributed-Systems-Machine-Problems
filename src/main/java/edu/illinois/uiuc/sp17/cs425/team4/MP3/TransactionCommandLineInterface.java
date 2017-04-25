package edu.illinois.uiuc.sp17.cs425.team4.MP3;

import java.util.Scanner;

import edu.illinois.uiuc.sp17.cs425.team4.component.KeyLockManager;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

public class TransactionCommandLineInterface {
	
	private final KeyLockManager<String> lockManager;
	private Transaction activeTransaction;
	
	public TransactionCommandLineInterface(KeyLockManager<String> lockManager) {
		this.lockManager = lockManager;
		this.activeTransaction = null;
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
		if (parameters[0].equalsIgnoreCase("BEGIN")) {
			if (this.activeTransaction == null) {
				this.activeTransaction = this.lockManager.beginNew();
			} else {
				System.out.println("Transaction already in progress. Close that first.");
			}
		} else if (this.activeTransaction == null) {
			System.out.println("First begin another transaction before you type any other command.");
		} else if (parameters[0].equalsIgnoreCase("LOCK_R")) {
			if (parameters.length == 2) {
				this.lockManager.acquireReadLock(this.activeTransaction, parameters[1]);
			} else {
				System.out.println("Incorrect number of arguments.");
			}
		} else if (parameters[0].equalsIgnoreCase("UNLOCK_R")) {
			if (parameters.length == 2) {
				this.lockManager.releaseReadLock(this.activeTransaction, parameters[1]);
			} else {
				System.out.println("Incorrect number of arguments.");
			}
		} else if (parameters[0].equalsIgnoreCase("LOCK_W")) {
			if (parameters.length == 2) {
				this.lockManager.acquireWriteLock(this.activeTransaction, parameters[1]);
			} else {
				System.out.println("Incorrect number of arguments.");
			}
		} else if (parameters[0].equalsIgnoreCase("UNLOCK_W")) {
			if (parameters.length == 2) {
				this.lockManager.releaseWriteLock(this.activeTransaction, parameters[1]);
			} else {
				System.out.println("Incorrect number of arguments.");
			}
		} else if (parameters[0].equalsIgnoreCase("CLOSE")) {
			if (this.activeTransaction == null) {
				System.out.println("No active transaction.");
			} else {
				this.activeTransaction = null;
				// TODO cleanup abort etc.
			}
		} else {
			System.out.println("Wrong command.");
		}
		
	}
	
	/*private void readUserInput(String userInput) {
		String[] parameters = userInput.split(" ");
		if (parameters.length < 1) return;
		if (parameters[0].equalsIgnoreCase("BEGIN")) {
			if (this.activeTransaction == null) {
				this.activeTransaction = this.lockManager.beginNew();
			} else {
				System.out.println("Transaction already in progress. Close that first.");
			}
		} else if (this.activeTransaction == null) {
			System.out.println("First begin another transaction before you type any other command.");
		} else {
			System.out.println("Wrong command.");
		}
		
	}*/
}
