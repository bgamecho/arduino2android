/**
 * 
 */
package org.egokituz.arduino2android.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egokituz.arduino2android.BTManagerThread;
import org.egokituz.arduino2android.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

/**
 * @author Xabier Gardeazabal
 *
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener{


	public static final String PREF_DISCOVERY_PLAN = "pref_discoveryPlan";
	public static final String PREF_CONNECTION_TIMING = "pref_connectionTiming";
	public static final String PREF_CONNECTION_MODE = "pref_connectionMode";
	public static final String PREF_PERFORMANCE_MODE = "pref_performance_mode";
	public static final String PREF_DISCOVERY_INTERVAL = "pref_discovery_interval";

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        initSummary(getPreferenceScreen());
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
		.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
		.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {


		updatePrefSummary(findPreference(key));

		//TODO: set summary for the preferences
		Preference pref = findPreference(key);
		//		if (pref instanceof ListPreference) {
		//			ListPreference listPref = (ListPreference) pref;
		//			pref.setSummary(listPref.getEntry());
		//		}

		/*
		if (key.equals(PREF_DISCOVERY_PLAN)) {
			Preference connectionPref = findPreference(key);
			// Set summary to be the user-description for the selected value
			connectionPref.setSummary(sharedPreferences.getString(key, ""));
		}*/
	}

	private void initSummary(Preference p) {
		if (p instanceof PreferenceGroup) {
			PreferenceGroup pGrp = (PreferenceGroup) p;
			for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
				initSummary(pGrp.getPreference(i));
			}
		} else {
			updatePrefSummary(p);
		}
	}

	private void updatePrefSummary(Preference p) {
		if (p instanceof ListPreference) {
			ListPreference listPref = (ListPreference) p;
			p.setSummary(listPref.getEntry());
		}
		if (p instanceof EditTextPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;
			if (p.getTitle().toString().contains("assword"))
			{
				p.setSummary("******");
			} else {
				p.setSummary(editTextPref.getText());
			}
		}
		if (p instanceof MultiSelectListPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;
			p.setSummary(editTextPref.getText());
		}
	}

	public static Map<String, Integer> getCurrentPreferences(Context context){
		HashMap<String, Integer> preferenceMap = new HashMap<>();

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		String key, default_val;
		int value;

		key = SettingsActivity.PREF_DISCOVERY_PLAN;
		//default_val = context.getResources().getInteger(R.string.pref_discoveryPlan_default);
		default_val = context.getResources().getString(R.string.pref_discoveryPlan_default);
		value = Integer.parseInt(sharedPref.getString(key, default_val));
		preferenceMap.put(key, value);

		key = PREF_CONNECTION_TIMING;
		default_val = context.getResources().getString(R.string.pref_connectionTiming_default);
		value = Integer.parseInt(sharedPref.getString(key, default_val));
		preferenceMap.put(key, value);

		key = PREF_CONNECTION_MODE;
		default_val = context.getResources().getString(R.string.pref_connectionMode_default);
		value = Integer.parseInt(sharedPref.getString(key, default_val));
		preferenceMap.put(key, value);

		return preferenceMap;
	}


	public static List<String> preferenceListToString(HashMap<String, Integer> _hm){
		ArrayList<String> result = new ArrayList<String>();

		int value = -1;
		for(String key : _hm.keySet()){
			value = _hm.get(key);
			switch (key) {
			case PREF_DISCOVERY_PLAN:

				switch (value) {
				case BTManagerThread.INITIAL_DISCOVERY:
					result.add("INITIAL_DISCOVERY");
					break;
				case BTManagerThread.CONTINUOUS_DISCOVERY:
					result.add("CONTINUOUS_DISCOVERY");
					break;
				case BTManagerThread.PERIODIC_DISCOVERY:
					result.add("PERIODIC_DISCOVERY");
					break;
				default:
					break;
				}
				break;

			case PREF_CONNECTION_TIMING:
				value = _hm.get(key);
				switch (value) {
				case  BTManagerThread.PROGRESSIVE_CONNECT:
					result.add("PROGRESSIVE_CONNECT");
					break;
				case BTManagerThread.ALLTOGETHER_CONNECT:
					result.add("ALLTOGETHER_CONNECT");
					break;
				default:
					break;
				}
				break;

			case PREF_CONNECTION_MODE:
				switch (value) {
				case BTManagerThread.IMMEDIATE_STOP_DISCOVERY_CONNECT:
					result.add("IMMEDIATE_STOP_DISCOVERY_CONNECT");
					break;
				case BTManagerThread.IMMEDIATE_WHILE_DISCOVERING_CONNECT:
					result.add("IMMEDIATE_WHILE_DISCOVERING_CONNECT");
					break;
				case BTManagerThread.DELAYED_CONNECT:
					result.add("DELAYED_CONNECT");
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}

		}
		return result;
	}
}
