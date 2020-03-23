package net.benjaminurquhart.cleanse.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD.GeneralHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;
import net.benjaminurquhart.cleanse.storeapi.Request;
import net.explodingbush.ksoftapi.entities.IP;

public abstract class Handler extends GeneralHandler {
	
	private static final Map<String, Handler> handlers = new HashMap<>();
	
	private final Request REQUEST;
	private final boolean requireZip;
	
	public Handler(Request request) {
		this(request, true);
	}
	public Handler(Request request, boolean requireZip) {
		if(request == null) {
			throw new IllegalArgumentException("null");
		}
		this.requireZip = requireZip;
		this.REQUEST = request;
		
		Route route = this.getClass().getAnnotation(Route.class);
		if(route != null) {
			handlers.put(route.value(), this);
		}
	}
	public static Handler getHandlerFor(Route route) {
		return getHandlerFor(route.value());
	}
	public static Handler getHandlerFor(String route) {
		return handlers.get(route);
	}
	public static boolean isPrivateIP(String ip) {
		return ip.equals("127.0.0.1") || ip.startsWith("192.168.") || ip.startsWith("172.16.") || ip.startsWith("10.");
	}

	public Request getRequest() {
		return REQUEST;
	}
	
	@Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		try {
			String ip = session.getRemoteIpAddress();
			if(isPrivateIP(ip)) {
				String tmp = session.getHeaders().get("x-forwarded-for");
				if(tmp != null) {
					ip = tmp;
				}
			}
			System.out.println("Received request from " + ip + ": " + uriResource);
			Map<String, List<String>> params = session.getParameters();
			String zip = null;
			IP ipInfo = null;
			if(params.containsKey("zip") && !params.get("zip").isEmpty()) {
				zip = params.get("zip").get(0);
				if(!zip.matches("\\d{5}")) {
					return NanoHTTPD.newFixedLengthResponse(
							NanoHTTPD.Response.Status.BAD_REQUEST, 
							"application/json", 
							new JSONObject().put("error", "Invalid zip code: "+zip).toString()
					);
				}
			}
			else if(requireZip) {
				if(isPrivateIP(ip)) {
					return NanoHTTPD.newFixedLengthResponse(
							NanoHTTPD.Response.Status.BAD_REQUEST, 
							"application/json", 
							new JSONObject().put("error", "No zip code provided. GeoIP does not work for private IPs for obvious reasons").toString()
					);
				}
				try {
					ipInfo = Request.geoIP(ip);
					if(ipInfo != null) {
						zip = ipInfo.getPostalCode();
					}
					if(zip == null || zip.isEmpty()) {
						return NanoHTTPD.newFixedLengthResponse(
								NanoHTTPD.Response.Status.BAD_REQUEST, 
								"application/json", 
								new JSONObject().put("error", "No zip code provided and GeoIP failed to provide a location").toString()
						);
					}
				}
				catch(Exception e) {
					e.printStackTrace();
					return NanoHTTPD.newFixedLengthResponse(
							NanoHTTPD.Response.Status.BAD_REQUEST, 
							"application/json", 
							new JSONObject().put("error", "No zip code provided and GeoIP failed to provide a location").toString()
					);
				}
			}
			String query = params.containsKey("q") ? String.join("+", params.get("q")) : "toilet+paper";
			JSONArray results = REQUEST.getStatus(zip, query, params.containsKey("expand") && params.get("expand").get(0).equalsIgnoreCase("true"));
			JSONObject out = new JSONObject();
			if(ipInfo != null) {
				out.put("geoip", new JSONObject().put("zip_code", ipInfo.getPostalCode())
												 .put("city", ipInfo.getCity())
												 .put("region", ipInfo.getRegion())
												 .put("latitude", ipInfo.getLatitude())
												 .put("longitude", ipInfo.getLongitude())
												 .put("powered_by", "https://api.ksoft.si"));
			}
			if(results == null) {
				out.put("error", "Internal Server Error");
				return NanoHTTPD.newFixedLengthResponse(
						NanoHTTPD.Response.Status.INTERNAL_ERROR, 
						"application/json", 
						out.toString()
				);
			}
			else {
				out.put("results", results);
			}
			return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", out.toString());
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
