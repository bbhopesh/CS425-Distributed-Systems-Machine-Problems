package edu.illinois.uiuc.sp17.cs425.team4.component;

public interface TransactionManager<K, V> {

	public V get(K key);
	
	public void set(K key, V value);
	
	public boolean commit();
	
	public boolean abort();
}
