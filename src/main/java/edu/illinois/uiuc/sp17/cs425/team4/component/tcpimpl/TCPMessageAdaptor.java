package edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;
import edu.illinois.uiuc.sp17.cs425.team4.util.IOUtils;

/**
 * An implementation of the message adaptor for TCP protocol.
 * This implementation uses the length prefix to indicate the length of the outgoing message.
 * Same format is assumed for the incoming message where the first four bytes are treated as
 * integer specifying length of the message.
 * 
 * Incoming messages are assumed to be serialized Pair<Process, Message>,
 * SerializationUtils are used to deserialize.
 * 
 * @author bbassi2
 *
 */
public class TCPMessageAdaptor {
	/** Logger. */
	private final static Logger LOG = Logger.getLogger(TCPMessageAdaptor.class.getName());
	
	public Pair<Process, Message> read(Socket socket) throws IOException {
		try {
			byte[] serverResponse = IOUtils.readInputSizePrefixed(socket.getInputStream());
			// Deserialize.
			Pair<Process, Message> srcAndMsg = SerializationUtils.deserialize(serverResponse);
			return srcAndMsg;
		} catch (SocketTimeoutException e) {
			// timeout.
			LOG.debug(e.getMessage());
			throw e;
		} catch (EOFException e) {
			// remote socket closed by peer.
			
			// It is because of the way our IOUtils method is implemented that we are getting end of file exception 
			// if remote peer closes socket before writing anything.
			// I think, it is happening because readInt is throwing EOF if remote peer closes connection without writing
			// anything. I am not sure though, need to look at javadocs and different edge cases more carefully.
			
			// In any case, this block is meant to detect when remote peer closes socket. If there is a better
			// way to detect that, then, we can move to that.
			LOG.debug(e.getMessage());
			return null;
		}
	}

	public void write(Socket socket, Pair<Process, Message> sourceAndMessage) throws IOException {
		// Serialize.
		byte[] srcAndMsg = SerializationUtils.serialize(sourceAndMessage);
		// Prefix size of the message.
		byte[] sizePrefixedSrcAndMsg = IOUtils.prefixSize(srcAndMsg);
		// Write to socket.
		DataOutput outToServer = new DataOutputStream(socket.getOutputStream());
		outToServer.write(sizePrefixedSrcAndMsg);
	}
}
