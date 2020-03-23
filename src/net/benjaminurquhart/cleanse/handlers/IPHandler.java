package net.benjaminurquhart.cleanse.handlers;

import java.util.Map;

import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD.GeneralHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;

import net.benjaminurquhart.cleanse.storeapi.Request;
import net.explodingbush.ksoftapi.entities.IP;

@Route("/internal/geoip")
public class IPHandler extends GeneralHandler {

	@Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		try {
			JSONObject out = new JSONObject();
			IP ipInfo = Request.geoIP(session.getRemoteIpAddress());
			if(ipInfo != null) {
				out.put("geoip", new JSONObject().put("zip_code", ipInfo.getPostalCode())
												 .put("city", ipInfo.getCity())
												 .put("region", ipInfo.getRegion())
												 .put("latitude", ipInfo.getLatitude())
												 .put("longitude", ipInfo.getLongitude())
												 .put("powered_by", "https://api.ksoft.si"));
			}
			return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", out.toString());
		}
		catch(Exception e) {
			return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "application/json", new JSONObject().put("error", e.toString()).toString());
		}
	}
}
