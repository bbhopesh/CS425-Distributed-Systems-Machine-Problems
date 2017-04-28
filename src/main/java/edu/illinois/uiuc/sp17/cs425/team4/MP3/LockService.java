package edu.illinois.uiuc.sp17.cs425.team4.MP3;

import static edu.illinois.uiuc.sp17.cs425.team4.MP3.SystemConfig.*;

import java.io.IOException;


public class LockService {
	
	public static void main(String[] arg) throws IOException {
		LockServiceInitializer lockServiceInitializer = 
					new LockServiceInitializer(LOCK_SERVICE, MODEL);
		lockServiceInitializer.initialize();
	}
}
