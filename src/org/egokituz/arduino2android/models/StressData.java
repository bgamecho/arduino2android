/**
 * 
 */
package org.egokituz.arduino2android.models;

/**
 * @author Xabier Gardeazabal
 *
 */
public class StressData extends TestData {

	public String arduinoID;
	public int msgSize;
	
	/**
	 * 
	 */
	public StressData(long time, int size, String id) {
		timestamp = time;
		msgSize = size;
		arduinoID = id;
	}

	/* (non-Javadoc)
	 * @see org.egokituz.arduino2android.models.TestData#toString()
	 */
	@Override
	public String toString() {
		return timestamp+" "+arduinoID+" "+msgSize;
	}

}
