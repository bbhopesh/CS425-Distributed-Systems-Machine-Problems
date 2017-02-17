package edu.illinois.uiuc.sp17.cs425.team4.component.tcpimpl;

import java.io.IOException;
import java.net.Socket;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.MessageAdaptor;
import edu.illinois.uiuc.sp17.cs425.team4.component.ResponseWriter;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class TCPResponseWriter implements ResponseWriter{
	
	private final MessageAdaptor messageAdaptor;
	private final Socket tcpSocket;
	private final Process myIdentity;
	
	
	
	public TCPResponseWriter(MessageAdaptor messageAdaptor, Socket tcpSocket,
			Process myIdentity) {
		this.messageAdaptor = messageAdaptor;
		this.tcpSocket = tcpSocket;
		this.myIdentity = myIdentity;
	}

	@Override
	public void writeResponse(Message response) throws ContextedRuntimeException {
		// Not sure if we should allow calling this method once.
		// Anyway, not calling this more than once for now.
		if (response != null) {
			try {
				this.messageAdaptor.write(this.tcpSocket,
								Pair.of(this.myIdentity, response));
			} catch (Exception e) {
				throw new ContextedRuntimeException("Could not send the reponse.", e);
			}
		}
	}

	@Override
	public void close() {
		try {
			this.tcpSocket.close();
		} catch (IOException e) {
			// ignore
			// TODO Add log4j and print exception in debug mode.
		}
	}
}
