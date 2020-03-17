package net.benjaminurquhart.cleanse;

import net.benjaminurquhart.stdout.STDIOPlus;

public class Cleanse {

	public static void main(String[] args) throws Exception {
		STDIOPlus.enable();
		new Server(8888);
	}
}
