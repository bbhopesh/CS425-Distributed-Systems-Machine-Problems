package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.UUID;

public interface Transaction {
	public UUID getUUID();
	
	public String getDisplayName();
	
	public Process getOwner();
}
