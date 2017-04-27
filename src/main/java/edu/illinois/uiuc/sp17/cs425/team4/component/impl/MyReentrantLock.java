package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

public class MyReentrantLock extends ReentrantLock {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4157417350220171069L;
	
	public Collection<Thread> getQueuedThreads() {
		return super.getQueuedThreads();
	}
}
