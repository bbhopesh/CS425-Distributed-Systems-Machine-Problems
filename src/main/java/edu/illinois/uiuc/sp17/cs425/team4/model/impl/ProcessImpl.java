package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import net.jcip.annotations.Immutable;

/**
 * An implementation of the Process.
 * equals and hashcode only consider the UUID and no other attribute.
 * Child classes are not allowed to override these two methods.
 * 
 * @author bbassi2
 */
@Immutable
public class ProcessImpl implements Process, Serializable {

	/** Serial Version UID. */
	private static final long serialVersionUID = -7747392582909878577L;
	/** Unique identifier of the process. */
	private final UUID uId;
	/** Address of the process. */
	private final InetAddress inetAddress;
	/** Port of the process. */
	private final int port;
	
	/**
	 * Create an instance.
	 * @param inetAddress Address of the process.
	 * @param port Port of the process.
	 * @param uId Unique identifier of the process.
	 */
	public ProcessImpl(InetAddress inetAddress, int port, UUID uId) {
		this.inetAddress = inetAddress;
		this.port = port;
		this.uId = uId;
	}
	
	/**
	 * Create an instance with random unique identifier.
	 * @param inetAddress Address of the process.
	 * @param port Port of the process.
	 */
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

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uId == null) ? 0 : uId.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProcessImpl other = (ProcessImpl) obj;
		if (uId == null) {
			if (other.uId != null)
				return false;
		} else if (!uId.equals(other.uId))
			return false;
		return true;
	}
	
}
