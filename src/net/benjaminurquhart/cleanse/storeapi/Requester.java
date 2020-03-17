package net.benjaminurquhart.cleanse.storeapi;

import java.io.IOException;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Requester {

	private static final OkHttpClient CLIENT = new OkHttpClient();
	private static final Cache CACHE = new Cache();
	
	public static JSONObject requestJSON(String url) throws IOException {
		JSONObject result = CACHE.get(url);
		if(result != null) {
			return result;
		}
		Response response = CLIENT.newCall(new Request.Builder().url(url).build()).execute();
		String resultStr = response.body().string();
		if(response.code() != 200) {
			System.err.println("[WARN] Got status code " + response.code() + " from url " + url);
			System.err.println("[WARN] Data: " + resultStr);
		}
		result = new JSONObject(resultStr);
		CACHE.set(url, result);
		return result;
	}
}
