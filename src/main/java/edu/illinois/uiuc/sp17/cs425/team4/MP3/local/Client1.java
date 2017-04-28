package edu.illinois.uiuc.sp17.cs425.team4.MP3.local;

import static edu.illinois.uiuc.sp17.cs425.team4.MP3.local.LocalSystemConfig.*;

import java.io.IOException;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

import edu.illinois.uiuc.sp17.cs425.team4.MP3.ClientInitializer;


public class Client1 {
	private static final Process MY_IDENTITY = LocalSystemConfig.myIdentity("C1");
	
	public static void main(String[] args) throws IOException, InterruptedException {
		ClientInitializer initializer = 
				new ClientInitializer(SERVERS, MY_IDENTITY, 
												MODEL, LOCK_SERVICE,
												READ_WRITE_REQUEST_TIMEOUT);
		
		initializer.initialize();
	}

}
