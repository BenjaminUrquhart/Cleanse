package net.benjaminurquhart.cleanse.storeapi.requests;

import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import net.benjaminurquhart.cleanse.storeapi.Cache;
import net.benjaminurquhart.cleanse.storeapi.InvalidZipException;
import net.benjaminurquhart.cleanse.storeapi.Request;
import net.benjaminurquhart.cleanse.storeapi.Requester;
import net.benjaminurquhart.cleanse.storeapi.Route;

public class TargetRequest extends Request {
	
	private Cache<String, List<Integer>> storesIDCache = new Cache<>(-1);
	private Cache<String, JSONObject> productCache = new Cache<>(3600);
	
	private Map<String, Object> locks = new WeakHashMap<>();
	
	private JSONObject getProductStatus(String tcin, int storeID) {
		String key = tcin+" "+storeID;
		JSONObject status = productCache.get(key);
		if(status != null) {
			return status;
		}
		Object lock = locks.computeIfAbsent(key, k -> new Object());
		synchronized(lock) {
			status = productCache.get(key);
			if(status != null) {
				return status;
			}
			try {
				JSONObject item = Requester.requestJSONObject(Route.TARGET_PRODUCT_INFO.format(tcin, storeID))
										   .getJSONObject("product")
										   .getJSONObject("item");
				if(!item.has("child_items")) {
					return null;
				}
				JSONArray subitems = item.getJSONArray("child_items"), locations;
				JSONObject child, location;
				for(int i = 0, size = subitems.length(); i < size; i++) {
					child = subitems.getJSONObject(i);
					locations = child.getJSONObject("available_to_promise_store")
									 .getJSONArray("products")
									 .getJSONObject(0)
									 .getJSONArray("locations");
					for(int j = 0, length = locations.length(); j < length; j++) {
						location = locations.getJSONObject(j);
						status = new JSONObject();
						status.put("id", Integer.parseInt(location.getString("location_id")));
						status.put("name", location.getString("store_name"));
						status.put("address", location.getString("formatted_store_address"));
						status.put("phone", location.optString("store_main_phone"));
						status.put("on_hand", Math.max(0, location.getInt("onhand_quantity")));
						status.put("status", location.getString("availability_status"));
						productCache.set(tcin+" "+location.getString("location_id"), status);
					}
				}
				return productCache.get(key);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	private List<Integer> getNearbyStoreIDsByZip(String zip) {
		List<Integer> stores = storesIDCache.get(zip);
		if(stores != null) {
			return stores;
		}
		Object lock = locks.computeIfAbsent(zip, z -> new Object());
		synchronized(lock) {
			stores = storesIDCache.get(zip);
			if(stores != null) {
				return stores;
			}
			try {
				JSONObject result = Requester.requestJSONArray(Route.TARGET_NEARBY_STORES.format(zip, 20, 20, "mile")).getJSONObject(0);
				if(!result.has("locations")) {
					throw new InvalidZipException(zip);
				}
				JSONArray locations = result.getJSONArray("locations");
				stores = new ArrayList<>();
				for(int i = 0, size = locations.length(); i < size; i++) {
					stores.add(locations.getJSONObject(i).getInt("location_id"));
				}
				storesIDCache.set(zip, stores);
				return stores;
			}
			catch(InvalidZipException e) {
				throw e;
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	@Override
	public JSONArray getStatus(String zip, String product, boolean expand) {
		try {
			List<Integer> nearby = null;
			try {
				nearby = this.getNearbyStoreIDsByZip(zip);
			}
			catch(InvalidZipException e) {
				return new JSONArray().put(new JSONObject().put("error", "Unknown zip code: "+zip));
			}
			JSONArray results = Requester.requestJSONObject(
					Route.TARGET_SEARCH.format(
							nearby.get(0), 
							nearby.stream().map(String::valueOf).collect(Collectors.joining(",")), 
							product, 
							"/s/"+product)
					)
					.getJSONObject("search_response")
					.getJSONObject("items")
					.getJSONArray("Item");
			JSONObject status, item, price, tmp, store;
			JSONArray statuses = new JSONArray(), availability, tmp2;
			String tcin;
			for(int i = 0, size = results.length(); i < size; i++) {
				status = new JSONObject();
				availability = new JSONArray();
				item = results.getJSONObject(i);
				status.put("title", item.getString("title"));
				status.put("tcin", tcin = item.getString("tcin"));
				status.put("url", "https://target.com" + item.getString("url"));
				price = item.getJSONObject("price");
				if(price.getBoolean("is_current_price_range")) {
					status.put("price", new JSONObject().put("price_min", price.optDouble("current_retail_min", -1))
														.put("price_max", price.optDouble("current_retail_max", -1))
														.put("formatted", price.optString("formatted_current_price", "N/A")));
				}
				else {
					status.put("price", new JSONObject().put("price_min", price.optDouble("current_retail", -1))
														.put("price_max", price.optDouble("current_retail", -1))
														.put("formatted", price.optString("formatted_current_price", "N/A")));
				}
				if(expand) {
					for(int id : nearby) {
						tmp = this.getProductStatus(tcin, id);
						if(tmp != null && !tmp.getString("status").equals("NOT_SOLD_IN_STORE")) {
							availability.put(tmp);
						}
					}
				}
				else {
					try {
						tmp2 = Requester.requestJSONObject(Route.TARGET_AGGREGATE.format(tcin, zip, 20, 20)).getJSONArray("products").getJSONObject(0).getJSONArray("locations");
						for(int j = 0, length = tmp2.length(); j < length; j++) {
							store = new JSONObject();
							tmp = tmp2.getJSONObject(j);
							if(tmp.getJSONObject("in_store_only").getString("availability_status").equals("NOT_SOLD_IN_STORE")) {
								continue;
							}
							store.put("id", Integer.parseInt(tmp.getString("location_id")));
							store.put("name", tmp.getString("store_name"));
							store.put("address", tmp.getString("store_address"));
							store.put("on_hand", Math.max(0, tmp.getInt("location_available_to_promise_quantity")));
							store.put("status", tmp.getJSONObject("in_store_only").getString("availability_status"));
							availability.put(store);
						}
					}
					catch(Exception e) {
						tmp = this.getProductStatus(tcin, nearby.get(0));
						if(tmp != null && !tmp.getString("status").equals("NOT_SOLD_IN_STORE")) {
							availability.put(tmp);
						}
					}
				}
				status.put("availability", availability);
				statuses.put(status);
			}
			return statuses;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
