package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import org.apache.commons.lang3.exception.ContextedRuntimeException;

import edu.illinois.uiuc.sp17.cs425.team4.component.Codec;
import edu.illinois.uiuc.sp17.cs425.team4.component.MessageReceiptListener;
import edu.illinois.uiuc.sp17.cs425.team4.util.IOUtils;

/**
 * Plain Vanilla message listener. 
 * This class just prints incoming messages to the provided print stream.
 * By default, the print stream is stdout but can be set to something else.
 * The class uses a codec to convert between incoming message byte[] and type T.
 * @author bbassi2
 * 
 * @param <T> type to convert incoming byte[] to.
 */
public class PlainVanillaTcpMessageListener<T> implements MessageReceiptListener {
	/** Codec. */
	private final Codec<T> codec;
	/** Print stream to which message should be displayed. */
	private final PrintStream printStream;
	
	/**
	 * Create an instance.
	 * @param codec Codec to be used.
	 * @param printStream Print stream to print messages to.
	 */
	public PlainVanillaTcpMessageListener(Codec<T> codec, PrintStream printStream) {
		this.codec = codec;
		this.printStream = printStream;
	}
	
	/**
	 * Create an instance. Defaults printstream to System.out
	 * @param codec Codec to be used.
	 */
	public PlainVanillaTcpMessageListener(Codec<T> codec) {
		this(codec, System.out);
	}
	
	
	@Override
	public void messageReceived(Object incomingConnection) {
		// Cast to TCP socket.
		Socket socket = (Socket) incomingConnection;
		try {
			byte[] msg = IOUtils.readInputSizePrefixed(socket.getInputStream());
			T x = this.codec.decode(msg);
			this.printStream.println(socket.getInetAddress().getHostName() + ":" + socket.getPort() + ": " +x);
		} catch (IOException e) {
			throw new ContextedRuntimeException(e);
		}
		
		try {
			socket.close();
		} catch (IOException e) {
			// just log and ignore if there is an error while closing socket.
			e.printStackTrace();
		}
		
	}

}
