/**
 * 
 */
package org.egokituz.arduino2android.models;

/**
 * {@linkplain TestData} sub-class for statistics
 * 
 * @author Xabier Gardeazabal
 *
 */
public class TestStatistics extends TestData {
	
	
	public long startTime;
	public Float startBattery;
	public float batteryDrainHour = 0f;
	public long transferedBytes = 0;
	public long totalErrors = 0;
	public int totalDiscoveries = 0;
	public long totalPings = 0;
	public long totalMessages = 0;
	public float meanPing = 0f;
	public float meanCPU = 0f;
	public float btSpeed =0f;

	/**
	 * 
	 */
	public TestStatistics() {
		// TODO Auto-generated constructor stub
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
