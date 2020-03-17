package net.benjaminurquhart.cleanse.storeapi;

public enum Route {
	
	TARGET_NEARBY_STORES("https://redsky.target.com/v3/stores/nearby/%s?within=%d&limit=%d&unit=%s");

	private final String path;
	
	private Route(String path) {
		this.path = path;
	}
	public String getPath() {
		return path;
	}
	@Override
	public String toString() {
		return this.name()+" ("+path+")";
	}
}
