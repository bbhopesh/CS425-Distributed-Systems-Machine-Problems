package edu.illinois.uiuc.sp17.cs425.team4.POC;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageReceiptListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessengerBuilder;

public class Main {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("Let's start on MP1.");
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		Messenger tcpMessenger = new TCPMessengerBuilder()
									.setThreadPool(threadPool)
									.setPort(10005)
									.setMyIdentity(getProcess())
									.setMessageAdaptor(new TCPMessageAdaptor())
									.build();
		MessageReceiptListener listener = 
				new PlainVanillaTcpMessageListener();
		
		tcpMessenger.registerListener(listener);
		tcpMessenger.initialize();
	}
	
	private static Process getProcess() throws UnknownHostException {
		UUID uId = new UUID(34, 78);
		InetAddress addr = InetAddress.getLocalHost();
		return new ModelImpl().createProcess(addr, 10005,  "Main", uId);
	}

}
