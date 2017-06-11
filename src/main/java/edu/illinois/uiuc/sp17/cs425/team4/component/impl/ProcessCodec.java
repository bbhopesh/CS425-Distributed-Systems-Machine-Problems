package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import edu.illinois.uiuc.sp17.cs425.team4.component.Codec;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * A process codec that creates a string by appending port to hostname and then uses string codec to convert to bytes.
 * This codec doesn't support decoding.
 * 
 * @author bbassi2
 *
 */
public class ProcessCodec implements Codec<Process> {
	
	private final Codec<String> stringCodec = new PlainVanillaStringCodec();
	
	@Override
	public byte[] encode(Process p) {
		// Concatenate hostname and port name and use string codec.
		String processStr = p.getInetAddress() + String.valueOf(p.getPort());
		return this.stringCodec.encode(processStr);
	}

	@Override
	public Process decode(byte[] bytes) {
		throw new UnsupportedOperationException();
	}

}
