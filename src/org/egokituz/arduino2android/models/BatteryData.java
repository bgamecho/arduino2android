/**
 * 
 */
package org.egokituz.arduino2android.models;

/**
 * {@linkplain TestData} sub-class for Battery information
 * 
 * @author Xabier Gardeazabal
 *
 */
public class BatteryData extends TestData {

	
	public float batteryLevel;
	
	/**
	 * 
	 */
	public BatteryData(long time, Float battery) {
		timestamp = time;
		batteryLevel = battery;
	}

	/* (non-Javadoc)
	 * @see org.egokituz.arduino2android.models.TestData#toString()
	 */
	@Override
	public String toString() {
		return timestamp+" "+batteryLevel;
	}

}
