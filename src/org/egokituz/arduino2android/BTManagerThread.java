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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.egokituz.arduino2android.activities.SettingsActivity;
import org.egokituz.arduino2android.models.TestEvent;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author Xabier Gardeazabal
 */
public class BTManagerThread extends Thread{

	private static final String TAG = "BTManager";

	//TODO REQUEST_ENABLE_BT is a request code that we provide (It's really just a number that you provide for onActivityResult)
	protected static final int MESSAGE_ERROR_CREATING_BT_THREAD = 0;
	
	public static final int MESSAGE_BT_THREAD_CREATED = 2;
	public static final int MESSAGE_SEND_COMMAND = 3;
	public static final int MESSAGE_CONNECTION_LOST = 4;
	public static final int MESSAGE_SET_SCENARIO = 5;

	/*
	public static final int STABLE_SCENARIO = 1;
	public static final int PROGRESSIVE_SCENARIO = 2;
	public static final int OPPORTUNISTIC_SCENARIO = 3;
	 */

	// Discovery plan modes: just one, continuous, or periodic
	public static final int INITIAL_DISCOVERY = 1;
	public static final int CONTINUOUS_DISCOVERY = 2;
	public static final int PERIODIC_DISCOVERY = 3;

	// Connection modes: Connect all new devices one by one (progressively) or all at once
	public static final int PROGRESSIVE_CONNECT = 1;
	public static final int ALLTOGETHER_CONNECT = 2;

	/* Connection timings: if a device has been found... 
	  	A) connect it immediately WHILE discovering, 
	 	B) stopping discovery and connect 
	 	C) connect AFTER the discovery is finished (delayed)	*/
	public static final int IMMEDIATE_WHILE_DISCOVERING_CONNECT = 1;
	public static final int IMMEDIATE_STOP_DISCOVERY_CONNECT = 2;
	public static final int DELAYED_CONNECT = 3;

	//private int currentScenario = 0;
	private int discoveryPlan = 0;
	private int connectionMode = 0;
	private int connectionTiming = 0;

	private long discoveryInterval = 30000; // Interval between delayed discoveries (in milliseconds) Default 30 seconds
	private long lastDiscoveryTimestamp;
	
	private Handler mainHandler;
	private Context mainCtx;

	private ArduinoPlannerThread _plannerThread;

	/**
	 * The Bluetooth adapter of the device
	 */
	private BluetoothAdapter _BluetoothAdapter;

	/**
	 *  List of currently connected Arduinos (with an independent thread for each)
	 *  private List<BTDeviceThread> myArduinos; 
	 */
	private Map<String,ArduinoThread> myArduinoThreads;

	/**
	 * New discovered bluetooth devices list
	 */
	private List<BluetoothDevice> newDevicesList;

	/**
	 * Arduino devices ready to be paired
	 */
	private Map<String,BluetoothDevice> connectableArduinos;

	/**
	 * List of ignored devices that should never be used 
	 */
	private Map<String,BluetoothDevice> ignoredDevicesList;

	/**
	 * List of devices waiting to be connected (a connection request has been dispatched)
	 */
	private Map<String,BluetoothDevice> waitingToBeConnected;

	/**
	 * New BTManagerThread that manages the phone's Bluetooth antenna.
	 * It also manages the connection state of the connected devices' threads
	 * For each device discovered, it launches an "ArduinoThread" thread
	 * @param context
	 * @param handler
	 * @throws Exception 
	 */
	public BTManagerThread(Context context, Handler handler) throws Exception {
		this.setName(TAG);
		Log.v(TAG, "BTManagerThread Constructor start");

		mainHandler = handler;
		mainCtx = context;

		myArduinoThreads = new HashMap<String,ArduinoThread>();
		newDevicesList = new ArrayList<BluetoothDevice>();
		connectableArduinos = new HashMap<String,BluetoothDevice>();
		ignoredDevicesList = new HashMap<String,BluetoothDevice>();
		waitingToBeConnected = new HashMap<String,BluetoothDevice>();
		
		// Get the Bluetooth adapter of this device
		_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		// Check if this device supports Bluetooth
		if (_BluetoothAdapter == null) {
			// TODO Device does not support Bluetooth
			throw new Exception("This device does not support Bluetooth");
		}

		// create activity asking the user to turn on Bluetooth if it is not already enabled
		requestBluetoothEnable();

		registerBroadcastReceivers();
	}

	/**
	 * Registers the desired intent filters to the {@link BroadcastReceiver} of the main context
	 */
	private void registerBroadcastReceivers() {
		// Register the BroadcastReceivers
		// When finalizing, remember to remove the ones that are not needed!
		IntentFilter filter0 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		IntentFilter filter4 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		IntentFilter filter5 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		IntentFilter filter6 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

		mainCtx.registerReceiver(myReceiver, filter0);
		mainCtx.registerReceiver(myReceiver, filter1);
		mainCtx.registerReceiver(myReceiver, filter2);
		mainCtx.registerReceiver(myReceiver, filter3);
		mainCtx.registerReceiver(myReceiver, filter6);
		mainCtx.registerReceiver(myReceiver, filter4);
		mainCtx.registerReceiver(myReceiver, filter5);
	}

	/**
	 * This method starts a new activity that prompts the user to enable Bluetooth
	 * @throws Exception This device does not support Bluetooth
	 */
	private void requestBluetoothEnable() throws Exception {
		// If Bluetooth is not already enabled, prompt and ask the ser to enable it
		if (!_BluetoothAdapter.isEnabled()){
			Log.e(TAG, "Bluetooth disabled");
			Log.v(TAG, "Asking for user permission to activate Bluetooth");
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mainCtx.startActivity(enableBtIntent); // Start a new activity to turn Bluetooth ON
			
			//((Activity) mainCtx).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			//TODO implement onActivityResult in main Activity
		}
	}

	/**
	 * Handler for receiving commands from the Activities
	 */
	public Handler btHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			ArduinoThread arduinoTh;
			Bundle mBundle;
			String devName, devId, devMAC;
			BluetoothDevice btDevice;

			switch (msg.what) {
			case MESSAGE_BT_THREAD_CREATED:
				// Message received from Main Activity
				// This message implies that the Main Activity has created the requested thread

				arduinoTh = (ArduinoThread) msg.obj;
				btDevice = arduinoTh.getBluetoothDevice();
				devName = btDevice.getName();
				devMAC = btDevice.getAddress();
				devId = devName+"-"+devMAC;
				waitingToBeConnected.remove(devId);
				myArduinoThreads.put(devId, arduinoTh);
				Log.v(TAG, "Thread created for "+devName);

				// Check the connection mode (in case it is progressive, now's the time to request the connection for the next connectable device) 
				if(connectionMode == PROGRESSIVE_CONNECT && connectableArduinos.size()>0){
					_plannerThread.connectAvalaiableArduinos();
				}
				
				if(waitingToBeConnected.size()==0)
					startDiscoveryIfPossible();

				break;

			case MESSAGE_ERROR_CREATING_BT_THREAD:
				// Message received from Main Activity
				// This message implies that the Main Activity could not create the requested thread

				//TODO what should be done with an Arduino that cannot be connected?
				// try to recover from the error
				btDevice = (BluetoothDevice) msg.obj;
				devName = btDevice.getName();
				devMAC = btDevice.getAddress();
				devId = devName+"-"+devMAC;
				waitingToBeConnected.remove(devId);

				// Check the connection mode (in case it is progressive, now's the time to request the connection for the next connectable device) 
				if(connectionMode == PROGRESSIVE_CONNECT && connectableArduinos.size()>0){
					_plannerThread.connectAvalaiableArduinos();
				}
				
				if(waitingToBeConnected.size()==0)
					startDiscoveryIfPossible();


				break;

			case MESSAGE_CONNECTION_LOST:
				// Message received from a running Arduino Thread
				// This message implies that an error occurred while reading or writing to the socket

				mBundle = msg.getData();
				devName = mBundle.getString("NAME");
				devMAC =  mBundle.getString("MAC");
				arduinoTh = (ArduinoThread) msg.obj;
				devId = devName+"-"+devMAC;

				_plannerThread.arduinoErrorRecovery(devId, arduinoTh);

				break;

			case MESSAGE_SEND_COMMAND:
				// Message received from the Main Activiy
				// This message implies a request to write to the socket of a running Arduino

				mBundle = msg.getData();
				String command = mBundle.getString("COMMAND");
				devMAC =  mBundle.getString("MAC");

				sendCommandToArduino(devMAC, command);

				break;

			case MESSAGE_SET_SCENARIO:
				HashMap<String, Integer> test_parameters = (HashMap<String, Integer>) msg.obj; // hash-map containing the test parameters defined through the preferences of the app
				
				Integer aux = test_parameters.get(SettingsActivity.PREF_DISCOVERY_PLAN);
				if(aux != null)
					discoveryPlan = aux;
				aux = test_parameters.get(SettingsActivity.PREF_CONNECTION_TIMING);
				if(aux != null)
					connectionTiming = aux;
				aux = test_parameters.get(SettingsActivity.PREF_CONNECTION_MODE);
				if(aux != null)
					connectionMode = aux;
				test_parameters.get(SettingsActivity.PREF_DISCOVERY_INTERVAL);
				if(aux != null)
					discoveryInterval = aux;
			}
		}
	};


	/** 
	 * BroadcastReceiver that listens to:
	 * Device discovery, connection and disconnection (ACTION_FOUND, ACTION_ACL_CONNECTED and ACTION_ACL_DISCONNECTED)
	 * Discovery start and end: ACTION_DISCOVERY_STARTED and ACTION_DISCOVERY_FINISHED.
	 */
	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			BluetoothDevice device;
			String deviceName;
			String devId;
			boolean newArduinoAvalable;
			long timestamp = System.currentTimeMillis();
			String msg;
			TestEvent event = new TestEvent();
			event.timestamp = timestamp;

			switch (action){
			case BluetoothDevice.ACTION_FOUND:

				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				devId= device.getName()+"-"+device.getAddress();

				if(!newDevicesList.contains(device) && !ignoredDevicesList.containsKey(devId)){
					Log.v(TAG, "New device found: "+devId);

					synchronized (newDevicesList) {
						newDevicesList.add(device);
					}

					switch (connectionTiming) {
					case IMMEDIATE_STOP_DISCOVERY_CONNECT:

						_plannerThread.fetchDevices();
						_plannerThread.connectAvalaiableArduinos();

						break;
					case IMMEDIATE_WHILE_DISCOVERING_CONNECT:
						// While discovering, fetch new devices
						Log.v(TAG, "Fetching while discovering... ");
						_plannerThread.fetchDevices();
						_plannerThread.connectAvalaiableArduinos();
						break;
					case DELAYED_CONNECT:
						// Do nothing (when discovery is finished the fetching will be done)
						//Log.v(TAG, "The devices will be fetched when discovery is finished.");
						break;
					}
				}


				break;
			case BluetoothDevice.ACTION_ACL_CONNECTED:
				//Low-level (ACL) connection has been established with a remote BT device

				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				deviceName = device.getName();				

				// Notify the main activity about the connection:
				msg = timestamp+" "+deviceName+"-connected";
				

				event.eventID = TestEvent.EVENT_NEW_DEVICE_CONNECTED;
				event.source = deviceName;
				mainHandler.obtainMessage(TestApplication.MESSAGE_BT_EVENT, event).sendToTarget();

				Log.v(TAG, "connection established with "+deviceName);
				break;
				/*
			case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
				//TODO ACL disconnection has been requested for a remote device, and it will soon be disconnected. 
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				deviceName = device.getName()+"-"+device.getAddress();
				Log.v(TAG, "Disconnect requested "+deviceName);

				break;
				 */
			case BluetoothDevice.ACTION_ACL_DISCONNECTED:
				// When a remote device has been disconnected, discard it
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				deviceName = device.getName();
				Log.v(TAG, "Disconnected "+deviceName);

				// Notify the main activity about the disconnection:
				event.eventID = TestEvent.EVENT_DEVICE_DISCONNECTED;
				event.source = deviceName;
				mainHandler.obtainMessage(TestApplication.MESSAGE_BT_EVENT, event).sendToTarget();

				finalizeArduinoThread(device.getAddress());
				devId = deviceName+"-"+device.getAddress();
				myArduinoThreads.remove(devId);

				break;

			case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
				// When discovery is started, notify the main activity
				Log.v(TAG, "discovery started");

				event.eventID = TestEvent.EVENT_NEW_DISCOVERY_STARTED;
				mainHandler.obtainMessage(TestApplication.MESSAGE_BT_EVENT, event).sendToTarget();
				break;

			case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
				//TODO When discovery is finished
				Log.v(TAG, "discovery is finished");
				
				event.eventID = TestEvent.EVENT_NEW_DISCOVERY_STARTED;
				mainHandler.obtainMessage(TestApplication.MESSAGE_BT_EVENT, event).sendToTarget();

				if(connectionTiming == DELAYED_CONNECT){
					_plannerThread.fetchDevices();
					_plannerThread.connectAvalaiableArduinos();
				}

				startDiscoveryIfPossible();
				break;

				/*
			case BluetoothAdapter.ACTION_STATE_CHANGED:
				final int currentState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				//TODO check if PREVIOUS_STATE is useful for something

				switch (currentState) {
				case BluetoothAdapter.STATE_ON:
					//TODO Bluetooth Adapter was turned ON (enabled)
					Log.v(TAG, "Bluetooth has been enabled");

					break;
				case BluetoothAdapter.STATE_OFF:
					//TODO  Bluetooth Adapter was turned OFF (disabled)
					Log.v(TAG, "Warning: Bluetooth has been disabled");

					break;
				case BluetoothAdapter.STATE_TURNING_ON:
					//TODO check if this is necessary
					Log.v(TAG, "Bluetooth is turning on");

					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
					//TODO Bluetooth adapter is turning off. 
					//Local clients should immediately attempt graceful disconnection of any remote links
					Log.v(TAG, "Warning: Bluetooth is turning off");

					break;
				} */
			} // switch-case broadcast
		} // onReceive()


	}; // new() myReceiver 

	
	private void startDiscoveryIfPossible() {

		switch (discoveryPlan) {
		case INITIAL_DISCOVERY:
			// Do nothing, since the initial discovery has already been done
			//Log.v(TAG, "No more discoveries will be done.");
			break;
		case CONTINUOUS_DISCOVERY:
			// Start a new discovery immediately, unless a conn. timing is not while discovering, and a connection is being done
			if(connectionTiming != IMMEDIATE_WHILE_DISCOVERING_CONNECT){
				if(!(waitingToBeConnected.size() > 0)){
					_BluetoothAdapter.startDiscovery(); // newDevicesList is updated when ACTION_FOUND
					lastDiscoveryTimestamp = System.currentTimeMillis();
				}
			}else{
				_BluetoothAdapter.startDiscovery(); // newDevicesList is updated when ACTION_FOUND
				lastDiscoveryTimestamp = System.currentTimeMillis();
			}
			break;
		case PERIODIC_DISCOVERY:
			Runnable postRunnable = new Runnable() {
				@Override
				public void run() {
					_BluetoothAdapter.startDiscovery(); // newDevicesList is updated when ACTION_FOUND
					lastDiscoveryTimestamp = System.currentTimeMillis();
				}
			};
			//btHandler.removeCallbacks(postRunnable);
			btHandler.postDelayed(postRunnable, discoveryInterval);

			break;
		}
	}


	public String[] getConnectedArduinos(){
		String[] result = null;
		ArrayList<String> devices = new ArrayList<String>(); 

		for(String devId : myArduinoThreads.keySet()){
			ArduinoThread dev = myArduinoThreads.get(devId);
			if(dev.isAlive() && dev.isConnected())
				devices.add(devId);
		}
		result = (String[]) devices.toArray(new String[devices.size()]);
		return result;
	}

	/**
	 * Send messages to the class from we received the myHandler
	 * @param code
	 * @param value
	 */
	public void sendMessage(String code, String value){
		Message msg = new Message();
		Bundle myDataBundle = new Bundle();
		myDataBundle.putString(code,value);
		msg.setData(myDataBundle);
		mainHandler.sendMessage(msg);  
	}

	/**
	 * Stops the thread in a safe way
	 */
	public void finalize(){
		Log.v(TAG, "finalize()");
		//TODO poner a null receivers

		//Finalize Arduino threads
		for(ArduinoThread th : myArduinoThreads.values()){
			th.finalizeThread();
		}
		
		//mainHandler.removeCallbacksAndMessages (null); 

		Log.v(TAG, "Unregistering receiver");
		try {
			mainCtx.unregisterReceiver(myReceiver);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.v(TAG, "receiver unregistered");

		exit_condition = true;
		try {
			_plannerThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void finalizeArduinoThread(String devId) {
		ArduinoThread th = myArduinoThreads.remove(devId);
		if(th != null){
			if(th.getDeviceMAC().contentEquals(devId)){
				Log.v(TAG, "Finalizing thread for "+th.getDeviceName());
				th.finalizeThread();
			}
		}
	}

	@Override
	public synchronized void start() {
		Log.v(TAG, "BTManager starting...");
		exit_condition = false;
		super.start();

	}

	@Override
	public void run() {
		super.run();

		Log.v(TAG, "BTManager run()");

		_plannerThread = new ArduinoPlannerThread((BTManagerThread) this);
		_plannerThread.start();

		// Get bonded devices (only the first time) 		
		//Set<BluetoothDevice> bondedDevices = _BluetoothAdapter.getBondedDevices();
		//newDevicesList.addAll(bondedDevices);

		long pingInterval = 99;
		while(!exit_condition){

			// TODO consider using a POST-DELAYED run
			pingAll();

			/*
			mainHandler.postDelayed( new Runnable() {
				@Override
				public void run() {
					pingAll();
				}
			}, pingInterval);
			 */

			try {
				Thread.sleep(pingInterval);
			} catch (InterruptedException e) { e.printStackTrace(); }
		}
	}

	private void pingAll(){

		synchronized (myArduinoThreads) {
			for (ArduinoThread th : myArduinoThreads.values()) {
				sendCommandToArduino(th.getDeviceMAC(), "p");
			}
		}
	}

	private void sendCommandToArduino(String devMAC, String command){
		for(ArduinoThread th : myArduinoThreads.values()){
			if(th.isConnected())
				if(th.getDeviceMAC().equals(devMAC)){
					Message sendMsg = new Message();
					Bundle myDataBundle = new Bundle();
					myDataBundle.putString("COMMAND", command);
					sendMsg.setData(myDataBundle);
					// Obtain the handler from the Thread and send the command in a Bundle
					((ArduinoThread) th).arduinoHandler.sendMessage(sendMsg);
				}
		}
	}


	//--------------
	//NESTED CLASS: ArduinoPlannerThread 
	//--------------

	private boolean exit_condition;

	/**
	 * @author Xabier Gardeazabal
	 *
	 * This class represents the Planner, and manages the timing of the following phases: 
	 * +Bluetooth discovery plans (initial, continuous & periodic)
	 * +Arduino devices' connection setup (one by one, or alltogether)
	 * +Connected Arduino devices' error handling  
	 */
	private class ArduinoPlannerThread extends Thread {
		BTManagerThread BTmgr;

		/**
		 * @param btMngr BTManagerThread 
		 */
		private ArduinoPlannerThread(BTManagerThread btMngr) {
			this.setName("ArduinoPlanner");
			this.BTmgr = btMngr;
		}

		public void run() {

			// Initial discovery (there's always at least one initial discovery)
			_BluetoothAdapter.startDiscovery(); // newDevicesList is updated when ACTION_FOUND
			lastDiscoveryTimestamp = System.currentTimeMillis();

			long now = lastDiscoveryTimestamp;
			
			// Loop to prevent the thread from finalizing and not answering to calls
			while (!exit_condition){
				try {

					// Discovery control block, in case the Broadcast-Receiver missed a state-change
					now = System.currentTimeMillis();
					if(now-lastDiscoveryTimestamp>discoveryInterval*2)
						startDiscoveryIfPossible();

					Thread.sleep(999);
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
		}


		/**
		 * Checks if new devices have been found by the last Bluetooth discovery.
		 * For each new device, checks if its name happens to meet with our Arduinos.
		 * If it does, it stores it as connectable, for later use.
		 * If it does not, the device is ignored.
		 * 
		 * @return true if a new Arduino is connectable
		 */
		public boolean fetchDevices(){
			boolean result = false;

			// Select which devices are our Arduinos, and put them in the connectableArduinos list
			synchronized (newDevicesList) {
				if (newDevicesList.size() > 0) {
					BluetoothDevice device;
					Iterator<BluetoothDevice> myIterator = newDevicesList.iterator();

					while (myIterator.hasNext()) {
						device = myIterator.next();
						String deviceId = device.getName()+"-"+device.getAddress();

						// Proceed only if the device is one of our Arduinos
						if (deviceId.contains("ROBOTICA_")) {

							// Discard Arduinos that are already connected, ignored or due for connection 
							if (!myArduinoThreads.containsKey(deviceId) 
									&& !ignoredDevicesList.containsKey(deviceId)
									&& !connectableArduinos.containsKey(deviceId)) {
								synchronized (connectableArduinos) {
									connectableArduinos.put(deviceId, device);
								}
								Log.v(TAG, "New connectable arduino: "+deviceId);
								result = true;
							}
						} else if (!ignoredDevicesList.containsKey(deviceId)) {
							// Put device in the Black-List, as it does not have any interest to us
							//Log.v(TAG, "Ignoring device " + deviceId);
							ignoredDevicesList.put(deviceId, device);
						}
					}
					newDevicesList.clear();
				}
			}// synchronized-end
			return result;
		}

		/**
		 * If 'connectionMode' is PROGRESSIVE_CONNECT: requests a connection with ONLY the next connectable Arduino device 
		 * 	(if there are more connectable devices left, when the handler receives a MESSAGE_BT_THREAD_CREATED
		 * 	or MESSAGE_ERROR_CREATING_BT_THREAD, the handler will call this function)
		 * 
		 * If 'connectionMode' is ALLTOGETHER_CONNECT: it requests a connection with EACH and EVERY connectable Arduino device
		 */
		public void connectAvalaiableArduinos(){

			if(connectableArduinos.size()>0){
				if(myArduinoThreads.size()<7){

					if(connectionTiming != IMMEDIATE_WHILE_DISCOVERING_CONNECT){
						// Stop discovery and fetch new devices immediately
						Log.v(TAG, "Canceling discovery and requesting connection");
						_BluetoothAdapter.cancelDiscovery();
					}

					switch (connectionMode) {
					case PROGRESSIVE_CONNECT:
						// Request the connection with JUST ONE arduino device
						if(connectableArduinos.size()>0){
							BluetoothDevice arduino = connectableArduinos.values().iterator().next();
							String devId = arduino.getName()+"-"+arduino.getAddress();
							waitingToBeConnected.put(devId, arduino);

							//mainHandler.obtainMessage(MainActivity.MESSAGE_CONNECT_ARDUINO,arduino).sendToTarget();

							//Log.v(TAG, "Progressive connect || Requesting thread conn. for " + arduino.getName());
							BackgroundThreadDispatcher thDispatcher = new BackgroundThreadDispatcher();
							thDispatcher.execute(arduino);


							synchronized (connectableArduinos) {
								connectableArduinos.remove(devId);
							}
						}

						break;
					case ALLTOGETHER_CONNECT:
						// Request the connection with EACH and EVERY connectable Arduino device

						synchronized (connectableArduinos) {
							for (BluetoothDevice arduino : connectableArduinos.values()) {
								String devId = arduino.getName()+"-"+arduino.getAddress();
								waitingToBeConnected.put(devId, arduino);

								//mainHandler.obtainMessage(MainActivity.MESSAGE_CONNECT_ARDUINO,arduino).sendToTarget();

								Log.v(TAG,"All-together connect || Requesting thread conn. for "+ arduino.getName());
								BackgroundThreadDispatcher thDispatcher = new BackgroundThreadDispatcher();
								thDispatcher.execute(arduino);
							}
							connectableArduinos.clear();
						}

						break;
					}
				} else{
					Log.v(TAG, "Too many connections are open already.");
				}
			}else{
				Log.v(TAG, "There isn't any new arduino");
			}
		}

		/**
		 * Decides what should be done when a device misbehaves
		 * @param arduinoTh 
		 * @param devMAC 
		 * @param MAC: the address of the buggy Bluetooth-Arduino device 
		 */
		public void arduinoErrorRecovery(String devId, ArduinoThread arduinoTh){
			// 1. Try to reconnect the socket

			if(devId!=null)
				finalizeArduinoThread(devId);
		}
	}

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
				_newArduinoThread = new ArduinoThread(mainHandler, btHandler, newDevice);
				_newArduinoThread.start();

				// Notify the Bluetooth Manager that the requested thread has been successfully created 
				btHandler.obtainMessage(BTManagerThread.MESSAGE_BT_THREAD_CREATED, _newArduinoThread).sendToTarget();
				return "OK";
			} catch (Exception e) {
				// Notify the Bluetooth Manager that the requested thread could not be created
				btHandler.obtainMessage(BTManagerThread.MESSAGE_ERROR_CREATING_BT_THREAD, newDevice).sendToTarget();
				//Log.v(TAG, "Could not create thread for "+devId);
				if(_newArduinoThread != null){
					_newArduinoThread.finalizeThread();
					//e.printStackTrace();
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

}


