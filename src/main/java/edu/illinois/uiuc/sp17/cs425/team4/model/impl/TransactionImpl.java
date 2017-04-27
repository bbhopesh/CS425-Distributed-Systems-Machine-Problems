package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.io.Serializable;
import java.util.UUID;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

public class TransactionImpl implements Transaction, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5613089691345612420L;
	private final Process owner;
	private final String displayName;
	private final UUID uId;
	
	public TransactionImpl(Process owner, String displayName) {
		this(owner, displayName, UUID.randomUUID());
	}
	
	public TransactionImpl(Process owner, String displayName, UUID uId) {
		this.owner = owner;
		this.displayName = displayName;
		this.uId = uId;
	}
	
	@Override
	public UUID getUUID() {
		return this.uId;
	}

	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	@Override
	public Process getOwner() {
		return this.owner;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		TransactionImpl other = (TransactionImpl) obj;
		if (uId == null) {
			if (other.uId != null)
				return false;
		} else if (!uId.equals(other.uId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return this.displayName;
	}
	
	
	
}
