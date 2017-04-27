package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MyReentrantReadWriteLock extends ReentrantReadWriteLock {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1194365083847807095L;

	public Collection<Thread> getQueuedWriterThreads() {
		return super.getQueuedWriterThreads();
	}
}
