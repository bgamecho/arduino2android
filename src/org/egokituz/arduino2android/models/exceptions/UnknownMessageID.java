package org.egokituz.arduino2android.models.exceptions;

/**
 * Custom exception class for unkown message IDs
 * 
 * @author Xabier Gardeazabal
 *
 */
public class UnknownMessageID extends Exception {
	private static final long serialVersionUID = -8014083640785987605L;

	public UnknownMessageID() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UnknownMessageID(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
		// TODO Auto-generated constructor stub
	}

	public UnknownMessageID(String detailMessage) {
		super(detailMessage);
		// TODO Auto-generated constructor stub
	}

	public UnknownMessageID(Throwable throwable) {
		super(throwable);
		// TODO Auto-generated constructor stub
	}

	
}
