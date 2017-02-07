package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.ChatApplication;
import edu.illinois.uiuc.sp17.cs425.team4.component.Multicast;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.TextMessage;

public class SimpleChatApplication implements ChatApplication {

	private static final String STOP_MSG = "exit()";
	private static final String GREETING = "Welcome to chat application of CS425 Team 4. Type " + STOP_MSG + " to quit.";
	private static final String EXIT_MSG = "Good bye.";
	private static final String PROMPT = ">> ";
	private final InputStream userInput;
	private final PrintStream output;
	private final Multicast multicast;
	private final Model model;
	
	public SimpleChatApplication(Multicast multicast, Model model,
			InputStream userInput, PrintStream output) {
		this.userInput = userInput;
		this.output = output;
		this.model = model;
		this.multicast = multicast;
		this.multicast.registerApplication(this);
	}
	
	public SimpleChatApplication(Multicast multicast, Model model) {
		this(multicast, model, System.in, System.out);
	}
	
	@Override
	public void deliver(Pair<Process, Message> incomingMessage) {
		TextMessage message = (TextMessage) incomingMessage.getRight();
		// We don't care about which peer forwarded the message to us.
		// We care about whose message is this.
		Process sender = message.getOriginatingSource();
		StringBuilder sb = new StringBuilder();
		
		sb.append(sender.getDisplayName())
			.append(": ")
			.append(message.getText());
		this.output.println(sb.toString());
	}

	@Override
	public void startChat() {
		this.output.println(GREETING);
		Scanner input = new Scanner(this.userInput);
		while(true) {
			this.output.print(PROMPT);
			String nextMsg = input.nextLine();
			if(shouldStop(nextMsg)) break;
			this.multicast.multicast(toTextMessage(nextMsg));
		}
		this.output.println(EXIT_MSG);
		input.close();
	}
	
	private boolean shouldStop(String string) {
		return string != null && string.trim().equals(STOP_MSG);
	}
	
	private TextMessage toTextMessage(String msg) {
		return model.createTextMessage(msg);
	}
}
