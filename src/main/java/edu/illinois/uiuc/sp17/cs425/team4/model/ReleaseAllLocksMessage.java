package edu.illinois.uiuc.sp17.cs425.team4.model;

public interface ReleaseAllLocksMessage extends Message {
	public Transaction getTransaction();
}
