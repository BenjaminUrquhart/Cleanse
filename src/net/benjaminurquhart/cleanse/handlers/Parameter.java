package net.benjaminurquhart.cleanse.handlers;

import java.net.URLEncoder;
import java.nio.charset.Charset;

public enum Parameter {
	EXPAND(boolean.class, false),
	QUERY("q", String.class, "toilet paper"),
	ZIP(int.class, 54476);
	
	private final String name;
	private final Class<?> type;
	private final Object example;
	
	private Parameter(Class<?> type, Object example) {
		this(null, type, example);
	}
	private Parameter(String name, Class<?> type, Object example) {
		this.name = name == null ? this.name().toLowerCase() : name;
		this.type = type;
		
		this.example = example;
	}
	public Class<?> getType() {
		return type;
	}
	public Object getExampleValue() {
		return example;
	}
	public String getExampleValueEncoded() {
		try {
			return URLEncoder.encode(String.valueOf(example), Charset.defaultCharset().displayName());
		}
		catch(Exception e) {
			return String.valueOf(example);
		}
	}
	@Override
	public String toString() {
		return name;
	}
}
