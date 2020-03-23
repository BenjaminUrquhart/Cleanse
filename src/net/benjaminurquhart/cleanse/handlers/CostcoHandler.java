package net.benjaminurquhart.cleanse.handlers;

import net.benjaminurquhart.cleanse.storeapi.requests.CostcoRequest;

@Parameters(Parameter.QUERY)
@Route("/api/costco")
public class CostcoHandler extends Handler {
	
	private static final CostcoRequest COSTCO = new CostcoRequest();

	public CostcoHandler() {
		super(COSTCO, false);
	}
}
