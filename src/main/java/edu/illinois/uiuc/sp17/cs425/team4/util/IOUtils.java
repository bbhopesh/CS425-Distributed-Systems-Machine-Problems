package edu.illinois.uiuc.sp17.cs425.team4.util;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.commons.lang3.ArrayUtils;

public final class IOUtils {

	private IOUtils() {}
	
	public static byte[] readInputSizePrefixed(InputStream inputStream) throws IOException {
		DataInput input = new DataInputStream(inputStream);
		int messageLength = input.readInt();
		byte[] msg = new byte[messageLength];
		// First four-bytes were message length, hence starting from 5th is the message.
		input.readFully(msg);
		return msg;
	}
	
	public static byte[] prefixSize(byte[] data) {
		byte[] size = ByteBuffer.allocate(Integer.BYTES).putInt(data.length).array();
		return ArrayUtils.addAll(size, data);
	}
}
