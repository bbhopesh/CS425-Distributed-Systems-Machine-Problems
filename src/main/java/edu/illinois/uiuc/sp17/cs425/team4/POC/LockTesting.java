package edu.illinois.uiuc.sp17.cs425.team4.POC;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockTesting {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ReadWriteLock lock = new ReentrantReadWriteLock();
		System.out.println("Locking read lock on thread: " + Thread.currentThread().getId());
		lock.writeLock().lock();
		lock.readLock().lock();
		System.out.println("Unlocking read lock on thread: " + Thread.currentThread().getId());
		System.out.println("Hmmmmmmmm");
		lock.readLock().unlock();
		lock.writeLock().unlock();
		
	}
	
	private static class MyThread extends Thread {
		
		private final ReadWriteLock readWriteLock;

		public MyThread(ReadWriteLock readWriteLock) {
			this.readWriteLock = readWriteLock;
		}
		
		public void run() {
			System.out.println("Unlocking read lock on thread: " + Thread.currentThread().getId());
	        this.readWriteLock.readLock().unlock();
	    }
	}
	
	

}
