package net.benjaminurquhart.cleanse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.reflections.Reflections;

import fi.iki.elonen.router.RouterNanoHTTPD;
import net.benjaminurquhart.cleanse.handlers.NotFoundHandler;
import net.benjaminurquhart.cleanse.handlers.Route;

public class Server extends RouterNanoHTTPD {
	
	private final Map<String, Class<? extends DefaultHandler>> handlers;
	private static Server INSTANCE;
	private int port;
	
	public static Server getInstance() {
		if(INSTANCE == null) {
			try {
				String port = System.getenv("port");
				INSTANCE = new Server(port == null ? 8888 : Integer.parseInt(port));
			}
			catch(Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return INSTANCE;
	}

	private Server(int port) throws IOException {
		super(port);
		this.handlers = new HashMap<>();
		this.port = port;
		
		this.addMappings();
		start(RouterNanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}
	
	public int getPort() {
		return port;
	}
	
	public Map<String, Class<? extends DefaultHandler>> getEndpoints() {
		return Collections.unmodifiableMap(handlers);
	}
	
	@Override
    public void addMappings() {
		this.setNotFoundHandler(NotFoundHandler.class);
        try {
        	Route route;
        	int loaded = 0, ignored = 0, failed = 0;
        	Reflections reflections = new Reflections("net.benjaminurquhart.cleanse.handlers");
        	for(Class<? extends DefaultHandler> clazz : reflections.getSubTypesOf(DefaultHandler.class)) {
        		if((route = clazz.getAnnotation(Route.class)) != null) {
        			try {
        				this.addRoute(route.value(), clazz);
        				handlers.put(route.value(), clazz);
        				loaded++;
        				System.err.println("Added handler "+clazz.getName()+" for route "+route.value());
        			}
        			catch(Exception e) {
        				failed++;
        				System.err.println("Failed to add handler" +clazz.getName()+" for route "+route.value()+": "+e);
        			}
        		}
        		else {
        			ignored++;
        			System.err.println("Handler "+clazz.getName()+" is missing the @Route annotation. Ignoring...");
        		}
        	}
        	System.err.printf("\nLoaded: %d\nFailed: %d\nIgnored: %d\n-Total-: %d\n", loaded, failed, ignored, loaded+failed+ignored);
        }
        catch(Exception e) {
        	e.printStackTrace();
        	System.exit(1);
        }
    }
}
