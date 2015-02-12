/**
 * 
 */
package org.egokituz.arduino2android;

/**
 * Class for storing application-global variables
 * 
 * @author Xabier Gardeazabal
 *
 */
final public class Constants { //final to prevent instantiation
	
	/** Discovery plan constants */
	public static final String INITIAL_DISCOVERY = "INITIAL_DISCOVERY";
	public static final String CONTINUOUS_DISCOVERY = "CONTINUOUS_DISCOVERY";
	public static final String PERIODIC_DISCOVERY = "PERIODIC_DISCOVERY";
	
	public static final String PROGRESSIVE_CONNECT = "PROGRESSIVE_CONNECT";
	public static final String ALLTOGETHER_CONNECT = "ALLTOGETHER_CONNECT";
	
	public static final String IMMEDIATE_STOP_DISCOVERY_CONNECT = "IMMEDIATE_STOP_DISCOVERY_CONNECT";
	public static final String IMMEDIATE_WHILE_DISCOVERING_CONNECT = "IMMEDIATE_WHILE_DISCOVERING_CONNECT";
	public static final String DELAYED_CONNECT = "DELAYED_CONNECT";
	
		
	//private constructor to prevent instantiation/inheritance
	private Constants(){
		
	}
}
