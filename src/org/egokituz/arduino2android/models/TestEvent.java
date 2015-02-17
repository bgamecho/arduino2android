/**
 * 
 */
package org.egokituz.arduino2android.models;

import org.egokituz.arduino2android.R;

/**
 * @author Xabier Gardeazabal
 *
 */
public class TestEvent extends TestData {
	
	public static final int EVENT_NEW_DISCOVERY_STARTED = 1;
	public static final int EVENT_DISCOVERY_FINISHED = 2;
	public static final int EVENT_NEW_DEVICE_FOUND = 3;
	public static final int EVENT_NEW_DEVICE_CONNECTED = 4;
	public static final int EVENT_DEVICE_DISCONNECTED = 5;
	
	public int eventID;
	public String eventDetails;

	/**
	 * 
	 */
	public TestEvent() {
	}

	/* (non-Javadoc)
	 * @see org.egokituz.arduino2android.models.TestData#toString()
	 */
	@Override
	public String toString() {
		switch (eventID) {
		case EVENT_NEW_DISCOVERY_STARTED:
			return timestamp+"New discovery started";
		case EVENT_DISCOVERY_FINISHED:
			return timestamp+"Discovery finished";
		case EVENT_NEW_DEVICE_FOUND:
			return timestamp+"New device found";
		case EVENT_NEW_DEVICE_CONNECTED:
			return timestamp+"New device connected";
		case EVENT_DEVICE_DISCONNECTED:
			return timestamp+"Device disconnected";
		}
		return null;
	}

}
