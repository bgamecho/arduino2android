/**
 * 
 */
package org.egokituz.arduino2android.models;

/**
 * {@linkplain TestData} sub-class for CPU information
 * 
 * @author Xabier Gardeazabal
 *
 */
public class CPUData extends TestData {
	
	public Float cpuLoad; 
	
	public CPUData() {
		
	}
	
	public CPUData(long time, Float load ){
		timestamp = time;
		cpuLoad = load*100; // normalize to a percentage range 0-100
	}

	/* (non-Javadoc)
	 * @see org.egokituz.arduino2android.models.TestData#toString()
	 */
	@Override
	public String toString() {
		return timestamp+" "+cpuLoad;
	}

}
