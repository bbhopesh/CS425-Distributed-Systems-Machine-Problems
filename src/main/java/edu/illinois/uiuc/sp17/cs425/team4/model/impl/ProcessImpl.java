package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import net.jcip.annotations.Immutable;

@Immutable
public class ProcessImpl implements Process, Serializable {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((inetAddress == null) ? 0 : inetAddress.hashCode());
		result = prime * result + port;
		result = prime * result + ((uId == null) ? 0 : uId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProcessImpl other = (ProcessImpl) obj;
		if (inetAddress == null) {
			if (other.inetAddress != null)
				return false;
		} else if (!inetAddress.equals(other.inetAddress))
			return false;
		if (port != other.port)
			return false;
		if (uId == null) {
			if (other.uId != null)
				return false;
		} else if (!uId.equals(other.uId))
			return false;
		return true;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -7747392582909878577L;
	private final UUID uId;
	private final InetAddress inetAddress;
	private final int port;
	
	public ProcessImpl(InetAddress inetAddress, int port, UUID uId) {
		this.inetAddress = inetAddress;
		this.port = port;
		this.uId = uId;
	}
	
	public ProcessImpl(InetAddress inetAddress, int port) {
		this(inetAddress, port, UUID.randomUUID());
	}
	
	@Override
	public UUID getUUID() {
		return this.uId;
	}

	@Override
	public InetAddress getInetAddress() {
		return this.inetAddress;
	}

	@Override
	public int getPort() {
		return this.port;
	}

}
