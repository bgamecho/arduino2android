/**
 * 
 */
package org.egokituz.arduino2android.gui;

import java.util.ArrayList;
import java.util.Collections;

import org.egokituz.arduino2android.R;
import org.egokituz.arduino2android.TestApplication;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

/**
 * @author Xabier Gardeazabal
 *
 */
public class TestSectionFragment extends Fragment{


	Spinner spinnerBluetooth;
	ListView devicesListView;
	private Context m_mainContext;
	private TestApplication m_mainApp;

	public TestSectionFragment() {
		super();
	}

	public void setArguments(Context c, TestApplication app) {
		m_mainContext = c;
		m_mainApp = app;
	}

	// this method is only called once for this fragment
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// retain this fragment
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,	Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_section_main_activity, container, false);


		// Demonstration of a collection-browsing activity.
		rootView.findViewById(R.id.buttonBeginTest).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				/*
				Intent intent = new Intent(getActivity(), CollectionDemoActivity.class);
				startActivity(intent);*/
				m_mainApp.beginTest();
			}
		});

		// Demonstration of navigating to external activities.
		rootView.findViewById(R.id.buttonRefresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateSpinner();
			}
		});

		return rootView;
	}

	//Updates the items of the Bluetooth devices' spinner
	public void updateSpinner(){
		// TODO update spinned with running threads
		try {
			ArrayList<String> threads = new ArrayList<String>();
			Collections.addAll(threads, m_mainApp.getBTManager().getConnectedArduinos());
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(m_mainContext, android.R.layout.simple_spinner_item, threads);

			Spinner devSpin = (Spinner)getView().findViewById(R.id.spinnerBluetooth);
			devSpin.setAdapter(adapter);
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
