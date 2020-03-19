package net.benjaminurquhart.cleanse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;
import net.benjaminurquhart.cleanse.storeapi.Request;
import net.benjaminurquhart.cleanse.storeapi.requests.TargetRequest;
import net.explodingbush.ksoftapi.entities.IP;

public class Server extends NanoHTTPD {
	
	private final TargetRequest TARGET = new TargetRequest();

	public Server(int port) throws IOException {
		super(port);
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}

	@Override
    public Response serve(IHTTPSession session) {
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
					return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "application/json", new JSONObject().put("error", "Invalid zip code: "+zip).toString());
				}
			}
			else {
				try {
					ipInfo = Request.geoIP(ip);
					zip = ipInfo.getPostalCode();
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
			JSONArray results = TARGET.getToiletPaperStatus(zip, params.containsKey("expand") && params.get("expand").contains("true"));
			JSONObject out = new JSONObject();
			if(ipInfo != null) {
				out.put("geoip", new JSONObject().put("zip_code", ipInfo.getPostalCode())
												 .put("city", ipInfo.getCity())
												 .put("region", ipInfo.getRegion())
												 .put("latitude", ipInfo.getLatitude())
												 .put("longitude", ipInfo.getLongitude())
												 .put("powered_by", "https://api.ksoft.si"));
			}
			out.put("results", results);
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
