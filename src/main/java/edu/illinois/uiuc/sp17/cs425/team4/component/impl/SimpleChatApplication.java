package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.io.PrintStream;
import java.util.Scanner;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.ChatApplication;
import edu.illinois.uiuc.sp17.cs425.team4.component.Multicast;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message.MessageType;
import edu.illinois.uiuc.sp17.cs425.team4.model.Model;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.TextMessage;
import edu.illinois.uiuc.sp17.cs425.team4.model.impl.ModelImpl;

/**
 * A simple chat application that reads takes user input from standard input
 * and stores output in the provided output print stream.
 * If no print stream is provided, standard output is used.
 * 
 * @author bbassi2
 */
public class SimpleChatApplication implements ChatApplication {

	/** Stop message. */
	private static final String STOP_MSG = "exit()";
	/** Greeting message. */
	private static final String GREETING = "Welcome to chat application of CS425 Team 4. I am %s. Type " + STOP_MSG + " to quit.";
	/** Exit message. */
	private static final String EXIT_MSG = "Good bye.";
	/** Prompt string. */
	private static final String PROMPT = ">> ";
	/** Output print stream. Chat output goes to this stream. */
	private final PrintStream output;
	/** Multicast. */
	private final Multicast multicast;
	/** Model. */
	private final Model model;
	/** Myself. */
	private final Process mySelf;
	
	/**
	 * Create an instance.
	 * @param multicast Multicast.
	 * @param mySelf Process object to be used as m identity.
	 * @param output Output printstream that takes chat transcript.
	 */
	public SimpleChatApplication(Multicast multicast, Process mySelf, PrintStream output) {
		this.output = output;
		this.multicast = multicast;
		this.multicast.registerApplication(this);
		this.mySelf = mySelf;
		this.model = new ModelImpl();
	}
	
	/**
	 * Create an instance. 
	 * @param multicast Multicast.
	 * @param mySelf Process object to be used as m identity.
	 */
	public SimpleChatApplication(Multicast multicast, Process mySelf) {
		this(multicast, mySelf, System.out);
	}
	
	@Override
	public void deliver(Pair<Process, Message> incomingMessage) {
		Process sender = incomingMessage.getLeft();
		Message message = incomingMessage.getRight();
		if (isTextMessage(message)) {
			handleTextMessage((TextMessage) message);
		} else if (isFailureMessage(message)) {
			handleFailureMessage(sender);
		} else {
			// ignore.
			// SimpleChatApplication only handles two types of messages.
		}
	}
	
	private boolean isTextMessage(Message message) {
		return message.getMessageType() == MessageType.TEXT;
	}
	
	private boolean isFailureMessage(Message message) {
		return message.getMessageType() == MessageType.LEFT;
	}
	
	private void handleTextMessage(TextMessage txt) {
		// We don't care about which peer forwarded the message to us.
		// We care about whose message is this.
		Process sender = txt.getOriginatingSource();
		StringBuilder sb = new StringBuilder();
		
		sb.append(sender.getDisplayName())
		.append(": ")
		.append(txt.getText());
		this.output.println(sb.toString());
	}
	
	private void handleFailureMessage(Process failure) {
		this.output.println(failure.getDisplayName() + " has left the chat.");
	}

	@Override
	public void startChat() {
		System.out.println(String.format(GREETING, this.mySelf.getDisplayName()));
		Scanner input = new Scanner(System.in);
		while(true) {
			System.out.print(PROMPT);
			String nextMsg = input.nextLine();
			if(shouldStop(nextMsg)) break;
			// Multicast gives the failed messages list back but can't do anything with it, so ignore.
			this.multicast.multicast(toTextMessage(nextMsg));
		}
		System.out.println(EXIT_MSG);
		input.close();
		// close output.
		this.output.close();
	}
	
	/**
	 * Checks if stopping condition has met.
	 * @param string Message.
	 * @return boolean indicating if chat should be ended.
	 */
	private boolean shouldStop(String string) {
		return string != null && string.trim().equals(STOP_MSG);
	}
	
	/**
	 * Wraps string in a text message.
	 * @param msg Message.
	 * @return string wrapped in a text message.
	 */
	private TextMessage toTextMessage(String msg) {
		return this.model.createTextMessage(msg, this.mySelf);
	}
}
