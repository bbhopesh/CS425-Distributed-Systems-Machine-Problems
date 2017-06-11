package edu.illinois.uiuc.sp17.cs425.team4.component;

/**
 * A hash function interface.
 * 
 * @author bbassi2
 */
public interface HashFunction {
	/**
	 * Calculate hash of the given byte array.
	 * @param obj byte array whose hash has to be calculated.
	 * @return hash value.
	 */
	public byte[] hash(byte[] obj);
}
