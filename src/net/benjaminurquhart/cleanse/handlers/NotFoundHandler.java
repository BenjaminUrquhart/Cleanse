package net.benjaminurquhart.cleanse.handlers;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD.GeneralHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;
import net.benjaminurquhart.cleanse.Server;

public class NotFoundHandler extends GeneralHandler {
	@Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		String ip = session.getHeaders().get("x-forwarded-for");
		if(ip == null) {
			ip = session.getRemoteIpAddress();
		}
		System.out.println("Received request from " + ip + " with invalid route: " + uriResource);
		return RouterNanoHTTPD.newFixedLengthResponse(
				Response.Status.NOT_FOUND, 
				"application/json", 
				new JSONObject().put("endpoints", new JSONArray(Server.getInstance().getRoutes())).toString()
		);
	}
}
