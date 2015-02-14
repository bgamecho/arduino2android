/**
 * 
 */
package org.egokituz.arduino2android.models;

/**
 * @author Xabier Gardeazabal
 *
 */
public class CPUData extends TestData {
	
	public Float cpuLoad; 
	
	public CPUData() {
		
	}
	
	public CPUData(long time, Float load ){
		timestamp = time;
		cpuLoad = load;
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
