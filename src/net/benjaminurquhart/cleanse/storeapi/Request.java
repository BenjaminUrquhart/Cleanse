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
					ksoft = new KSoftAPI(new JSONObject(Files.lines(new File("api_keys.json").toPath()).collect(Collectors.joining())).getString("ksoft"));
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

	public abstract JSONArray getToiletPaperStatus(String zip);
	public abstract JSONArray getHandSanitizerStatus(String zip);
}
