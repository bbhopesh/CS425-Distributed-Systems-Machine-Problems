package edu.illinois.uiuc.sp17.cs425.team4.MP3.local;

import static edu.illinois.uiuc.sp17.cs425.team4.MP3.local.LocalSystemConfig.*;

import java.io.IOException;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

import edu.illinois.uiuc.sp17.cs425.team4.MP3.ServerInitializer;


public class Server5 {
	private static final Process MY_IDENTITY = LocalSystemConfig.myIdentity("E");
	
	public static void main(String[] args) throws IOException {
		ServerInitializer initializer = new ServerInitializer(MY_IDENTITY, MODEL);
		initializer.initialize();
	}
}
