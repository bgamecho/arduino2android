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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.egokituz.utils.ArduinoMessage;
import org.egokituz.utils.BadMessageFrameFormat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * @author xgardeazabal
 */
public class MainActivity extends Activity {

	public final static String TAG = "ArduinoActivity";

	//TODO REQUEST_ENABLE_BT is a request code that we provide (It's really just a number that you provide for onActivityResult)
	private static final int REQUEST_ENABLE_BT = 1;
	public static final int MESSAGE_DATA_READ = 2;
	protected static final int MESSAGE_BATTERY_STATE_CHANGED = 4;
	public static final int MESSAGE_CPU_USAGE = 5;
	public static final int MESSAGE_PING_READ = 6;
	public static final int MESSAGE_ERROR_READING = 7;

	protected static final int MESSAGE_BT_EVENT = 8;

	Spinner spinnerBluetooth;
	ListView devicesListView;
	
	Context context;

	private BTManagerThread _BTManager;
	private BatteryMonitorThread _BatteryMonitor;
	private LoggerThread _Logger;
	private CPUMonitorThread _cpuMonitor;

	public boolean finishApp; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		setContentView(R.layout.activity_main);

		finishApp = false;

		if(!handlerThread.isAlive())
			handlerThread.start();
		createHandler();

		try {
			_BTManager = new BTManagerThread(this, arduinoHandler);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(this, "Could not start the BT manager", duration);
			toast.show();
			this.finish();
		}

		_BatteryMonitor = new BatteryMonitorThread(this, arduinoHandler);
		_cpuMonitor = new CPUMonitorThread(this, arduinoHandler);
		_Logger = new LoggerThread(this, arduinoHandler);

	}

	@Override 
	public void onStart(){
		super.onStart();
		Log.v(TAG, "Arduino Activity --OnStart()--");

		setButtons();

		if(!_BatteryMonitor.isAlive())
			_BatteryMonitor.start();
		if(!_cpuMonitor.isAlive())
			_cpuMonitor.start();
		if(!_Logger.isAlive())
			_Logger.start();

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
		if(!finishApp){
			_BTManager.finalize();
			_BatteryMonitor.finalize();
			_cpuMonitor.finalize();
			_Logger.finalize();
			//Shut down the HandlerThread
			handlerThread.quit();
		}
		finishApp = true;
	}


	@Override
	public void finish() {
		Log.v(TAG, "Arduino Activity --OnDestroy()--");
		super.finish();

		//Finalize threads
		if(!finishApp){
			_BTManager.finalize();
			_BatteryMonitor.finalize();
			_cpuMonitor.finalize();
			_Logger.finalize();
			//Shut down the HandlerThread
			handlerThread.quit();
		}
		finishApp = true;
	}


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


	private void setButtons(){
		RadioButton discoveryMode = (RadioButton) findViewById(R.id.radio_discovery_initial);
		discoveryMode.setChecked(true);

		RadioButton connectionMode = (RadioButton) findViewById(R.id.radio_progressive);
		connectionMode.setChecked(true);

		RadioButton connectionTiming = (RadioButton) findViewById(R.id.radio_delayed);
		connectionTiming.setChecked(true);
	}


	public void setPlanParameters(View view){
		int discoveryMode = 0, connectionMode = 0, connectionTiming = 0;

		RadioGroup discoveryGroup = (RadioGroup) findViewById(R.id.discoveryGroup);
		int discoveryRadioButtonID = discoveryGroup.getCheckedRadioButtonId();
		switch (discoveryRadioButtonID) {
		case R.id.radio_discovery_initial:
			discoveryMode = BTManagerThread.INITIAL_DISCOVERY;
			break;
		case R.id.radio_discovery_continuous:
			discoveryMode = BTManagerThread.CONTINUOUS_DISCOVERY;
			break;
		case R.id.radio_discovery_periodic:
			discoveryMode = BTManagerThread.PERIODIC_DISCOVERY;
			break;
		}

		RadioGroup connectionModeGroup = (RadioGroup) findViewById(R.id.connectionModeGroup);
		int connModeRadioButtonID = connectionModeGroup.getCheckedRadioButtonId();
		switch (connModeRadioButtonID) {
		case R.id.radio_progressive:
			connectionMode = BTManagerThread.PROGRESSIVE_CONNECT;
			break;
		case R.id.radio_alltogether:
			connectionMode = BTManagerThread.ALLTOGETHER_CONNECT;
			break;
		}

		RadioGroup connectionTimingGroup = (RadioGroup) findViewById(R.id.connectionTimingGroup);
		int connTimingRadioButtonID = connectionTimingGroup.getCheckedRadioButtonId();
		switch (connTimingRadioButtonID) {
		case R.id.radio_immediate_stop:
			connectionTiming = BTManagerThread.IMMEDIATE_STOP_DISCOVERY_CONNECT;
			break;
		case R.id.radio_immediate_while:
			connectionTiming = BTManagerThread.IMMEDIATE_WHILE_DISCOVERING_CONNECT;
			break;
		case R.id.radio_delayed:
			connectionTiming = BTManagerThread.DELAYED_CONNECT;
			break;
		}

		if(_BTManager.isAlive()){
			Log.v(TAG, "Restarting BTManager thread with new parameters...");
			_BTManager.finalize();
			try {
				_BTManager = new BTManagerThread(this, arduinoHandler);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(this, "Could not start the BT manager", duration);
				toast.show();
				this.finish();
			}
		}
		// Set the Bluetooth Manager's plan with the selected parameters
		Message sendMsg;
		sendMsg = _BTManager.btHandler.obtainMessage(BTManagerThread.MESSAGE_SET_SCENARIO,connectionTiming); // TODO change the obj of the message
		sendMsg.arg1 = discoveryMode;
		sendMsg.arg2 = connectionMode;
		sendMsg.sendToTarget();
		
		_BTManager.start();			
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
		Collections.addAll(threads, _BTManager.getConnectedArduinos());
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

	if(_BTManager != null && _BTManager.isAlive())
		result = _BTManager.getConnectedArduinos();
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
				_Logger.logHandler.obtainMessage(LoggerThread.MESSAGE_PING, pingQueue).sendToTarget();
				break;
				
			case MESSAGE_DATA_READ:
				// Message received from a running Arduino Thread
				// This message implies that 99 well formed DATA messages were read by an Arduino Thread

				ArrayList<String> dataQueue = (ArrayList<String>) msg.obj;
				_Logger.logHandler.obtainMessage(LoggerThread.MESSAGE_WRITE_DATA, dataQueue).sendToTarget();
				break;

			case MESSAGE_BATTERY_STATE_CHANGED:
				// Message received from the Battery-Monitor Thread
				// This message implies that the Battery percentage has changed

				Float batteryLoad = (Float) msg.obj;
				timestamp = msg.getData().getLong("TIMESTAMP");

				// call the Logger to write the battery load
				sendMsg = timestamp+" "+batteryLoad;
				_Logger.logHandler.obtainMessage(LoggerThread.MESSAGE_WRITE_BATTERY, sendMsg).sendToTarget();

				break;

			case MESSAGE_CPU_USAGE:
				// Message received from a running Arduino Thread
				// This message implies that a malformed message has been read by an Arduino Thread

				Float cpu = (Float) msg.obj;
				timestamp = msg.getData().getLong("TIMESTAMP");

				// call the Logger to write the battery load
				sendMsg = timestamp+" "+cpu;
				_Logger.logHandler.obtainMessage(LoggerThread.MESSAGE_CPU, sendMsg).sendToTarget();

				break;
				
			case MESSAGE_ERROR_READING:
				// Message received from the CPU-Monitor Thread
				// This message implies that the CPU usage has changed

				String error = (String) msg.obj;
				
				// write to log file
				_Logger.logHandler.obtainMessage(LoggerThread.MESSAGE_ERROR, error).sendToTarget();

				break;
				
			case MESSAGE_BT_EVENT:
				// Message received from the CPU-Monitor Thread
				// This message implies that the CPU usage has changed

				String event = (String) msg.obj;
				
				// write to log file
				_Logger.logHandler.obtainMessage(LoggerThread.MESSAGE_EVENT, event).sendToTarget();

				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(context, event, duration);
				toast.show();
				break;
			}
		}
	};
}





}
