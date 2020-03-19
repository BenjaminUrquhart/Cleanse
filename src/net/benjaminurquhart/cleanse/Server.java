package net.benjaminurquhart.cleanse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.reflections.Reflections;

import fi.iki.elonen.router.RouterNanoHTTPD;
import net.benjaminurquhart.cleanse.handlers.NotFoundHandler;
import net.benjaminurquhart.cleanse.handlers.Route;

public class Server extends RouterNanoHTTPD {
	
	private final List<String> routes;
	private static Server INSTANCE;
	
	public static Server getInstance() {
		if(INSTANCE == null) {
			try {
				INSTANCE = new Server(8888);
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
		this.routes = new ArrayList<>();
		
		this.addMappings();
		start(RouterNanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}
	
	public List<String> getRoutes() {
		return routes;
	}
	
	@Override
    public void addMappings() {
		this.setNotFoundHandler(NotFoundHandler.class);
        try {
        	Route route;
        	int loaded = 0, ignored = 0, failed = 0;
        	Reflections reflections = new Reflections("net.benjaminurquhart.cleanse.handlers");
        	for(Class<?> clazz : reflections.getSubTypesOf(DefaultHandler.class)) {
        		if((route = clazz.getAnnotation(Route.class)) != null) {
        			try {
        				this.addRoute(route.value(), clazz);
        				routes.add(route.value());
        				loaded++;
        				System.err.println("Added handler"+clazz.getName()+" for route "+route.value());
        			}
        			catch(Exception e) {
        				failed++;
        				System.err.println("Failed to add handler"+clazz.getName()+" for route "+route.value()+": "+e);
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
