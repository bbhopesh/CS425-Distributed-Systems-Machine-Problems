package edu.illinois.uiuc.sp17.cs425.team4.component;

/**
 * Interface to define general contract of a codec to convert to/from byte arrays.
 * @author bbassi2
 * @param <T> Convert to/from type T.
 */
public interface Codec<T> {
	/**
	 * Encode t to byte[]
	 * @param t value to be encoded.
	 * @return encoded value.
	 */
	public byte[] encode(T t);
	
	/**
	 * Decode byte[].
	 * @param byteArr byte[] to be decoded. 
	 * @return decoded type.
	 */
	public T decode(byte[] byteArr);
}
