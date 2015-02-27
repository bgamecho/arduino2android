/**
 * 
 */
package org.egokituz.arduino2android.models;

/**
 * {@linkplain TestData} sub-class for messaging error information
 * 
 * @author Xabier Gardeazabal
 *
 */
public class TestError extends TestData {

	/**
	 * 
	 */
	public TestError() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.egokituz.arduino2android.models.TestData#toString()
	 */
	@Override
	public String toString() {
		return timestamp+" "+source;
	}

}
