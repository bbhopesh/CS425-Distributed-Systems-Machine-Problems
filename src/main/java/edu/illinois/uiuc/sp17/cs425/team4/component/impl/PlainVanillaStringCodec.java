package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import edu.illinois.uiuc.sp17.cs425.team4.component.Codec;

/**
 * A simple String codec which uses new String(byte array) to decode and string.getBytes to encode.
 * 
 * @author bbassi2.
 */
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
