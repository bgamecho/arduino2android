/**
 * 
 */
package org.egokituz.arduino2android.models;

/**
 * @author Xabier Gardeazabal
 *
 */
public class PingData extends TestData {
	
	public Float pingTime;
	public String arduinoID;
	

	/**
	 * 
	 */
	public PingData(long time, Float ping, String id) {
		timestamp = time;
		pingTime = ping;
		arduinoID = id;
	}

	/* (non-Javadoc)
	 * @see org.egokituz.arduino2android.models.TestData#toString()
	 */
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
