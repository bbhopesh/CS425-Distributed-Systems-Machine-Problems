package edu.illinois.uiuc.sp17.cs425.team4.MP3;

import static edu.illinois.uiuc.sp17.cs425.team4.MP3.SystemConfig.*;

import java.io.IOException;


public class Client {

	public static void main(String[] args) throws IOException, InterruptedException {
		ClientInitializer initializer = 
				new ClientInitializer(SERVERS, MY_IDENTITY, 
												MODEL, LOCK_SERVICE,
												READ_WRITE_REQUEST_TIMEOUT);
		
		initializer.initialize();
	}

}
