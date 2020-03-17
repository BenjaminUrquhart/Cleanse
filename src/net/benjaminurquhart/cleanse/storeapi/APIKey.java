package net.benjaminurquhart.cleanse.storeapi;

public enum APIKey {

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
