package net.benjaminurquhart.cleanse;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import net.benjaminurquhart.cleanse.storeapi.Requester;

public class Server extends NanoHTTPD {

	public Server(int port) throws IOException {
		super(port);
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}

	@Override
    public Response serve(IHTTPSession session) {
		try {
			return NanoHTTPD.newFixedLengthResponse(
					NanoHTTPD.Response.Status.OK, 
					"application/json", 
					Requester.requestJSON("https://redsky.target.com/v1/location_details/75665851?zip=91104&storeId=883").toString()
			);
		}
		catch(Exception e) {
			e.printStackTrace();
			return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/html", e.toString());
		}
	}
}
