package edu.illinois.uiuc.sp17.cs425.team4.component;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

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
