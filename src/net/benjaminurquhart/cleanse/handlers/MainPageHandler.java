package net.benjaminurquhart.cleanse.handlers;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD.GeneralHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;
import net.benjaminurquhart.cleanse.Server;

import static j2html.TagCreator.*;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Entities;

@Route("/")
public class MainPageHandler extends GeneralHandler {

	@Override
    public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		try {
			String ip = session.getRemoteIpAddress();
			if(Handler.isPrivateIP(ip)) {
				String tmp = session.getHeaders().get("x-forwarded-for");
				if(tmp != null) {
					ip = tmp;
				}
			}
			System.out.println("Received request from " + ip + ": " + uriResource);
			Map<String, Parameter[]> endpoints = new HashMap<>();
			Server.getInstance().getEndpoints()
								.entrySet()
								.stream()
								.filter(entry -> entry.getValue().getAnnotation(Parameters.class) != null)
								.forEach(entry -> endpoints.put(entry.getKey(), entry.getValue().getAnnotation(Parameters.class).value()));
			return NanoHTTPD.newFixedLengthResponse(
					html(
						head(
							title("Cleanse API"),
							link().withRel("stylesheet").withHref("https://cdn.jsdelivr.net/gh/kognise/water.css@latest/dist/dark.min.css")
						),
						body(
							h1("Cleanse API"),
							h2("Get the availability of items in nearby stores"),
							div(a().withHref("/api/endpoints").with(p("List of endpoints as JSON"))),
							each(endpoints.entrySet(), entry -> {
								List<String> params = Arrays.stream(entry.getValue()).map(param -> param+"="+param.getExampleValueEncoded()).collect(Collectors.toList());
								String request = entry.getKey()+"?"+String.join("&", params);
								return p(join("Example request:", a().withHref(request).with(code(request))));
							}),
							p(b("WARNING: The /api/target/ endpoint can take a VERY long time to return results. This is due to the internal structure of the Target API and cannot be avoided.")),
							each(endpoints.entrySet(), entry -> {
								String endpoint = entry.getKey();
								Parameter[] params = entry.getValue();
								return div(
										  join(
											  div(p(endpoint)), 
											  form().withMethod("post").with(
												  each(
													  Arrays.asList(params), 
													  param -> input().withPlaceholder(param.getExampleValue().toString())
													  				  .withValue(param.getExampleValue().toString())
																	  .withType(convertToHTMLType(param.getType()))
																	  .withName(param.toString())
																	  .withId(param.name())
																	  .withCondRequired(true)
												  ),
												  input().withType("hidden").withId("endpoint").withName("endpoint").withValue(endpoint),
												  button("Submit").withType("submit")
											)
										)
								);
							})
						)
					).render()
			);
		}
		catch(Throwable e) {
			return this.generateInternalErrorPage(e);
		}
	}
	@SuppressWarnings("unchecked")
	@Override
    public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		try {
			String ip = session.getRemoteIpAddress();
			if(Handler.isPrivateIP(ip)) {
				String tmp = session.getHeaders().get("x-forwarded-for");
				if(tmp != null) {
					ip = tmp;
				}
			}
			System.out.println("Received request from " + ip + ": " + uriResource);
			session.parseBody(null);
			Map<String, List<String>> params = session.getParameters();
			if(!params.containsKey("endpoint")) {
				return NanoHTTPD.newFixedLengthResponse(
						NanoHTTPD.Response.Status.BAD_REQUEST,
						"applciation/json",
						new JSONObject().put("error", "Missing required parameter: endpoint").toString()
				);
			}
			Handler handler = Handler.getHandlerFor(params.get("endpoint").get(0));
			if(handler == null) {
				return NanoHTTPD.newFixedLengthResponse(
						NanoHTTPD.Response.Status.NOT_FOUND,
						"application/json",
						new JSONObject().put("error", "Invalid endpoint: " + params.get("endpoint").get(0)).toString());
			}
			Parameter[] requiredParams = handler.getClass().getAnnotation(Parameters.class) == null ? new Parameter[0] : handler.getClass().getAnnotation(Parameters.class).value();
			boolean expand = false;
			String zip = null, q = null;
			for(Parameter param : requiredParams) {
				if(!params.containsKey(param.toString())) {
					return NanoHTTPD.newFixedLengthResponse(
							NanoHTTPD.Response.Status.BAD_REQUEST,
							"application/json",
							new JSONObject().put("error", "Missing required parameter: " + param).toString()
					);
				}
				switch(param) {
				case EXPAND:  expand = params.get("expand").get(0).equalsIgnoreCase("true"); break;
				case QUERY:   q = params.get("q").get(0); break;
				case ZIP: {
					zip = params.get("zip").get(0);
					if(!zip.matches("\\d{5}")) {
						return NanoHTTPD.newFixedLengthResponse(
								NanoHTTPD.Response.Status.BAD_REQUEST, 
								"application/json", 
								new JSONObject().put("error", "Invalid zip code: "+zip).toString()
						);
					}
				} break;
				}
			}
			JSONArray results = handler.getRequest().getStatus(zip, q, expand);
			if(results == null) {
				throw new RuntimeException("An unknown internal error occured");
			}
			if(results.length() == 1 && results.getJSONObject(0).has("error")) {
				return NanoHTTPD.newFixedLengthResponse(
						NanoHTTPD.Response.Status.BAD_REQUEST,
						"text/html",
						html(
							head(
								title(join("Results for ", q, " | Cleanse API")),
								link().withRel("stylesheet").withHref("https://cdn.jsdelivr.net/gh/kognise/water.css@latest/dist/dark.min.css")
							),
							body(
								h1("No stores found for the zipcode " + Entities.escape(zip)),
								form().withMethod("GET").with(button().withType("submit").with(p("Go Back")))
							)
						).render()
				);
			}
			return NanoHTTPD.newFixedLengthResponse(
					NanoHTTPD.Response.Status.OK,
					"text/html",
					html(
						head(
							title(join("Results for ", q, " | Cleanse API")),
							link().withRel("stylesheet").withHref("https://cdn.jsdelivr.net/gh/kognise/water.css@latest/dist/dark.min.css")
						),
						body(
							h2(join(String.valueOf(results.length()), "results for", Entities.escape(q))),
							table(
								tbody(
									each(results.toList(), obj -> {
										JSONObject json = new JSONObject();
										((Map<String, Object>)obj).forEach((k,v) -> json.put(k, JSONObject.wrap(v)));
										//System.err.println(json);
										return tr(
											td(img().withSrc(json.getJSONArray("images").getString(0))),
											td(join(
												a().withHref(json.getString("url")).with(p(json.getString("title"))),
												p(join("Price:", json.getJSONObject("price").getString("formatted"))),
												iffElse(
													json.has("stock"),
													p(join("Stock:", String.valueOf(json.optInt("stock")))),
													each(json.has("availability") ? json.getJSONArray("availability").toList() : Collections.emptyList(), o -> {
														JSONObject store = new JSONObject();
														((Map<String, Object>)o).forEach((k,v) -> store.put(k, JSONObject.wrap(v)));
														return p(join(
																a().withHref("https://www.google.com/maps/place/"+this.urlEncode(store.getString("address"))).with(b(store.getString("name"))), 
																String.format("(Stock: %d)", store.getInt("on_hand"))
														));
													})
												)
											))
										);
									})
								)
							)
						)
					).render()
			);
		}
		catch(Throwable e) {
			return this.generateInternalErrorPage(e);
		}
	}
	
	private String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, Charset.defaultCharset().displayName());
		}
		catch(Exception e) {
			return s;
		}
	}
	private String convertToHTMLType(Class<?> type) {
		if(type.equals(Integer.class) || type.equals(Long.class) || type.equals(int.class) || type.equals(long.class)) {
			return "number";
		}
		return "text";
	}
	private Response generateInternalErrorPage(Throwable e) {
		return NanoHTTPD.newFixedLengthResponse(
				NanoHTTPD.Response.Status.INTERNAL_ERROR, 
				"text/html", 
				html(
					body(
						h1("Internal Server Error"),
						p(e.toString()),
						hr(),
						address("Cleanse API. Powered by NanoHTTPD on Java " + System.getProperty("java.version"))
					)
				).render()
		);
	}
}
