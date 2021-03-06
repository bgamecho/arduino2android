package org.egokituz.arduino2android.models.exceptions;

/**
 * Custom exception class for incorrect ETX bytes
 * 
 * @author Xabier Gardeazabal
 *
 */
public class IncorrectETXbyte extends Exception {
	private static final long serialVersionUID = 187830150214578031L;

	public IncorrectETXbyte() {
		super();
		// TODO Auto-generated constructor stub
	}

	public IncorrectETXbyte(String detailMessage) {
		super(detailMessage);
		// TODO Auto-generated constructor stub
	}

	public IncorrectETXbyte(Throwable throwable) {
		super(throwable);
		// TODO Auto-generated constructor stub
	}

	public IncorrectETXbyte(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
		// TODO Auto-generated constructor stub
	}

}
