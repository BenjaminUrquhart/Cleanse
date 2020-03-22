package net.benjaminurquhart.cleanse.handlers;

public enum Parameter {
	EXPAND(boolean.class),
	QUERY("q", String.class),
	ZIP(int.class);
	
	private final String name;
	private final Class<?> type;
	
	private Parameter(Class<?> type) {
		this(null, type);
	}
	private Parameter(String name, Class<?> type) {
		this.name = name == null ? this.name().toLowerCase() : name;
		this.type = type;
	}
	public Class<?> getType() {
		return type;
	}
	@Override
	public String toString() {
		return name;
	}
}
