/**
 * Copyright (C) 2014 Xabier Gardeazabal
 * 				Euskal Herriko Unibertsitatea
 * 				University of The Basque Country
 *              xgardeazabal@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package org.egokituz.arduino2android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.egokituz.utils.ArduinoMessage;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import android.widget.Spinner;
import android.widget.Toast;

/**
 * @author xgardeazabal
 */
public class MainActivity extends Activity {

	public final static String TAG = "ArduinoActivity"; // Tag to identify this class' messages in the console or LogCat

	//TODO REQUEST_ENABLE_BT is a request code that we provide (It's really just a number that you provide for onActivityResult)
	private static final int REQUEST_ENABLE_BT = 1;
	public static final int MESSAGE_DATA_READ = 2;
	public static final int MESSAGE_BATTERY_STATE_CHANGED = 4;
	public static final int MESSAGE_CPU_USAGE = 5;
	public static final int MESSAGE_PING_READ = 6;
	public static final int MESSAGE_ERROR_READING = 7;
	public static final int MESSAGE_BT_EVENT = 8;
	public static final int SETTINGS_RESULT = 9; 

	Spinner spinnerBluetooth;
	ListView devicesListView;

	Context m_context; // Main Context

	//// Module threads ///////////////////
	private BTManagerThread m_BTManager;
	private BatteryMonitorThread m_BatteryMonitor;
	private LoggerThread m_Logger;
	private CPUMonitorThread m_cpuMonitor;


	private ArrayList<String> m_testParameters = new ArrayList<>();

	public boolean m_finishApp; // Flag for managing activity termination

	private HashMap m_testPlanParameters; // Used to store current plan settings


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		m_context = this;

		m_finishApp = false;

		if(!handlerThread.isAlive())
			handlerThread.start();
		createHandler();

		try {
			m_BTManager = new BTManagerThread(this, arduinoHandler);
		} catch (Exception e) {
			e.printStackTrace();

			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(this, "Could not start the BT manager", duration);
			toast.show();
			this.finish();
		}

		m_BatteryMonitor = new BatteryMonitorThread(this, arduinoHandler);
		m_cpuMonitor = new CPUMonitorThread(this, arduinoHandler);
		m_Logger = new LoggerThread(this, arduinoHandler);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.help:
			//showHelp();
			return true;

		case R.id.preferences:
			// Call SettingsActivity intent
			Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
			startActivityForResult(i, SETTINGS_RESULT);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override 
	public void onStart(){
		super.onStart();
		Log.v(TAG, "Arduino Activity --OnStart()--");


		// Start the logger thread
		if(!m_Logger.isAlive())
			m_Logger.start();

		//Start the monitoring modules' threads
		if(!m_BatteryMonitor.isAlive())
			m_BatteryMonitor.start();
		if(!m_cpuMonitor.isAlive())
			m_cpuMonitor.start();

	}

	@Override
	public void onResume(){
		super.onResume();
		Log.v(TAG, "Arduino Activity --OnResume()--");
	}

	@Override
	public void onPause(){
		Log.v(TAG, "Arduino Activity --OnPause()--");
		super.onPause();
	}

	@Override
	public void onStop(){
		Log.v(TAG, "Arduino Activity --OnStop()--");
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		Log.v(TAG, "Arduino Activity --OnBackPressed()--");
		super.onBackPressed();
	}

	@Override
	public void onRestart(){
		super.onRestart();
		Log.v(TAG, "Arduino Activity --OnRestart()--");
	}

	@Override
	public void onDestroy(){
		Log.v(TAG, "Arduino Activity --OnDestroy()--");
		super.onDestroy();

		//Finalize threads
		if(!m_finishApp){
			m_BTManager.finalize();
			m_BatteryMonitor.finalize();
			m_cpuMonitor.finalize();
			m_Logger.finalize();
			//Shut down the HandlerThread
			handlerThread.quit();
		}
		m_finishApp = true;
	}


	@Override
	public void finish() {
		Log.v(TAG, "Arduino Activity --OnDestroy()--");
		super.finish();

		//Finalize threads
		if(!m_finishApp){
			m_BTManager.finalize();
			m_BatteryMonitor.finalize();
			m_cpuMonitor.finalize();
			m_Logger.finalize();
			//Shut down the HandlerThread
			handlerThread.quit();
		}
		m_finishApp = true;
	}

	/*
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == REQUEST_ENABLE_BT) {
			// Bluetooth enable requested
			switch (resultCode){
			case RESULT_OK:
				Log.v(TAG, "Jay! User enabled Bluetooth!");
				this.spinnerBluetooth.setClickable(true);
				break;
			case RESULT_CANCELED:
				Log.v(TAG, "User  did not enable Bluetooth");
				this.spinnerBluetooth.setSelected(false);
				this.spinnerBluetooth.setClickable(false);
				break;
			}
		}
	}
	 */


	/**
	 * Method called with the onClick event of the "Begin Test" button.
	 * <p>Retrieves the current test parapemeters from the app's preferences, 
	 * notifies the logger to begin its work, sends the test parameters to the
	 * BluetoothManager module, and finally starts said module.
	 * @param view
	 */
	public void beginTest(View view){
		// Retrieve the test parameters from the app's settings/preferences
		m_testPlanParameters = (HashMap) SettingsActivity.getCurrentPreferences(m_context);

		// Tell the logger that a new Test has begun  //NOT ANYMORE: a new log folder may be created with the new parameters
		m_Logger.m_logHandler.obtainMessage(LoggerThread.MESSAGE_NEW_TEST, m_testPlanParameters).sendToTarget();

		//TODO: send the test parameters to the BluetoothManager-thread
		// Set the Bluetooth Manager's plan with the selected parameters
		Message sendMsg;
		sendMsg = m_BTManager.btHandler.obtainMessage(BTManagerThread.MESSAGE_SET_SCENARIO,m_testPlanParameters); // TODO change the obj of the message
		sendMsg.sendToTarget();

		// Begin a new test
		m_BTManager.start();
	}

	/**
	 * Updates the ListView containing the connected Arduinos

	private void populateDeviceListView() {
		devicesListView = (ListView) findViewById(R.id.listViewDevices);

		final String[] myDeviceList = getConnectedDevices();

		if(myDeviceList != null){
			ArrayAdapter<String> listViewArrayAdapter = new ArrayAdapter<String>(this, 
					android.R.layout.simple_list_item_1, myDeviceList);
			devicesListView.setAdapter(listViewArrayAdapter);
		}
	}*/

	//Updates the items of the Bluetooth devices' spinner
	public void updateSpinner(View view){
		// TODO update spinned with running threads
		try {
			ArrayList<String> threads = new ArrayList<String>();
			Collections.addAll(threads, m_BTManager.getConnectedArduinos());
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item, threads);

			Spinner devSpin = (Spinner)findViewById(R.id.spinnerBluetooth);
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

		if(m_BTManager != null && m_BTManager.isAlive())
			result = m_BTManager.getConnectedArduinos();
		return result;

	}

	/**
	 * Handler connected with the BTManager Threads: 
	 */
	private HandlerThread handlerThread = new HandlerThread("MyHandlerThread");
	public Handler arduinoHandler;
	private void createHandler(){
		arduinoHandler = new Handler(handlerThread.getLooper()) {
			String sendMsg;
			byte[] readBuf;
			int elapsedMilis;
			int bytes;
			String devName, devMAC;
			long timestamp;
			long msgCount, errCount;
			ArduinoMessage msgReading;

			@SuppressLint("NewApi")
			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {
				case MESSAGE_PING_READ:
					// Message received from a running Arduino Thread
					// This message implies that 99 well formed PING messages were read by an Arduino Thread

					ArrayList<String> pingQueue = (ArrayList<String>) msg.obj;
					// write to log file
					m_Logger.m_logHandler.obtainMessage(LoggerThread.MESSAGE_PING, pingQueue).sendToTarget();
					break;

				case MESSAGE_DATA_READ:
					// Message received from a running Arduino Thread
					// This message implies that 99 well formed DATA messages were read by an Arduino Thread

					ArrayList<String> dataQueue = (ArrayList<String>) msg.obj;
					m_Logger.m_logHandler.obtainMessage(LoggerThread.MESSAGE_WRITE_DATA, dataQueue).sendToTarget();
					break;

				case MESSAGE_BATTERY_STATE_CHANGED:
					// Message received from the Battery-Monitor Thread
					// This message implies that the Battery percentage has changed

					Float batteryLoad = (Float) msg.obj;
					timestamp = msg.getData().getLong("TIMESTAMP");

					// call the Logger to write the battery load
					sendMsg = timestamp+" "+batteryLoad;
					m_Logger.m_logHandler.obtainMessage(LoggerThread.MESSAGE_WRITE_BATTERY, sendMsg).sendToTarget();

					break;

				case MESSAGE_CPU_USAGE:
					// Message received from a running Arduino Thread
					// This message implies that a malformed message has been read by an Arduino Thread

					Float cpu = (Float) msg.obj;
					timestamp = msg.getData().getLong("TIMESTAMP");

					// call the Logger to write the battery load
					sendMsg = timestamp+" "+cpu;
					m_Logger.m_logHandler.obtainMessage(LoggerThread.MESSAGE_CPU, sendMsg).sendToTarget();

					break;

				case MESSAGE_ERROR_READING:
					// Message received from the CPU-Monitor Thread
					// This message implies that the CPU usage has changed

					String error = (String) msg.obj;

					// write to log file
					m_Logger.m_logHandler.obtainMessage(LoggerThread.MESSAGE_ERROR, error).sendToTarget();

					break;

				case MESSAGE_BT_EVENT:
					// Message received from the CPU-Monitor Thread
					// This message implies that the CPU usage has changed

					String event = (String) msg.obj;

					// write to log file
					m_Logger.m_logHandler.obtainMessage(LoggerThread.MESSAGE_EVENT, event).sendToTarget();

					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(m_context, event, duration);
					toast.show();
					break;
				}
			}
		};
	}





}
