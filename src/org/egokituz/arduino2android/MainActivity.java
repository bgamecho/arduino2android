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
	
	public static final String TOAST = "toast";

	Button refreshButton, connectButton, disconnectButton,startButton,finButton;
	Spinner spinnerBluetooth;
	ListView devicesListView;
	Button ledOn;
	TextView tvLdr;

	private String selected_arduinoMAC;
	
	private Float batteryLoad;

	private BTManagerThread _BTManager;
	private BatteryMonitorThread _BatteryMonitor;
	private LoggerThread _Logger;

	private ArduinoThread arduino;
	boolean ardionoOn;

	public boolean finishApp; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ardionoOn = false;
		finishApp = false;

		setButtons();
		populateDeviceListView();
		updateSpinner();

		_BTManager = new BTManagerThread(this, arduinoHandler);
		_BatteryMonitor = new BatteryMonitorThread(this, arduinoHandler);
		_Logger = new LoggerThread(this, arduinoHandler);

	}

	private void setButtons() {
		// TODO Auto-generated method stub

		refreshButton = (Button) findViewById(R.id.buttonRefresh);		
		refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				updateSpinner();
				Log.v(TAG, "Arduino Activity updateSpinner");
			}
		});

		spinnerBluetooth = (Spinner) findViewById(R.id.spinnerBluetooth);
		spinnerBluetooth.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				//				updateSpinner();
				//				Log.v(TAG, "Arduino Activity updateSpinner");
				return false;
			}
		});
		spinnerBluetooth.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				//TODO instead of passing the BT-device as String (Name+MAC), send a BluetoothDevice object
				String myMAC = spinnerBluetooth.getSelectedItem().toString();
				selected_arduinoMAC = myMAC.substring(myMAC.length()-17);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}

		});

		connectButton = (Button) findViewById(R.id.buttonConnect);
		connectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//connectToArduino();
			}
		});

		disconnectButton = (Button) findViewById(R.id.buttonDisconnect);
		disconnectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//disconnectAduino();
			}
		});

		startButton = (Button) findViewById(R.id.buttonStart);
		startButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startCommand();
			}
		});

		finButton =(Button) findViewById(R.id.buttonFin);
		finButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finalizeCommand();
			}
		});

		ledOn = (Button) findViewById(R.id.buttonLedOn);
		ledOn.setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						switchLed();
					}
				});

		tvLdr = (TextView) findViewById(R.id.textViewLDRvalue);

	}

	@Override 
	public void onStart(){
		Log.v(TAG, "Arduino Activity --OnStart()--");
		if(!_BTManager.isAlive())
			_BTManager.start();
		if(!_BatteryMonitor.isAlive())
			_BatteryMonitor.start();
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
				updateSpinner();
				break;
			case RESULT_CANCELED:
				Log.v(TAG, "User  did not enable Bluetooth");
				this.spinnerBluetooth.setSelected(false);
				this.spinnerBluetooth.setClickable(false);
				break;
			}
		}
	}

	/**
	 * Updates the ListView containing the connected Arduinos
	 */
	private void populateDeviceListView() {
		devicesListView = (ListView) findViewById(R.id.listViewDevices);

		final String[] myDeviceList = getConnectedDevices();

		if(myDeviceList != null){
			ArrayAdapter<String> listViewArrayAdapter = new ArrayAdapter<String>(this, 
					android.R.layout.simple_list_item_1, myDeviceList);
			devicesListView.setAdapter(listViewArrayAdapter);
		}



	}

	//Updates the items of the Bluetooth devices' spinner
	private void updateSpinner(){
		String[] myDeviceList = this.getBluetoothDevices();

		if(myDeviceList!=null){
			

	
		for(int i=0;i<myDeviceList.length; i++){
			if(!myDeviceList[i].startsWith("BT-") || !myDeviceList[i].startsWith("ROB") )
				myDeviceList[i].length();
		}

		if(myDeviceList != null){
			ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, 
					android.R.layout.simple_spinner_item, myDeviceList);
			spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
			spinnerBluetooth.setAdapter(spinnerArrayAdapter);
			spinnerBluetooth.setSelection(getIndex(spinnerBluetooth, "BT-"));
			this.spinnerBluetooth.setClickable(true);
		}else
			this.spinnerBluetooth.setClickable(false);
		
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

	public void startCommand(){
		sendCommandArduino("s");
	}
	public void finalizeCommand(){
		sendCommandArduino("f");
	}
	public void switchLed(){
		sendCommandArduino("l");
		//sendCommandArduino("U");
	}

	/**
	 * Notifies to the Bluetooth-Manager thread's handler by the MESSAGE_SEND_COMMAND.
	 * This then sends the data to the Arduino Thread, which will finally write it to the socket.
	 * 
	 * @param str command for the Arduino
	 */
	public void sendCommandArduino(String str) {

		if(selected_arduinoMAC != null){
			Message sendMsg = new Message();
			Bundle myDataBundle = new Bundle();
			myDataBundle.putString("COMMAND", str);
			myDataBundle.putString("MAC", selected_arduinoMAC);
			sendMsg.setData(myDataBundle);
			sendMsg.what = BTManagerThread.MESSAGE_SEND_COMMAND;
			//			try {
			//				Thread.sleep(50);
			//			} catch (InterruptedException e) {
			//				// TODO Auto-generated catch block
			//				e.printStackTrace();
			//			}
			// Obtain the handler from the Thread and send the command in a Bundle
			_BTManager.btHandler.sendMessage(sendMsg);
			//arduino.getHandler().sendMessage(sendMsg);
			//Log.v(TAG, "Command "+str+" sent");

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


	public void modifyText(String myLdr){
		tvLdr.setText(myLdr);
	}

	/**
	 * Handler connected with the BTManager Threads: 
	 */
	public Handler arduinoHandler = new Handler() {

		@SuppressLint("NewApi")
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case MESSAGE_READ:
				// Message received from a running Arduino Thread
				// This message implies that a well formed message was read by an Arduino Thread
				
				byte[] readBuf = (byte[]) msg.obj;
				int elapsedMilis = msg.arg1;
				int bytes = msg.arg2;
				String devName =  msg.getData().getString("NAME");
				String devMAC =  msg.getData().getString("MAC");
				String timestamp = msg.getData().getString("TIMESTAMP");

				MessageReading msgReading = new MessageReading(readBuf);
				
				// TODO write to log file
				String logMsg = timestamp+" "+devName+" "+elapsedMilis+"ms "+bytes+" bytes";
				_Logger.logHandler.obtainMessage(LoggerThread.MESSAGE_WRITE_TO_LOG_FILE, logMsg).sendToTarget();
				
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
				// This message implies that a the Battery percentage has changed
				
				batteryLoad = (Float) msg.obj;
				
				//TODO call the Logger to write the battery load
				
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
				Log.v(TAG, "Trying to connect to "+devId);
				_newArduinoThread = new ArduinoThread(arduinoHandler, _BTManager.btHandler, newDevice.getAddress());
				_newArduinoThread.start();
				
				// Notify the Bluetooth Manager that the requested thread has been successfully created 
				_BTManager.btHandler.obtainMessage(BTManagerThread.MESSAGE_BT_THREAD_CREATED, _newArduinoThread).sendToTarget();
				return "OK";
			} catch (Exception e) {
				// Notify the Bluetooth Manager that the requested thread could not be created
				_BTManager.btHandler.obtainMessage(BTManagerThread.MESSAGE_ERROR_CREATING_BT_THREAD, newDevice).sendToTarget();
				Log.v(TAG, "Could not create thread for "+devId);
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
			//populateDeviceListView();
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
				e.printStackTrace();
				Log.d(TAG, "Failure building MessageReading from byte buffer, probably an incopmplete or corrupted buffer");
			}
		}

		public String getPayload() {
			return payload;
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
