package edu.illinois.uiuc.sp17.cs425.team4.component;

public class PlainVanillaStringCodec implements Codec<String> {

	@Override
	public byte[] encode(String t) {
		return t.getBytes();
	}

	@Override
	public String decode(byte[] bytes) {
		return new String(bytes);
	}

}
