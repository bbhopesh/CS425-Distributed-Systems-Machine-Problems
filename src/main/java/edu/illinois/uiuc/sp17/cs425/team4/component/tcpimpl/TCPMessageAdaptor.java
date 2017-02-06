package edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageAdaptor;
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
public class TCPMessageAdaptor implements MessageAdaptor {

	@Override
	public Pair<Process, Message> read(Object conn) {
		try {
			Socket socket =  (Socket) conn;
			// Always assume that some message is sent. If not meaningful, NO_OP should be sent.
			// Read message and extract serialized form based on prefixed length.
			byte[] serverResponse = IOUtils.readInputSizePrefixed(socket.getInputStream());
			// Deserialize.
			Pair<Process, Message> srcAndMsg = SerializationUtils.deserialize(serverResponse);
			return srcAndMsg;
		} catch (IOException e) {
			throw new ContextedRuntimeException(e);
		}
	}

	@Override
	public void write(Object conn, Pair<Process, Message> sourceAndMessage) {
		try {
			Socket socket = (Socket) conn;
			// Serialize.
			byte[] srcAndMsg = SerializationUtils.serialize(sourceAndMessage);
			// Prefix size of the message.
			byte[] sizePrefixedSrcAndMsg = IOUtils.prefixSize(srcAndMsg);
			// Write to socket.
			DataOutput outToServer = new DataOutputStream(socket.getOutputStream());
			outToServer.write(sizePrefixedSrcAndMsg);
		} catch (IOException e) {
			throw new ContextedRuntimeException(e);
		}
	}
}
