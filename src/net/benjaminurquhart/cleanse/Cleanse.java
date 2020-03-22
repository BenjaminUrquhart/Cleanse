package net.benjaminurquhart.cleanse;

public class Cleanse {
	
	private static final boolean DEBUG;
	
	static {
		String pref = System.getenv("debug");
		if(pref != null && pref.matches("(?i)true|false")) {
			DEBUG = Boolean.parseBoolean(pref);
		}
		else {
			DEBUG = true;
		}
	}

	public static void main(String[] args) throws Exception {
		Server.getInstance();
	}
	
	public static void debug(Object obj) {
		if(DEBUG) System.err.println(obj);
	}
}
