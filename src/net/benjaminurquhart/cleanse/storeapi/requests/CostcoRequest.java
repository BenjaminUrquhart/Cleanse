package net.benjaminurquhart.cleanse.storeapi.requests;

import org.json.JSONArray;

import net.benjaminurquhart.cleanse.storeapi.Request;

public class CostcoRequest extends Request {
	
	/*
	 * Set-Cookie: invCheckPostalCode=91104; Expires=Fri, 17-Feb-23 12:00:01 GMT; Path=/
	 * Set-Cookie: invCheckStateCode=CA; Expires=Fri, 17-Feb-23 12:00:01 GMT; Path=/
	 * Set-Cookie: WAREHOUSEDELIVERY_WHS=National%3D00847%2CRegional%3D283-bd; Path=/
	 */

	@Override
	public JSONArray getStatus(String zip, String product, boolean expand) {
		try {
			
		}
		catch(Exception e) {
			
		}
		return new JSONArray();
	}

}
