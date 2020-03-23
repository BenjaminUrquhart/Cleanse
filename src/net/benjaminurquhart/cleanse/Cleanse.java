package net.benjaminurquhart.cleanse;

import net.benjaminurquhart.cleanse.storeapi.Requester;

public class Cleanse {
	
	private static final boolean DEBUG;
	
	static {
		String pref = System.getenv("debug");
		if(pref != null && pref.matches("(?i)true|false")) {
			DEBUG = Boolean.parseBoolean(pref);
		}
		else {
			DEBUG = false;
		}
	}

	public static void main(String[] args) throws Exception {
		Server server = Server.getInstance();
		for(String endpoint : server.getEndpoints().keySet()) {
			Requester.request("http://localhost:"+server.getPort()+endpoint);
		}
		System.out.println("Ready");
	}
	
	public static void debug(Object obj) {
		if(DEBUG) System.err.println(obj);
	}
}
