package edu.illinois.uiuc.sp17.cs425.team4.component;

/**
 * An interface to encode and decode java objects to and from byte arrays.
 * 
 * Different implemenations can perform encoding and decoding in different ways.
 * @author bbassi2
 * 
 * @param <T>
 */
public interface Codec<T> {
	/**
	 * Encode.
	 * @param t Java object.
	 * @return encoded byte array.
	 */
	public byte[] encode(T t);
	
	/**
	 * Decode.
	 * @param bytes byte array to be decoded.
	 * @return Decoded java object.
	 */
	public T decode(byte[] bytes);
}
