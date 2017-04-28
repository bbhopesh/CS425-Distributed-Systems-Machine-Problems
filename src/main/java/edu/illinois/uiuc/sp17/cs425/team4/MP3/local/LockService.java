package edu.illinois.uiuc.sp17.cs425.team4.MP3.local;

import static edu.illinois.uiuc.sp17.cs425.team4.MP3.local.LocalSystemConfig.*;

import java.io.IOException;

import edu.illinois.uiuc.sp17.cs425.team4.MP3.LockServiceInitializer;


public class LockService {
	
	public static void main(String[] arg) throws IOException {
		LockServiceInitializer lockServiceInitializer = 
					new LockServiceInitializer(LOCK_SERVICE, MODEL);
		lockServiceInitializer.initialize();
	}
}
