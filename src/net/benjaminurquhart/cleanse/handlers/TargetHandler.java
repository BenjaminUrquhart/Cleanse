package net.benjaminurquhart.cleanse.handlers;

import net.benjaminurquhart.cleanse.storeapi.requests.TargetRequest;

@Route("/target")
public class TargetHandler extends Handler {
	
	private static final TargetRequest TARGET = new TargetRequest();
	
	public TargetHandler() {
		super(TARGET);
	}
}
