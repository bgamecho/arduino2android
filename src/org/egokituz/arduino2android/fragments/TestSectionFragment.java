/**
 * 
 */
package org.egokituz.arduino2android.fragments;

import java.util.ArrayList;
import java.util.Collections;

import org.egokituz.arduino2android.R;
import org.egokituz.arduino2android.TestApplication;
import org.egokituz.arduino2android.activities.MainActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

/**
 * Main {@link Fragment} of the {@link MainActivity}. It contains the control buttons for a new test.
 * 
 * @author Xabier Gardeazabal
 *
 */
public class TestSectionFragment extends Fragment{

	private static final String TAG = "TestSectionFragment";

	public static final int REQUEST_ENABLE_BT_RESULT = 1;

	Spinner spinnerBluetooth;
	ListView devicesListView;

	/**
	 * Main context from the MainActivity
	 */
	private Context m_mainContext;

	/**
	 * The main Application for centralized data management and test control
	 */
	private TestApplication m_mainApp;

	private final int m_status_initial = 0;
	private final int m_status_ongoingTest = 1;
	
	private Button m_testButton;


	/**
	 * Constructor
	 */
	public TestSectionFragment() {
		super();
	}

	/**
	 * 
	 * @param c The main context
	 * @param app The main Application for centralized data management and test control
	 */
	public void setArguments(Context c, TestApplication app) {
		m_mainContext = c;
		m_mainApp = app;
	}

	// this method is only called once for this fragment
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// retain this fragment (so that when the activity's state changes, 
		// the configuration of this fragment is not lost
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,	Bundle savedInstanceState) {
		// Load the layout of this fragment
		View rootView = inflater.inflate(R.layout.fragment_section_main_activity, container, false);

		// Action of the "Begin test" button onClick event
		m_testButton = (Button) rootView.findViewById(R.id.buttonBeginTest);
		
		if(m_mainApp.isTestOngoing()){
			m_testButton.setTag(m_status_ongoingTest);
			m_testButton.setText(getResources().getString(R.string.stopTestButton));
		}
		else {
			m_testButton.setTag(m_status_initial);
			m_testButton.setText(getResources().getString(R.string.beginTestButton));
		}
		
		m_testButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int status =(Integer) view.getTag();
				switch (status) {
				case m_status_initial:
					requestBluetoothEnable();
					m_mainApp.beginTest();
					m_testButton.setText(getResources().getString(R.string.stopTestButton));
					break;

				case m_status_ongoingTest:
					m_mainApp.stopTest();
					m_testButton.setText(getResources().getString(R.string.beginTestButton));
					break;
				default:
					break;
				}
			}
		});

		// Action of the "Refresh list" button onClick event
		rootView.findViewById(R.id.buttonRefresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateSpinner();
			}
		});

		return rootView;
	}

	private void requestBluetoothEnable() {
		BluetoothAdapter _BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Check if this device supports Bluetooth
		if (_BluetoothAdapter == null) {
			// TODO Device does not support Bluetooth

		};

		// If Bluetooth is not already enabled, prompt and ask the ser to enable it
		if (!_BluetoothAdapter .isEnabled()){
			Log.e(TAG, "Bluetooth disabled");
			Log.v(TAG, "Asking for user permission to activate Bluetooth");
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			//enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			//m_mainContext.startActivity(enableBtIntent); // Start a new activity to turn Bluetooth ON

			//((Activity) m_mainContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT_RESULT);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT_RESULT);
			//TODO implement onActivityResult in main Activity
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == REQUEST_ENABLE_BT_RESULT) {
			// Bluetooth enable requested
			switch (resultCode){
			case android.app.Activity.RESULT_OK :
				Log.v(TAG, "Jay! User enabled Bluetooth!");
				//this.spinnerBluetooth.setClickable(true);
				break;
			case android.app.Activity.RESULT_CANCELED:
				Log.v(TAG, "User  did not enable Bluetooth");
				//this.spinnerBluetooth.setSelected(false);
				//this.spinnerBluetooth.setClickable(false);
				break;
			}
		}

	}

	/**
	 * Updates the items of the Bluetooth devices' spinner
	 */
	public void updateSpinner(){
		try {
			ArrayList<String> threads = new ArrayList<String>();
			Collections.addAll(threads, m_mainApp.getBTManager().getConnectedArduinos());
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(m_mainContext, android.R.layout.simple_spinner_item, threads);

			Spinner devSpin = (Spinner)getView().findViewById(R.id.spinnerBluetooth);
			devSpin.setAdapter(adapter);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Inquires the Bluetooth-Manager for the currently connected Arduino devices. 
	 * @return String[] array with the connected device IDs (name-MAC)
	 */
	public String[] getConnectedDevices(){
		String[] result = null;

		if(m_mainApp.getBTManager() != null && m_mainApp.getBTManager().isAlive())
			result = m_mainApp.getBTManager().getConnectedArduinos();
		return result;

	}
}
