package net.benjaminurquhart.cleanse.storeapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class Cache<K, V> {
	
	public static final int DEFAULT_CACHE_EXPIRE_SECONDS = 60;
	
	private int expireTimeSeconds;

	private Map<K, V> cache = Collections.synchronizedMap(new WeakHashMap<>());
	private Map<K, Long> expireTimes = Collections.synchronizedMap(new HashMap<>());
	
	public Cache() {
		this(DEFAULT_CACHE_EXPIRE_SECONDS);
	}
	public Cache(int expireTimeSeconds) {
		this.expireTimeSeconds = expireTimeSeconds;
	}
	
	public V get(K key) {
		if(cache.containsKey(key)) {
			if(expireTimes.get(key) >= System.currentTimeMillis()) {
				System.err.println("Cache status for " + key + " -> hit");
				return cache.get(key);
			}
			else {
				System.err.println("Cache status for " + key + " -> expired");
			}
		}
		else {
			System.err.println("Cache status for " + key + " -> miss");
		}
		expireTimes.remove(key);
		cache.remove(key);
		return null;
	}
	public void set(K key, V value) {
		System.err.println("Cached item " + key + (expireTimeSeconds < 1 ? "" : " for " + expireTimeSeconds + " seconds"));
		expireTimes.put(key, expireTimeSeconds < 1 ? Long.MAX_VALUE : (System.currentTimeMillis()+expireTimeSeconds*1000));
		cache.put(key, value);
	}
}
