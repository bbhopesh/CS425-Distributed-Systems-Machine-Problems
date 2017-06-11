package edu.illinois.uiuc.sp17.cs425.team4.model;

/**
 * A message asking to delete a key.
 * @author bbassi2.
 *
 * @param <K> Key type.
 */
public interface KeyDeleteMessage<K> extends Message {
	/**
	 * Get key to be deleted.
	 * @return key to be deleted.
	 */
	public K getKey();
}
