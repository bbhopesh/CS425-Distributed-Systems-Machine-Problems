package edu.illinois.uiuc.sp17.cs425.team4.component;

public interface Codec<T> {
	
	public byte[] encode(T t);
	
	public T decode(byte[] bytes);
}
