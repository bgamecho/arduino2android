/**
 * 
 */
package org.egokituz.arduino2android;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * @author Xabier Gardeazabal
 *
 */
public class PreferencesActivity extends PreferenceActivity {


	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
