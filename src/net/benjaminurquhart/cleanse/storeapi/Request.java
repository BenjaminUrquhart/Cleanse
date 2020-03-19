package net.benjaminurquhart.cleanse.storeapi;

import java.io.File;
import java.nio.file.Files;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import net.explodingbush.ksoftapi.KSoftAPI;
import net.explodingbush.ksoftapi.entities.IP;

public abstract class Request {
	
	private static KSoftAPI ksoft;
	private static Cache<String, IP> locationCache = new Cache<>(-1);
	
	public static IP geoIP(String ip) {
		IP ipInfo = locationCache.get(ip);
		if(ipInfo != null) {
			return ipInfo;
		}
		synchronized(locationCache) {
			ipInfo = locationCache.get(ip);
			if(ipInfo != null) {
				return ipInfo;
			}
			if(ksoft == null) {
				try {
					String token = System.getenv("ksoft");
					if(token == null) {
						token = new JSONObject(Files.lines(new File("api_keys.json").toPath()).collect(Collectors.joining())).getString("ksoft");
					}
					ksoft = new KSoftAPI(token);
				}
				catch(Exception e) {
					e.printStackTrace();
					return null;
				}
			}
			try {
				ipInfo = ksoft.getKumo().getIPAction().setIP(ip).execute();
				locationCache.set(ip, ipInfo);
				return ipInfo;
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public JSONArray getToiletPaperStatus(String zip) {
		return this.getToiletPaperStatus(zip, false);
	}
	public JSONArray getHandSanitizerStatus(String zip) {
		return this.getHandSanitizerStatus(zip, false);
	}
	public JSONArray getToiletPaperStatus(String zip, boolean expand) {
		return this.getStatus(zip, "toilet+paper", expand);
	}
	public JSONArray getHandSanitizerStatus(String zip, boolean expand) {
		return this.getStatus(zip, "hand+sanitizer", expand);
	}
	public abstract JSONArray getStatus(String zip, String product, boolean expand);
}
