package edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl;

import java.io.IOException;
import java.net.Socket;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.exceptions.MessengerException;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * A response writer implementation for TCP.
 * 
 * @author bbassi2
 */
public class TCPResponseWriter implements ResponseWriter {
	/**  Message adaptor. */
	private final TCPMessageAdaptor messageAdaptor;
	/** Tcp socket to write repl to. */
	private final Socket tcpSocket;
	/**  My identity. */
	private final Process myIdentity;
	

	public TCPResponseWriter(TCPMessageAdaptor messageAdaptor, Socket tcpSocket,
			Process myIdentity) {
		this.messageAdaptor = messageAdaptor;
		this.tcpSocket = tcpSocket;
		this.myIdentity = myIdentity;
	}

	@Override
	public void writeResponse(Message response) throws MessengerException {
		// Not sure if we should allow calling this method once.
		// Anyway, not calling this more than once for now anywhere in my existing code.
		if (response != null) {
			try {
				//addRandomDelay(response);
				this.messageAdaptor.write(this.tcpSocket,
								Pair.of(this.myIdentity, response));
			} catch (Exception e) {
				throw new MessengerException("Could not send the reponse.", e);
			}
		}
	}

	@Override
	public void close() {
		try {
			this.tcpSocket.close();
		} catch (IOException e) {
			// ignore
		}
	}
}
