package edu.illinois.uiuc.sp17.cs425.team4.POC;

import java.util.concurrent.Semaphore;

public class SemaphoreTesting {
	public static void main(String[] args) throws InterruptedException {
		Semaphore s =  new Semaphore(1);
		s.acquire();
		//s.release();
		Boolean done = false;
		new MyThread(s, done).start();
		Thread.sleep(2000);
		System.out.println("releasing");
		s.release();
		
	}
	
	private static class MyThread extends Thread {
		
		private final Semaphore s;
		@SuppressWarnings("unused")
		private volatile Boolean done;
		
		public MyThread(Semaphore s, Boolean done) {
			this.s = s;
			this.done = done;
		}
		public void run() {
			try {
				System.out.println("hi");
				s.acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("bye");
			this.done = true;
		}
	}
}
