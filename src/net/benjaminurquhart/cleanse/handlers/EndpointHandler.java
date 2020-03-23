package net.benjaminurquhart.cleanse.handlers;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD.DefaultHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.GeneralHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;
import net.benjaminurquhart.cleanse.Server;

@Route("/api/endpoints")
public class EndpointHandler extends GeneralHandler {
	
	@Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		try {
			String ip = session.getRemoteIpAddress();
			if(Handler.isPrivateIP(ip)) {
				String tmp = session.getHeaders().get("x-forwarded-for");
				if(tmp != null) {
					ip = tmp;
				}
			}
			System.out.println("Received request from " + ip + ": " + uriResource);
			Map<String, Class<? extends DefaultHandler>> handlers = Server.getInstance().getEndpoints();
			JSONArray endpoints = new JSONArray(), paramsJSON;
			JSONObject out = new JSONObject(), info;
			Class<? extends DefaultHandler> clazz;
			Parameters params;
			for(String route : handlers.keySet()) {
				paramsJSON = new JSONArray();
				info = new JSONObject();
				clazz = handlers.get(route);
				info.put("route", route);
				params = clazz.getAnnotation(Parameters.class);
				if(params != null) {
					for(Parameter parameter : params.value()) {
						paramsJSON.put(new JSONObject().put("name", parameter.toString())
													   .put("type", parameter.getType().getSimpleName()));
					}
					info.put("parameters", paramsJSON);
					endpoints.put(info);
				}
			}
			out.put("warning", "The /api/target/ endpoint can take a VERY long time to return results. This is due to the internal structure of the Target API and cannot be avoided.");
			out.put("endpoints", endpoints);
			return NanoHTTPD.newFixedLengthResponse(
					NanoHTTPD.Response.Status.OK, 
					"application/json", 
					out.toString()
			);
		}
		catch(Throwable e) {
			e.printStackTrace();
			return NanoHTTPD.newFixedLengthResponse(
					NanoHTTPD.Response.Status.INTERNAL_ERROR, 
					"application/json", 
					new JSONObject().put("error", e.toString()).toString()
			);
		}
	}
}
