package net.benjaminurquhart.cleanse.storeapi;

import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class CookieJarImpl implements CookieJar {
	
	private Cache<String, List<Cookie>> cookieCache = new Cache<>();
	
	@Override
	public List<Cookie> loadForRequest(HttpUrl url) {
		List<Cookie> out = cookieCache.get(url.host());
		return out == null ? Collections.emptyList() : out;
	}

	@Override
	public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
		cookieCache.set(url.host(), cookies);
	}

}
