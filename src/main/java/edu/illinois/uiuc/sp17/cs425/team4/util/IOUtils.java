package edu.illinois.uiuc.sp17.cs425.team4.util;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Class to hold bunch of static utility methods for IO.
 * 
 * @author bbassi2
 */
public final class IOUtils {

	/**
	 * Private constructor so that no one could create instances.
	 */
	private IOUtils() {}
	
	/**
	 * Read input from stream assuming first four bytes to be length of the rest of the message.
	 * @param inputStream Input stream.
	 * @return stream contents as byte[]
	 * @throws IOException if there is a problem while reading stream.
	 */
	public static byte[] readInputSizePrefixed(InputStream inputStream) throws IOException {
		DataInput input = new DataInputStream(inputStream);
		int messageLength = input.readInt();
		byte[] msg = new byte[messageLength];
		// First four-bytes were message length, hence starting from 5th is the message.
		input.readFully(msg);
		return msg;
	}
	
	/**
	 * Creates a new byte[] with size of the provided byte[] prefixed.
	 * Four bytes are used to denote the size and these should be interpreted as integer.
	 * @param data date.
	 * @return size prefixed to data.
	 */
	public static byte[] prefixSize(byte[] data) {
		byte[] size = ByteBuffer.allocate(Integer.BYTES).putInt(data.length).array();
		return ArrayUtils.addAll(size, data);
	}
}
