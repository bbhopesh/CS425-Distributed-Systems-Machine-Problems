package edu.illinois.uiuc.sp17.cs425.team4.POC;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockTesting {

	public static void main(String[] args) throws InterruptedException {
		
		new DeadlockDetection().start();
		
		ReentrantReadWriteLock lock1 = new ReentrantReadWriteLock();
		System.out.println("Locking1 read lock on thread: " + Thread.currentThread().getId());
		//lock1.readLock().lock();
		lock1.writeLock().lock();
		ReentrantReadWriteLock lock2 = new ReentrantReadWriteLock();
		new MyThread(lock1, lock2).start();
		Thread.sleep(5);
		System.out.println("Locking2 write lock on thread: " + Thread.currentThread().getId());
		lock2.writeLock().lock();
		
	}
	
	private static class MyThread extends Thread {
		
		private final ReadWriteLock lock1;
		private final ReadWriteLock lock2;

		public MyThread(ReadWriteLock lock1, ReadWriteLock lock2) {
			this.lock1 = lock1;
			this.lock2 = lock2;
		}
		
		public void run() {
			System.out.println("Locking2 read lock on thread: " + Thread.currentThread().getId());
			//this.lock2.readLock().lock();
			lock2.writeLock().lock();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Locking1 write lock on thread: " + Thread.currentThread().getId());
			this.lock1.writeLock().lock();
	    }
	}
	
	private static class DeadlockDetection extends Thread {
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
		
		public void run() {
			while (true) {
				lockOrderingDeadlocks();
				
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	

}
