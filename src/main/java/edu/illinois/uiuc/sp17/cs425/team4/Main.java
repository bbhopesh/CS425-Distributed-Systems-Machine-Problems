package edu.illinois.uiuc.sp17.cs425.team4;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageReceiptListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.PlainVanillaTcpMessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.StringCodec;
import edu.illinois.uiuc.sp17.cs425.team4.component.impl.TCPMessengerBuilder;

public class Main {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("Let's start on MP1.");
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		Messenger tcpMessenger = new TCPMessengerBuilder()
									.setThreadPool(threadPool)
									.setPort(10005)
									.build();
		MessageReceiptListener listener = 
				new PlainVanillaTcpMessageListener<String>(new StringCodec());
		
		tcpMessenger.registerListener(listener);
		tcpMessenger.initialize();
	}

}
