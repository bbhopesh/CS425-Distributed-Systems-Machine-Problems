package edu.illinois.uiuc.sp17.cs425.team4.POC;

import java.util.concurrent.locks.ReadWriteLock;

import com.google.common.util.concurrent.Striped;


public class GuavaStripedLockPOC {
	
	public static void main(String[] args) {
		Striped<ReadWriteLock> stripes = Striped.readWriteLock(5);
		stripes.get("hi").readLock().lock();
		//stripes.get("hi").readLock().unlock();
		new MyThread(stripes).start();
	}
	
	private static class MyThread extends Thread {
		
		private final Striped<ReadWriteLock> stripes;

		public MyThread(Striped<ReadWriteLock> stripes) {
			this.stripes = stripes;
		}
		
		public void run() {
			System.out.println("Unlocking read lock on thread: " + Thread.currentThread().getId());
	        this.stripes.get("hi").readLock().unlock();
	    }
	}
}
