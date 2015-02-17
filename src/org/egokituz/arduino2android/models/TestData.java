/**
 * 
 */
package org.egokituz.arduino2android.models;

import java.util.ArrayList;

/**
 * @author Xabier Gardeazabal
 *
 */
public abstract class TestData {
	
	// Data-type identifiers
	public static final int DATA_BATTERY = 100;
	public static final int DATA_CPU = 200;
	public static final int DATA_PING = 300;
	public static final int DATA_ERROR = 400;
	public static final int DATA_EVENT = 500;
	public static final int DATA_STRESS = 600;
	public static final int DATA_STATISTIC = 700;

	/**
	 * The source device this data comes from
	 */
	public String source;
	
	/**
	 * The time this data object was captured
	 */
	public long timestamp;
	
	@Override
	public abstract String toString();
	
	public static ArrayList<String> toStringList(ArrayList<TestData> list){
		ArrayList<String> result = new ArrayList<String>();
		for(TestData d : list){
			result.add(d.toString());
		}
		return result;
	}
}
