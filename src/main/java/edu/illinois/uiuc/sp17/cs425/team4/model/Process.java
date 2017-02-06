package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

/** 
 * Interface to represent a process in a distributed system.
 * 
 * @author bbassi2
 */
public interface Process extends Serializable {
	/** Unique identifier of this process in the distributed system. */
	public UUID getUUID();
	
	/** Address at which the process is running. */
	public InetAddress getInetAddress();
	
	/** Port at which process is listening. */
	public int getPort();
	
	@Override
	public int hashCode();
	
	@Override
	public boolean equals(Object obj);
}
