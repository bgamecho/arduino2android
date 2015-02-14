/**
 * 
 */
package org.egokituz.arduino2android.models;

/**
 * @author Xabier Gardeazabal
 *
 */
public class BatteryData extends TestData {

	public Float batteryLevel;
	
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
		return null;
	}

}
