package net.benjaminurquhart.cleanse.storeapi;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Requester {

	private static final OkHttpClient CLIENT = new OkHttpClient();
	private static final Cache<String, String> CACHE = new Cache<>(600);
	
	public static JSONObject requestJSONObject(String url) throws IOException {
		return new JSONObject(request(url));
	}
	public static JSONArray requestJSONArray(String url) throws IOException {
		return new JSONArray(request(url));
	}
	public static String request(String url) throws IOException {
		String result = CACHE.get(url);
		if(result != null) {
			return result;
		}
		synchronized(CACHE) {
			result = CACHE.get(url);
			if(result != null) {
				return result;
			}
			Response response = CLIENT.newCall(new Request.Builder().url(url).build()).execute();
			result = response.body().string();
			if(response.code() >= 300) {
				System.err.println("[WARN] Got status code " + response.code() + " from url " + url);
				System.err.println("[WARN] Data: " + result);
			}
			else {
				CACHE.set(url, result);
			}
		}
		return result;
	}
}
