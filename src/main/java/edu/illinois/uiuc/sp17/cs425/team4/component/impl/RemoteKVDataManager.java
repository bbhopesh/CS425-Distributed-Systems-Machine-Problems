package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataManager;

public class RemoteKVDataManager<K,V> implements KVDataManager<K, V> {

	@Override
	public Pair<Long, V> read(K key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(K key, V value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Pair<Long, V> read(K key, long asOfTimestamp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(K key, V value, long timestamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(K key) {
		// TODO Auto-generated method stub
		
	}

}
