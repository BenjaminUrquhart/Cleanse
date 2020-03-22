package net.benjaminurquhart.cleanse.storeapi;

public class InvalidZipException extends RuntimeException {

	private static final long serialVersionUID = 6060522207992776630L;
	
	public InvalidZipException(String zip) {
		super(zip);
	}
}
