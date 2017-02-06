package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

public interface Process extends Serializable {
	
	public UUID getUUID();
	
	public InetAddress getInetAddress();
	
	public int getPort();
	
	@Override
	public int hashCode();
	
	@Override
	public boolean equals(Object obj);
}
