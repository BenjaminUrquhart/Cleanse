package net.benjaminurquhart.cleanse.handlers;

import java.util.Map;

import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD.GeneralHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;

public class NotFoundHandler extends GeneralHandler {
	@Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		String ip = session.getRemoteIpAddress();
		if(Handler.isPrivateIP(ip)) {
			String tmp = session.getHeaders().get("x-forwarded-for");
			if(tmp != null) {
				ip = tmp;
			}
		}
		System.out.println("Received request from " + ip + " with invalid route: " + uriResource);
		return RouterNanoHTTPD.newFixedLengthResponse(
				Response.Status.NOT_FOUND, 
				"application/json", 
				new JSONObject().put("error", "Not Found").toString()
		);
	}
}
