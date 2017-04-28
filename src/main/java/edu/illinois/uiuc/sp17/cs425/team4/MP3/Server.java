package edu.illinois.uiuc.sp17.cs425.team4.MP3;

import static edu.illinois.uiuc.sp17.cs425.team4.MP3.SystemConfig.*;

import java.io.IOException;


public class Server {
	public static void main(String[] args) throws IOException {
		ServerInitializer initializer = new ServerInitializer(MY_IDENTITY, MODEL);
		initializer.initialize();
	}
}
