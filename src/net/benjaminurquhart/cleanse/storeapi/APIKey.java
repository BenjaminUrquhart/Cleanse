package net.benjaminurquhart.cleanse.storeapi;

public enum APIKey {

	// Costco geolocation API key
	VIRTUAL_EARTH("Ao5ZqVFnjiL2fCfkgFfGZ8JpRX-ksJ0wbIckQ6HnVV0i9cG95H3Wh3lfKRaJE1wg"),
	// Target's RedSky key is publicly available. Check the networking tab of a dev console on their site.
	TARGET("eb2551e4accc14f38cc42d32fbc2b2ea");
	
	private final String key;
	
	private APIKey(String key) {
		this.key = key;
	}
	@Override
	public String toString() {
		return key;
	}
}
