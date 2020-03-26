package net.benjaminurquhart.cleanse.storeapi.requests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import net.benjaminurquhart.cleanse.storeapi.Request;
import net.benjaminurquhart.cleanse.storeapi.Requester;
import net.benjaminurquhart.cleanse.storeapi.Route;

public class CostcoRequest extends Request {
	
	/*
	 * Set-Cookie: invCheckPostalCode=91104; Expires=Fri, 17-Feb-23 12:00:01 GMT; Path=/
	 * Set-Cookie: invCheckStateCode=CA; Expires=Fri, 17-Feb-23 12:00:01 GMT; Path=/
	 * Set-Cookie: WAREHOUSEDELIVERY_WHS=National%3D00847%2CRegional%3D283-bd; Path=/
	 */
	
	private static final Pattern 
		PRODUCT_PART_NUMBER = Pattern.compile("<!-- PartNumberBC = (\\d+) and productInventory\\.ffmcenterid = (\\d+) -->"),
		PRODUCT_INFORMATION = Pattern.compile("<!-- InStock = (\\d+) and ProductSKU = (\\d+) and IsPurchasable = (\\d+) and depotCatalogEnableFlag = (true|false) -->"),
		PRODUCT_DELIVERY    = Pattern.compile("<!-- standardData\\.ffmCenterId = (\\d+) and twoDayData\\.ffmCenterId = (\\d+) and userData\\.programTypes = (.+) -->");
	
	@SuppressWarnings("serial")
	private static final Map<String, List<String>> HEADERS = new HashMap<String, List<String>>() {{
		put("Accept", Arrays.asList("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
		put("Connection", Arrays.asList("keep-alive"));
	}};

	@Override
	public JSONArray getStatus(String zip, String product, boolean expand) {
		try {
			String html = Requester.request(Route.COSTCO_SEARCH.format(product).replace("%2B", "+"), HEADERS);
			Document document = Jsoup.parse(html);
			Elements elements = document.getElementsByAttributeValue("class", "product-list grid");
			JSONArray out = new JSONArray(), tmp;
			if(elements.isEmpty()) {
				return out;
			}
			Element results = elements.get(0);
			//System.err.println(results);
			List<String> comments = this.getComments(results);
			List<JSONObject> products = results.getElementsByAttributeValueMatching("class", "col\\-xs\\-\\d+ col\\-md\\-\\d+ col\\-lg\\-\\d+ col\\-xl\\-\\d+ product")
											   .stream()
											   .map(element -> {
												   JSONObject productInfo = element.getElementsByAttributeValue("class", "description")
														   						   .stream()
														   						   .map(Element::children)
														   						   .flatMap(List::stream)
														   						   .map(e -> new JSONObject().put("title", e.text()).put("url", e.attr("href")))
														   						   .findFirst()
														   						   .orElse(new JSONObject());
												   JSONObject price = new JSONObject();
												   JSONArray images = new JSONArray();
												   element.getElementsByAttributeValue("class", "img-responsive").forEach(e -> images.put(e.attr(e.hasAttr("src") ? "src" : "data-src")));
												   price.put("formatted", element.getElementsByAttributeValue("class", "price").stream().map(Element::text).findFirst().orElse("Unknown"));
												   
												   return productInfo.put("price", price).put("images", images);
											   }).collect(Collectors.toList());
											   
			
			Matcher partNumber, information, delivery;
			int index = 0;
			JSONObject info = null;
			for(String comment : comments) {
				if(comment.equals("<!-- BEGIN - CostcoGLOBALSAS/Widgets/ProductTile/ProductTileSearchResults.jspf -->")) {
					info = products.get(index++);
					continue;
				}
				else if(comment.equals("<!-- END - CostcoGLOBALSAS/Widgets/ProductTile/ProductTileSearchResults.jspf -->")) {
					if(info.has("stock")) {
						info.put("status", info.getInt("stock") == 0 ? "OUT_OF_STOCK" : "IN_STOCK");
					}
					out.put(info);
					continue;
				}
				partNumber = PRODUCT_PART_NUMBER.matcher(comment);
				information = PRODUCT_INFORMATION.matcher(comment);
				delivery = PRODUCT_DELIVERY.matcher(comment);
				if(delivery.find()) {
					info.put("shipping", new JSONObject().put("standard_ffmcenter", Integer.parseInt(delivery.group(1)))
														 .put("two_day_ffmcenter", Integer.parseInt(delivery.group(2)))
														 .put("shipping_types", delivery.group(3).startsWith("[") ? new JSONArray(delivery.group(3)) : delivery.group(3)));
				}
				else if(partNumber.find()) {
					tmp = info.has("parts") ? info.getJSONArray("parts") : new JSONArray();
					tmp.put(new JSONObject().put("part_number", Integer.parseInt(partNumber.group(1)))
											.put("ffmcenter", Integer.parseInt(partNumber.group(2))));
					info.put("parts", tmp);
				}
				else if(information.find()) {
					info.put("stock", Integer.parseInt(information.group(1)))
						.put("sku", Integer.parseInt(information.group(2)))
						.put("purchasable", information.group(3).equals("1"))
						.put("catalog_enabled", Boolean.parseBoolean(information.group(4)));
				}
			}
			return out;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private List<String> getComments(Node node) {
		List<String> out = new ArrayList<>();
		if(node.nodeName().equals("#comment")) {
			out.add(node.toString());
		}
		else {
			for(Node n : node.childNodes()) {
				out.addAll(this.getComments(n));
			}
		}
		return out;
	}
}