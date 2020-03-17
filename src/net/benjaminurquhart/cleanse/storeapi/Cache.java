package net.benjaminurquhart.cleanse.storeapi;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.json.JSONObject;

public class Cache {
	
	public static final int CACHE_EXPIRE_SECONDS = 60;

	private Map<String, JSONObject> cache = new WeakHashMap<>();
	private Map<String, Long> expireTimes = new HashMap<>();
	
	public JSONObject get(String key) {
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
	public void set(String key, JSONObject value) {
		System.err.println("Cached URL " + key + " for " + CACHE_EXPIRE_SECONDS + " seconds");
		expireTimes.put(key, System.currentTimeMillis()+CACHE_EXPIRE_SECONDS*1000);
		cache.put(key, value);
	}
}
