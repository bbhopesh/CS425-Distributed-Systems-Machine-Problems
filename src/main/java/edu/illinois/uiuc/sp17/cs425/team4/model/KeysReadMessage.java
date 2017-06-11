package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.Set;

/**
 * Message asking to read values for the given keys.
 * @author bbassi2.
 *
 * @param <K> Key type.
 */
public interface KeysReadMessage<K> extends Message {
	/**
	 * Get keys for which values have to be read.
	 * @return Set of keys for which valus are to be read.
	 */
	public Set<K> readKeys();

}
