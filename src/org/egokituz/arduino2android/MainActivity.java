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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * @author xgardeazabal
 */
public class MainActivity extends Activity {

	public final static String TAG = "ArduinoActivity";

	public static final int MESSAGE_TOAST = 0;
	//TODO REQUEST_ENABLE_BT is a request code that we provide (It's really just a number that you provide for onActivityResult)
	private static final int REQUEST_ENABLE_BT = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_CONNECT_ARDUINO = 3;
	protected static final int MESSAGE_BATTERY_STATE_CHANGED = 4;
	public static final int MESSAGE_CPU_USAGE = 5;
	public static final int MESSAGE_PING = 6;

	Spinner spinnerBluetooth;
	ListView devicesListView;

	private String selected_arduinoMAC;

	private BTManagerThread _BTManager;
	private BatteryMonitorThread _BatteryMonitor;
	private LoggerThread _Logger;
	private CPUMonitorThread _cpuMonitor;

	private ArduinoThread arduino;
	boolean ardionoOn;

	public boolean finishApp; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ardionoOn = false;
		finishApp = false;

		//populateDeviceListView();

		_BTManager = new BTManagerThread(this, arduinoHandler);
		_BatteryMonitor = new BatteryMonitorThread(this, arduinoHandler);
		_cpuMonitor = new CPUMonitorThread(this, arduinoHandler);
		_Logger = new LoggerThread(this, arduinoHandler);

	}

	@Override 
	public void onStart(){
		Log.v(TAG, "Arduino Activity --OnStart()--");
		if(!_BTManager.isAlive()){
			_BTManager.start();

			// Set the Bluetooth Manager's plan  
			Message sendMsg;
			sendMsg = _BTManager.btHandler.obtainMessage(BTManagerThread.MESSAGE_SET_SCENARIO,BTManagerThread.DELAYED_CONNECT); // TODO change the obj of the message
			sendMsg.arg1 = BTManagerThread.INITIAL_DISCOVERY;
			sendMsg.arg2 = BTManagerThread.ALLTOGETHER_CONNECT;
			sendMsg.sendToTarget();
		}

		setButtons();

		if(!_BatteryMonitor.isAlive())
			_BatteryMonitor.start();
		if(!_cpuMonitor.isAlive())
			_cpuMonitor.start();
		if(!_Logger.isAlive())
			_Logger.start();
		super.onStart();
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
		disconnectAduino();
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		Log.v(TAG, "Arduino Activity --OnBackPressed()--");
		if(ardionoOn){
			disconnectAduino();			
		}else{
			super.onBackPressed();;
		}
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


	private int getIndex(Spinner spinner, String myString){

		int index = 0;

		for (int i=0;i<spinner.getCount();i++){
			String aux = (String) spinner.getItemAtPosition(i);
			if (aux.contains(myString)){
				index = i;
				continue;
			}
		}
		return index;
	}

	/**
	 * 
	 */
	public void connectToArduino(){
		try {
			arduino = new ArduinoThread(arduinoHandler, _BTManager.btHandler, selected_arduinoMAC);
			arduino.start();
			ardionoOn = true;

			//sendCommandArduino("s");			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public void disconnectAduino(){
		//sendCommandArduino("f");

		new Handler().postDelayed(new Runnable(){
			public void run() {

				//If an arduino thread is running, finalize it
				if(arduino!=null)
					arduino.finalizeThread();
				/*
				new Handler().postDelayed(new Runnable(){
					public void run() {
						finish(); //Finnish whole app
					}                   
				}, 1000);
				 */
			}                   
		}, 1000);
	}

	public void pingCommand(){
		String MAC = selected_arduinoMAC;
		sendCommandArduino(MAC, "p");
	}
	public void startCommand(){
		String MAC = selected_arduinoMAC;
		sendCommandArduino(MAC, "s");
	}
	public void finalizeCommand(){
		String MAC = selected_arduinoMAC;
		sendCommandArduino(MAC, "f");
	}
	public void switchLed(){
		String MAC = selected_arduinoMAC;
		sendCommandArduino(MAC, "l");
		//sendCommandArduino("U");
	}

	/**
	 * Notifies to the Bluetooth-Manager thread's handler by the MESSAGE_SEND_COMMAND.
	 * This then sends the data to the Arduino Thread, which will finally write it to the socket.
	 * 
	 * @param str command for the Arduino
	 */
	public void sendCommandArduino(String MAC, String str) {

		if(selected_arduinoMAC != null){
			Message sendMsg = new Message();
			Bundle myDataBundle = new Bundle();
			myDataBundle.putString("COMMAND", str);
			myDataBundle.putString("MAC", selected_arduinoMAC);
			sendMsg.setData(myDataBundle);
			sendMsg.what = BTManagerThread.MESSAGE_SEND_COMMAND;
			_BTManager.btHandler.sendMessage(sendMsg);
		}
	}

	// TODO delete this function
	public String[] getBluetoothDevices(){

		String[] result = null;
		ArrayList<String> devices = new ArrayList<String>(); 
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();    
		if (mBluetoothAdapter == null) {
			// TODO Device does not support Bluetooth
		}
		if (!mBluetoothAdapter.isEnabled()){
			Log.e(TAG, "Bluetooth disabled");
			Log.v(TAG, "Asking for user permission to activate Bluetooth");
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			//TODO implement onActivityResult in main Activity


		}else{
			// Get bonded devices 
			Set<BluetoothDevice> devList = mBluetoothAdapter.getBondedDevices();

			for( BluetoothDevice device : devList)
				//TODO instead of passing the BT-devices as String[] (Name+MAC), send BluetoothDevice objects
				devices.add(device.getName() + "-"+ device.getAddress());	  	

			String[] aux_items = new String[devices.size()];
			final String[] items = devices.toArray(aux_items);
			result = items;
		}
		return result;

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
	public Handler arduinoHandler = new Handler() {
		String sendMsg;
		byte[] readBuf;
		int elapsedMilis;
		int bytes;
		String devName, devMAC;
		long timestamp;
		long msgCount, errCount;
		MessageReading msgReading;

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


				msgReading = new MessageReading(readBuf);
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

				msgReading = new MessageReading(readBuf);
				int frNum = msgReading.getFrameNum();
				Log.v(TAG,"Ping nº "+frNum+" time: "+pingTime);

				// write to log file
				sendMsg = timestamp+" "+devName+" "+pingTime;
				_Logger.logHandler.obtainMessage(LoggerThread.MESSAGE_PING, sendMsg).sendToTarget();

				//tvLdr.setText(readMessage);
				break;

			case MESSAGE_CONNECT_ARDUINO:
				// Message received from the Bluetooth-Manager Thread
				// This message implies that a request to create an Arduino Thread

				BluetoothDevice newDevice = (BluetoothDevice) msg.obj;
				Log.v(TAG, "Dispatching thread creation for "+newDevice.getName());
				BackgroundThreadDispatcher thDispatcher = new BackgroundThreadDispatcher();
				thDispatcher.execute(newDevice);

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


	// ------------------------------------------//
	// NESTED CLASS: BackgroundThreadDispatcher  //
	// ------------------------------------------//
	/**
	 * @author Xabier Gardeazabal
	 * 
	 * This performs an ArduinoThread thread creation without blocking the UI 
	 */
	private class BackgroundThreadDispatcher extends AsyncTask<BluetoothDevice, Void, String> {

		protected String doInBackground(BluetoothDevice... params) {
			Thread.currentThread().setName("ThreadDispatcher");
			ArduinoThread _newArduinoThread = null;

			BluetoothDevice newDevice = params[0];

			String devId = newDevice.getName()+"-"+newDevice.getAddress();

			//TODO check that there is no other thread connected with this device??

			try {
				//Log.v(TAG, "Trying to connect to "+devId);
				_newArduinoThread = new ArduinoThread(arduinoHandler, _BTManager.btHandler, newDevice.getAddress());
				_newArduinoThread.start();

				// Notify the Bluetooth Manager that the requested thread has been successfully created 
				_BTManager.btHandler.obtainMessage(BTManagerThread.MESSAGE_BT_THREAD_CREATED, _newArduinoThread).sendToTarget();
				return "OK";
			} catch (Exception e) {
				// Notify the Bluetooth Manager that the requested thread could not be created
				_BTManager.btHandler.obtainMessage(BTManagerThread.MESSAGE_ERROR_CREATING_BT_THREAD, newDevice).sendToTarget();
				//Log.v(TAG, "Could not create thread for "+devId);
				if(_newArduinoThread != null){
					_newArduinoThread.finalizeThread();
					e.printStackTrace();
				}
				return "ERROR";
			}
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			// Update the ListView containing the connected Arduinos
			//populateDeviceListView(); //ERROR: only the main thread/activity can manipulate the layout
		}


	}

	/**
	 * @author Xabier Gardeazabal
	 *
	 * This class builds a message according to the specified message format
	 * and retrieves the different fields 
	 */
	private class MessageReading{
		private static final String TAG = "MessageReading";

		byte stx;
		byte msgId;
		byte frameSeqNum;
		byte dlc;
		String payload;
		byte etx;
		byte crc_bytes[] = new byte[8];
		long crc;
		//private ByteBuffer auxBuffer = ByteBuffer.allocate(Long.SIZE);    

		public MessageReading(byte[] buffer) {
			int bufferIndex = 0;

			try {
				stx 				= buffer[bufferIndex++];
				msgId 				= buffer[bufferIndex++];
				frameSeqNum			= buffer[bufferIndex++];
				dlc 				= buffer[bufferIndex++];
				payload = new String(buffer, bufferIndex, --dlc);
				bufferIndex+=dlc;
				crc_bytes [0]= buffer[bufferIndex++];
				crc_bytes [1]= buffer[bufferIndex++];
				crc_bytes [2]= buffer[bufferIndex++];
				crc_bytes [3]= buffer[bufferIndex++];
				etx = buffer[bufferIndex];

				ByteBuffer auxBuffer = ByteBuffer.wrap(crc_bytes);
				auxBuffer = ByteBuffer.wrap(crc_bytes);
				auxBuffer.order(ByteOrder.LITTLE_ENDIAN);
				crc = auxBuffer.getLong();

				long checksum = getChecksum(payload.getBytes());

				if(checksum != crc)
					Log.e(TAG, "Payload contains erros: orig."+crc+" calc."+checksum);

			} catch (Exception e) {
				/* An exception should only happen if the buffer is too short and we walk off the end of the bytes.
				 * Because of the way we read the bytes from the device this should never happen, but just in case
				 * we'll catch the exception */
				Log.d(TAG, "Failure building MessageReading from byte buffer, probably an incopmplete or corrupted buffer");
				e.printStackTrace();

			}
		}

		public String getPayload() {
			return payload;
		}

		public int getFrameNum(){
			return frameSeqNum;
		}

		/**
		 * Calculates the CRC (Cyclic Redundancy Check) checksum value of the given bytes
		 * according to the CRC32 algorithm.
		 * @param bytes 
		 * @return The CRC32 checksum
		 */
		private long getChecksum(byte bytes[]){

			Checksum checksum = new CRC32();

			// update the current checksum with the specified array of bytes
			checksum.update(bytes, 0, bytes.length);

			// get the current checksum value
			long checksumValue = checksum.getValue();

			return checksumValue;
		}
	}

}
