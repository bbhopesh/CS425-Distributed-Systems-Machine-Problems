package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;


public class DeadlockDoctor<K> implements Runnable {
	/**  Logger. */
	private final static Logger LOG = Logger.getLogger(DeadlockDoctor.class.getName());
	
	private final ConcurrentMap<K, MyReentrantReadWriteLock> readWriteLocks;
	private final ConcurrentMap<K, MyReentrantLock> upgradeReadToWriteLocks;
	private ConcurrentMap<K, Queue<Thread>> rToWUpgradeDeadlockCandidates;
	
	public DeadlockDoctor(ConcurrentMap<K, Queue<Thread>> rToWUpgradeDeadlockCandidates,
							ConcurrentMap<K, MyReentrantReadWriteLock> readWriteLocks,
							ConcurrentMap<K, MyReentrantLock> upgradeReadToWriteLocks) {
		this.readWriteLocks = readWriteLocks;
		this.upgradeReadToWriteLocks = upgradeReadToWriteLocks;
		this.rToWUpgradeDeadlockCandidates = rToWUpgradeDeadlockCandidates;
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				lockOrderingDeadlocks();
				readToWriteUpgradeDeadlocks();
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// break;
					// ignore.
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
			
		}
		
	}
	
	private void lockOrderingDeadlocks() {
		// Use Java features :)
		ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
		long[] deadlockedThreads = tmx.findDeadlockedThreads();
		 if (deadlockedThreads != null) {
		     ThreadInfo[] infos = tmx.getThreadInfo(deadlockedThreads, true, true);
		     System.out.println("The following threads are deadlocked:");
		     for (ThreadInfo ti : infos) {
		        System.out.println(ti);
		     }
		  }
	}
	
	private void readToWriteUpgradeDeadlocks() {
		// If one or more threads are holding read lock and waiting at upgrade lock, and another thread is
		// holding upgrade lock and waiting at write lock, then we have deadlock and all locks holding
		// read lock and waiting for upgrade lock should be killed.
		List<Thread> toBeKilled = new LinkedList<>();
		
		for (Entry<K, Queue<Thread>> deadlockCandidateEntry: this.rToWUpgradeDeadlockCandidates.entrySet()) {
			K deadlockCandidateKey = deadlockCandidateEntry.getKey();
			Queue<Thread> deadlockCandidateThreads = deadlockCandidateEntry.getValue();
			MyReentrantLock upgradeLock = this.upgradeReadToWriteLocks.get(deadlockCandidateKey);
			MyReentrantReadWriteLock readWriteLock =  this.readWriteLocks.get(deadlockCandidateKey);
			if (upgradeLock != null && readWriteLock != null) {
				Collection<Thread> queuedWriterThreads = readWriteLock.getQueuedWriterThreads();
				if (queuedWriterThreads != null && !queuedWriterThreads.isEmpty()) {
					// we take upgrade lock before taking write lock, hence at max only one thread should be queued for write.
					Collection<Thread> upgradeQueuedThreads = upgradeLock.getQueuedThreads();
					if (upgradeQueuedThreads != null && !upgradeQueuedThreads.isEmpty()) {
						for (Thread deadlockCandidateThread: deadlockCandidateThreads) {
							if (upgradeQueuedThreads.contains(deadlockCandidateThread)) {
								toBeKilled.add(deadlockCandidateThread);
							}
						}
					}
					
				}
			}
			
		}
		
		for (Thread kill: toBeKilled) {
			System.out.println("@@: " + kill.getId());
		}
	}
}
