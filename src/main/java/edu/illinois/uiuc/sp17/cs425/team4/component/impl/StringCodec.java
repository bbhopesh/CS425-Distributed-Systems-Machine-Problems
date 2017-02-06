package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import edu.illinois.uiuc.sp17.cs425.team4.component.Codec;
import net.jcip.annotations.Immutable;

/**
 * String codec. Used to convert between byte[] and string.
 * 
 * @author bbassi2
 */
@Immutable
public class StringCodec implements Codec<String> {
	/** Charset to be used for conversion between byte[] and String. */
	private final Charset charset;
	
	/**
	 * Create an instance with UTF_8 charset.
	 */
	public StringCodec() {
		this(StandardCharsets.UTF_8);
	}
	
	/**
	 * Create an instance with given charset.
	 * @param charset charset to be used for conversion between byte[] and string.
	 */
	public StringCodec(Charset charset) {
		this.charset = charset;
	}
	
	@Override
	public byte[] encode(String t) {
		return t.getBytes(this.charset);
	}

	@Override
	public String decode(byte[] byteArr) {
		return new String(byteArr, this.charset);
	}

}
