package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.UUID;

/**
 * An object representing a transaction.
 * 
 * @author bbassi2.
 *
 */
public interface Transaction {
	/**
	 * @return UUID of transaction.
	 */
	public UUID getUUID();
	
	/**
	 * @return Display name of transaction.
	 */
	public String getDisplayName();
	
	/**
	 * @return Owner of transaction.
	 */
	public Process getOwner();
}
