package net.benjaminurquhart.cleanse.storeapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.benjaminurquhart.cleanse.Cleanse;

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
				Cleanse.debug("Cache status for " + key + " -> hit");
				return cache.get(key);
			}
			else {
				Cleanse.debug("Cache status for " + key + " -> expired");
			}
		}
		else {
			Cleanse.debug("Cache status for " + key + " -> miss");
		}
		expireTimes.remove(key);
		cache.remove(key);
		return null;
	}
	public void set(K key, V value) {
		Cleanse.debug("Cached item " + key + (expireTimeSeconds < 1 ? "" : " for " + expireTimeSeconds + " seconds"));
		if(expireTimeSeconds < 0) {
			expireTimes.put(key, Long.MAX_VALUE);
		}
		else {
			expireTimes.put(key, System.currentTimeMillis()+expireTimeSeconds*1000);
		}
		cache.put(key, value);
	}
}
