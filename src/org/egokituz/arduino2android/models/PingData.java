/**
 * 
 */
package org.egokituz.arduino2android.models;

/**
 * {@linkplain TestData} sub-class for Ping information
 * 
 * @author Xabier Gardeazabal
 *
 */
public class PingData extends TestData {
	
	public long pingTime;
	public String arduinoID;
	public int msgSize; 
	

	/**
	 * 
	 */
	public PingData(long time, int size, long ping, String id) {
		timestamp = time;
		msgSize = size;
		pingTime = ping;
		arduinoID = id;
	}

	/* (non-Javadoc)
	 * @see org.egokituz.arduino2android.models.TestData#toString()
	 */
	@Override
	public String toString() {
		return timestamp+" "+arduinoID+" "+msgSize+" "+pingTime;
	}

}
