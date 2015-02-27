package org.egokituz.arduino2android.models.exceptions;

/**
 * Custom exception class for badly formatted message frames
 *  
 * @author Xabier Gardeazabal
 *
 */
public class BadMessageFrameFormat extends Exception {
	private static final long serialVersionUID = 1033924695297657739L;

	public BadMessageFrameFormat() {
		super();
		// TODO Auto-generated constructor stub
	}

	public BadMessageFrameFormat(String detailMessage) {
		super(detailMessage);
		// TODO Auto-generated constructor stub
	}

	public BadMessageFrameFormat(Throwable throwable) {
		super(throwable);
		// TODO Auto-generated constructor stub
	}

	public BadMessageFrameFormat(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
		// TODO Auto-generated constructor stub
	}

}
