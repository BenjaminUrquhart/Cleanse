package net.benjaminurquhart.cleanse;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import net.benjaminurquhart.cleanse.storeapi.requests.TargetRequest;

public class Server extends NanoHTTPD {
	
	private final TargetRequest TARGET = new TargetRequest();

	public Server(int port) throws IOException {
		super(port);
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}

	@Override
    public Response serve(IHTTPSession session) {
		try {
			System.out.println("Received request from " + session.getRemoteIpAddress());
			Response response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", TARGET.getToiletPaperStatus("06810").toString());
			System.out.println("Done");
			return response;
		}
		catch(Exception e) {
			e.printStackTrace();
			return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/html", e.toString());
		}
	}
}
