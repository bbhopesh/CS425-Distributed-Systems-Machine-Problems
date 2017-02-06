package edu.illinois.uiuc.sp17.cs425.team4.component;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * Interface to convert between network specific protocols/message formats to one standard format.
 * As the kind of objects that each network protocol uses to send/receive differ a lot,
 * the interface had to be keep type of connection as Object.
 * 
 * @author bbassi2
 */
public interface MessageAdaptor {
	/**
	 * Read source and message from the connection.
	 * @param conn connection.
	 * @return pair containing message and it's source.
	 */
	public Pair<Process,Message> read(Object conn);
	
	/**
	 * Write the source and message to the connection.
	 * @param conn Connection.
	 * @param sourceAndMessage Pair containing source and message.
	 */
	public void write(Object conn, Pair<Process,Message> sourceAndMessage);
}
