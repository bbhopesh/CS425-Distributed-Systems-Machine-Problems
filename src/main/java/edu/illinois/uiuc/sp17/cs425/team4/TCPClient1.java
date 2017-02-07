package edu.illinois.uiuc.sp17.cs425.team4;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageReceiptListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.Messenger;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.PlainVanillaTcpMessageListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl.TCPMessengerBuilder;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;

public class TCPClient1 {
	public static void main(String[] args) throws UnknownHostException, IOException {
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		Messenger tcpMessenger = new TCPMessengerBuilder()
									.setThreadPool(threadPool)
									.setPort(10010)
									.setMyIdentity(getProcess())
									.setMessageAdaptor(new TCPMessageAdaptor())
									.build();
		MessageReceiptListener listener = 
				new PlainVanillaTcpMessageListener(System.out);
		
		tcpMessenger.registerListener(listener);
		tcpMessenger.initialize();
		Message m = tcpMessenger.send(Pair.of(getProcess1(), new ModelImpl().createTextMessage(getProcess(), "Balle")));
		if (m != null) {
			System.out.println(m.getMessageType());
		}
		
	}
	
	private static Process getProcess() throws UnknownHostException {
		UUID uId = new UUID(85, 36);
		InetAddress addr = InetAddress.getLocalHost();
		return new ModelImpl().createProcess(addr, 10010, uId);
	}
	
	private static Process getProcess1() throws UnknownHostException {
		UUID uId = new UUID(34, 78);
		InetAddress addr = InetAddress.getLocalHost();
		return new ModelImpl().createProcess(addr, 10005, uId);
	}
}
