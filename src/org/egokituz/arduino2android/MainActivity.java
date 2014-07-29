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
	public static final int MESSAGE_READ = 2;
	protected static final int MESSAGE_BATTERY_STATE_CHANGED = 4;
	public static final int MESSAGE_CPU_USAGE = 5;
	public static final int MESSAGE_PING = 6;

	Spinner spinnerBluetooth;
	ListView devicesListView;

	private BTManagerThread _BTManager;
	private BatteryMonitorThread _BatteryMonitor;
	private LoggerThread _Logger;
	private CPUMonitorThread _cpuMonitor;

	public boolean finishApp; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
			Toast toast = Toast.makeText(this, "Bluetooth not supported", duration);
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
		
		if(!_BTManager.isAlive()){
			_BTManager.start();

			// Set the Bluetooth Manager's plan  
			Message sendMsg;
			sendMsg = _BTManager.btHandler.obtainMessage(BTManagerThread.MESSAGE_SET_SCENARIO,BTManagerThread.DELAYED_CONNECT); // TODO change the obj of the message
			sendMsg.arg1 = BTManagerThread.INITIAL_DISCOVERY;
			sendMsg.arg2 = BTManagerThread.PROGRESSIVE_CONNECT;
			sendMsg.sendToTarget();
		}
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

	public void setPlanButton(View view){


		RadioGroup discoveryGroup = (RadioGroup) findViewById(R.id.discoveryGroup);
		int discoveryRadioButtonID = discoveryGroup.getCheckedRadioButtonId();
		switch (discoveryRadioButtonID) {
		case R.id.radio_discovery_initial:

			break;
		case R.id.radio_discovery_continuous:

			break;
		case R.id.radio_discovery_periodic:

			break;
		}

		RadioGroup connectionModeGroup = (RadioGroup) findViewById(R.id.connectionModeGroup);
		int connModeRadioButtonID = connectionModeGroup.getCheckedRadioButtonId();
		switch (connModeRadioButtonID) {
		case R.id.radio_progressive:

			break;
		case R.id.radio_alltogether:

			break;
		}

		RadioGroup connectionTimingGroup = (RadioGroup) findViewById(R.id.connectionTimingGroup);
		int connTimingRadioButtonID = connectionTimingGroup.getCheckedRadioButtonId();
		switch (connTimingRadioButtonID) {
		case R.id.radio_immediate_stop:

			break;
		case R.id.radio_immediate_while:

			break;
		case R.id.radio_delayed:

			break;
		}
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
				case MESSAGE_READ:
					// Message received from a running Arduino Thread
					// This message implies that a well formed message was read by an Arduino Thread

					readBuf = (byte[]) msg.obj;
					elapsedMilis = msg.arg1;
					bytes = msg.arg2;
					devName =  msg.getData().getString("NAME");
					devMAC =  msg.getData().getString("MAC");
					timestamp = msg.getData().getLong("TIMESTAMP");


					try {
						msgReading = new ArduinoMessage(readBuf);
					} catch (BadMessageFrameFormat e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					//String payload = msgReading.getPayload();

					// write to log file
					sendMsg = timestamp+" "+devName+" "+elapsedMilis+"ms "+bytes+" bytes";
					_Logger.logHandler.obtainMessage(LoggerThread.MESSAGE_WRITE_TO_LOG_FILE, sendMsg).sendToTarget();

					break;
				case MESSAGE_PING:
					// Message received from a running Arduino Thread
					// This message implies that a well formed PING message was read by an Arduino Thread

					readBuf = (byte[]) msg.obj;
					elapsedMilis = msg.arg1;
					bytes = msg.arg2;
					devName =  msg.getData().getString("NAME");
					devMAC =  msg.getData().getString("MAC");
					timestamp = msg.getData().getLong("TIMESTAMP");
					msgCount = msg.getData().getLong("MSG_COUNT");
					errCount = msg.getData().getLong("ERROR_COUNT");

					// If it's a ping message, the field PINGSENTTIME is relevant
					long pingSentTime = msg.getData().getLong("PINGSENTTIME");
					long pingTime = timestamp-pingSentTime;

					try {
						msgReading = new ArduinoMessage(readBuf);
					} catch (BadMessageFrameFormat e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					int frNum = msgReading.getFrameNum();
					Log.v(TAG,"Ping nº "+frNum+" time: "+pingTime);

					// write to log file
					sendMsg = timestamp+" "+devName+" "+pingTime;
					_Logger.logHandler.obtainMessage(LoggerThread.MESSAGE_PING, sendMsg).sendToTarget();

					//tvLdr.setText(readMessage);
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
					// Message received from the CPU-Monitor Thread
					// This message implies that the CPU usage has changed

					Float cpu = (Float) msg.obj;
					timestamp = msg.getData().getLong("TIMESTAMP");

					// call the Logger to write the battery load
					sendMsg = timestamp+" "+cpu;
					_Logger.logHandler.obtainMessage(LoggerThread.MESSAGE_CPU, sendMsg).sendToTarget();

					break;
				}
			}
		};
	}





}
