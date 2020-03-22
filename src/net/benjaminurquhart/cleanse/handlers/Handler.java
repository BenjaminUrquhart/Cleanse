package net.benjaminurquhart.cleanse.handlers;

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
	}

	@Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		try {
			String ip = session.getHeaders().get("x-forwarded-for");
			if(ip == null) {
				ip = session.getRemoteIpAddress();
			}
			System.out.println("Received request from " + ip);
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
			else {
				try {
					ipInfo = Request.geoIP(ip);
					if(ipInfo != null) {
						zip = ipInfo.getPostalCode();
					}
					if(requireZip && (zip == null || zip.isEmpty())) {
						return NanoHTTPD.newFixedLengthResponse(
								NanoHTTPD.Response.Status.BAD_REQUEST, 
								"application/json", 
								new JSONObject().put("error", "No zip code provided and GeoIP failed to provide a location").toString()
						);
					}
				}
				catch(Exception e) {
					e.printStackTrace();
					if(requireZip) {
						return NanoHTTPD.newFixedLengthResponse(
								NanoHTTPD.Response.Status.BAD_REQUEST, 
								"application/json", 
								new JSONObject().put("error", "No zip code provided and GeoIP failed to provide a location").toString()
						);
					}
				}
			}
			String query = params.containsKey("q") ? String.join("+", params.get("q")) : "toilet+paper";
			JSONArray results = REQUEST.getStatus(zip, query, params.containsKey("expand") && params.get("expand").contains("true"));
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
			Response response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", out.toString());
			System.out.println("Done");
			return response;
		}
		catch(Exception e) {
			e.printStackTrace();
			return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/html", e.toString());
		}
	}
}
