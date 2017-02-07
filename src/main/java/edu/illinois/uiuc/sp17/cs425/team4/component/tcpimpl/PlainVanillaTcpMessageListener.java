package edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl;

import java.io.PrintStream;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageReceiptListener;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.model.TextMessage;

/**
 * Plain Vanilla message listener. 
 * This class just prints incoming messages to the provided print stream.
 * By default, the print stream is stdout but can be set to something else.
 * The class uses a codec to convert between incoming message byte[] and type T.
 * @author bbassi2
 * 
 * @param <T> type to convert incoming byte[] to.
 */
public class PlainVanillaTcpMessageListener implements MessageReceiptListener {
	/** Print stream to which message should be displayed. */
	private final PrintStream printStream;
	
	/**
	 * Create an instance.
	 * @param codec Codec to be used.
	 * @param printStream Print stream to print messages to.
	 */
	public PlainVanillaTcpMessageListener(PrintStream printStream) {
		this.printStream = printStream;
	}
	
	
	
	/*@Override
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
		
	}*/

	@Override
	public Message messageReceived(Pair<Process, Message> sourceAndMsg) {
		Process x = sourceAndMsg.getLeft();
		TextMessage txt = (TextMessage) sourceAndMsg.getRight();
		System.out.println(txt);
		return null;
	}



	@Override
	public void notifyFailure(Pair<Pair<Process, Message>, Message> failedMsg, Exception exception) {
		// TODO Auto-generated method stub
		
	}

}
