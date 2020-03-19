package net.benjaminurquhart.cleanse.storeapi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public enum Route {
	
	TARGET_NEARBY_STORES("https://redsky.target.com/v3/stores/nearby/%s?within=%s&limit=%s&unit=%s", APIKey.TARGET),
	TARGET_SEARCH("https://redsky.target.com/v2/plp/search/?pricing_store_id=%s&store_ids=%s&keyword=%s&pageId=%s&channel=web&count=24&default_purchasability_filter=true&facet_recovery=false&isDLP=false&offset=0&pageId=none&kwr=y", APIKey.TARGET),
	TARGET_PRODUCT_INFO("https://redsky.target.com/v3/pdp/tcin/%s?storeId=%s", APIKey.TARGET),
	TARGET_AGGREGATE("https://api.target.com/fulfillment_aggregator/v1/fiats/%s?nearby=%s&radius=%s&limit=%s&requested_quantity=1", APIKey.TARGET),
	
	COSTCO_SEARCH("https://www.costco.com/CatalogSearch?dept=All&keyword=%s"),
	COSTCO_PRODUCT_INFO("https://www.costco.com/AjaxGetContractPrice?productId=%s&WH=%s");
	
	private final String path;
	
	private Route(String path) {
		this(path, null);
	}
	private Route(String path, APIKey key) {
		if(key != null) {
			path+="&key="+key;
		}
		this.path = path;
	}
	public String format(Object...objects) {
		String[] formatted = new String[objects.length];
		for(int i = 0; i < objects.length; i++) {
			try {
				formatted[i] = URLEncoder.encode(String.valueOf(objects[i]), Charset.defaultCharset().displayName());
			} 
			catch (UnsupportedEncodingException e) {
				formatted[i] = String.valueOf(objects[i]);
			}
		}
		return String.format(path, (Object[])formatted);
	}
	@Override
	public String toString() {
		return path;
	}
}
