package org.egokituz.arduino2android.models.exceptions;

/**
 * Custom exception class for unexpected frame sequence number
 * 
 * @author Xabier Gardeazabal
 *
 */
public class UnexpectedFrameSequenceNumber extends Exception {
	private static final long serialVersionUID = -8014083640785987605L;

	public UnexpectedFrameSequenceNumber() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UnexpectedFrameSequenceNumber(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
		// TODO Auto-generated constructor stub
	}

	public UnexpectedFrameSequenceNumber(String detailMessage) {
		super(detailMessage);
		// TODO Auto-generated constructor stub
	}

	public UnexpectedFrameSequenceNumber(Throwable throwable) {
		super(throwable);
		// TODO Auto-generated constructor stub
	}

	
}
