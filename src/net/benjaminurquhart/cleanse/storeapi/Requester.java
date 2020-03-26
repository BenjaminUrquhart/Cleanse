package net.benjaminurquhart.cleanse.storeapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import net.benjaminurquhart.cleanse.Cleanse;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

public class Requester {
	
	public static final String USERAGENT = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:74.0) Gecko/20100101 Firefox/74.0";
	
	private static final Cache<String, String> CACHE = new Cache<>(3600);
	private static final String[] CURL_ARGS = {
			"curl",
			"URL",
			"-H",
			"User-Agent: "+USERAGENT,
			"-H",
			"Host: ",
			"-H",
			"Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
			"-H",
			"Connection: keep-alive",
			"-H",
			"Accept-Language: en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7",
			"-H",
			"Upgrade-Insecure-Requests: 1",
			"-s",
			"--compressed",
			"-L",
			"-v"
	};
	
	private static OkHttpClient CLIENT;
	private static Map<String, Object> locks = new WeakHashMap<>();
	private static Set<String> HOSTS_THAT_NEED_CURL = new HashSet<>();
	
	static {
		//HOSTS_THAT_NEED_CURL.add("www.costco.com");
		try {
			CLIENT = new OkHttpClient.Builder()
					 .connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
					 .protocols(new ArrayList<>(EnumSet.of(Protocol.HTTP_1_1)))
					 .cookieJar(new CookieJarImpl())
					 .build();
		}
		catch(Exception e) {
			e.printStackTrace();
			CLIENT = new OkHttpClient();
		}
	}
	
	public static JSONObject requestJSONObject(String url) throws IOException {
		return new JSONObject(request(url));
	}
	public static JSONArray requestJSONArray(String url) throws IOException {
		return new JSONArray(request(url));
	}
	public static String request(String url) throws IOException {
		return request(url, Collections.emptyMap());
	}
	public static String request(String url, Map<String, List<String>> headers) throws IOException {
		String result = CACHE.get(url);
		if(result != null) {
			return result;
		}
		Object lock = locks.computeIfAbsent(url, k -> new Object());
		synchronized(lock) {
			result = CACHE.get(url);
			if(result != null) {
				return result;
			}
			Request.Builder builder = new Request.Builder().url(url).addHeader("User-Agent", USERAGENT);
			for(String key : headers.keySet()) {
				for(String value : headers.get(key)) {
					builder.addHeader(key, value);
				}
			}
			String host = HttpUrl.parse(url).host();
			try {
				if(HOSTS_THAT_NEED_CURL.contains(host)) {
					throw new SocketTimeoutException();
				}
				Response response = CLIENT.newCall(builder.build()).execute();
				result = response.body().string();
				if(response.code() >= 300) {
					System.err.println("[WARN] Got status code " + response.code() + " from url " + url);
					// No need for excessive 400s
					if(response.code() == 400) {
						CACHE.set(url, result);
					}
				}
				else {
					CACHE.set(url, result);
				}
			}
			catch(SocketTimeoutException e) {
				HOSTS_THAT_NEED_CURL.add(host);
				System.err.println("Failed to request via OkHttp. Defaulting to curl... This ignores the header parameter!");
				try {
					String[] args = Arrays.copyOf(CURL_ARGS, CURL_ARGS.length);
					args[1] = url;
					args[5]+=host;
					Cleanse.debug(Arrays.stream(args).map(s -> s.contains(" ") || s.contains("&") ? '"'+s+'"' : s).collect(Collectors.joining(" ")));
					Process process = new ProcessBuilder(args)
					.redirectErrorStream(true)
					.start();
					BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					result = reader.lines().collect(Collectors.joining("\n"));
					//System.err.println(result);
					if(!result.contains("<H1>Access Denied</H1>")) {
						CACHE.set(url, result);
					}
					Cleanse.debug(process.waitFor());
					reader.close();
				}
				catch(Exception exec) {
					throw new IOException(exec);
				}
			}
		}
		return result;
	}
}
