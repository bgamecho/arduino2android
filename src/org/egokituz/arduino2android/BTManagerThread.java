/**
 * This class manages the phone's Bluetooth antenna
 * For each device discovered, it launches an "ArduinoThread" thread 
 */
package org.egokituz.arduino2android;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author Xabier Gardeazabal
 *
 */
public class BTManagerThread extends Thread{

	private static final String TAG = "BTManager";

	//TODO REQUEST_ENABLE_BT is a request code that we provide (It's really just a number that you provide for onActivityResult)
	protected static final int MESSAGE_ERROR_CREATING_BT_THREAD = 0;
	private static final int REQUEST_ENABLE_BT = 1;
	public static final int MESSAGE_BT_THREAD_CREATED = 2;
	public static final int MESSAGE_SEND_COMMAND = 3;
	public static final int MESSAGE_CONNECTION_LOST = 4;

	private Handler mainHandler;
	private Context mainCtx;

	private ArduinoPlannerThread _discoveryThread;

	private final BluetoothAdapter _BluetoothAdapter;

	// List of currently connected Arduinos (with an independent thread for each) 
	//private List<BTDeviceThread> myArduinos;
	private Map<String,BTDeviceThread> myArduinoThreads;

	// New discovered bluetooth devices list
	private List<BluetoothDevice> newDevicesList;

	// Arduino devices ready to be paired
	private Map<String,BluetoothDevice> connectableArduinos;

	// List of ignored devices that should never be used 
	private Map<String,BluetoothDevice> ignoredDevicesList;

	public static BufferedWriter out;

	public BTManagerThread(Context context, Handler handler) {
		Log.v(TAG, "BTManagerThread Constructor start");

		mainHandler = handler;
		mainCtx = context;

		//set log to write/append file
		try {
			createFileOnDevice(false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//TODO set BLUETOOTH
		_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();    
		if (_BluetoothAdapter == null) {
			// TODO Device does not support Bluetooth
		}
		if (!_BluetoothAdapter.isEnabled()){
			Log.e(TAG, "Bluetooth disabled");
			Log.v(TAG, "Asking for user permission to activate Bluetooth");
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			((Activity) mainCtx).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			//TODO implement onActivityResult in main Activity
		}

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

	public void writeToFile(String message){
		try {
			createFileOnDevice(true);
			out.write(message+"\n");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function to initially create the log file and it also writes the time of creation to file.
	 */
	private void createFileOnDevice(Boolean append) throws IOException {

		File Root = Environment.getExternalStorageDirectory();
		if(Root.canWrite()){
			File  LogFile = new File(Root, "LogXABI.txt");
			FileWriter LogWriter = new FileWriter(LogFile, append);
			out = new BufferedWriter(LogWriter);
			Date date = new Date();
			out.write("Logged at" + String.valueOf(date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds() + "\n"));
			//TODO remember to call out.close() or otherwise it won't write to the file
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
			String devId;
			String MAC;
			BluetoothDevice btDevice;


			switch (msg.what) {
			case MESSAGE_BT_THREAD_CREATED:
				arduinoTh = (ArduinoThread) msg.obj;
				btDevice = arduinoTh.getBluetoothDevice(); 
				devId = btDevice.getName()+"-"+btDevice.getAddress();
				myArduinoThreads.put(devId, arduinoTh);
				Log.v(TAG, "Thread was successfully created");
				break;
			case MESSAGE_SEND_COMMAND:
				mBundle = msg.getData();
				String command = mBundle.getString("COMMAND");
				MAC =  mBundle.getString("MAC");

				for(BTDeviceThread th : myArduinoThreads.values()){
					if(th.getDeviceMAC().equals(MAC)){
						Message sendMsg = new Message();
						Bundle myDataBundle = new Bundle();
						myDataBundle.putString("COMMAND", command);
						sendMsg.setData(myDataBundle);
						// Obtain the handler from the Thread and send the command in a Bundle
						((ArduinoThread) th).getHandler().sendMessage(sendMsg);
					}
				}
				break;
			case MESSAGE_CONNECTION_LOST:
				mBundle = msg.getData();
				MAC =  mBundle.getString("MAC");
				_discoveryThread.arduinoErrorRecovery(MAC);

				break;
			case MESSAGE_ERROR_CREATING_BT_THREAD:
				//TODO what should be done with an Arduino that cannot be connected?
				// try to recover from the error
				btDevice = (BluetoothDevice) msg.obj;
				MAC = btDevice.getAddress();
				_discoveryThread.arduinoErrorRecovery(MAC);

			default:
				break;
			}
		}

	};

	// The BroadcastReceiver that listens for discovered devices and connected devices
	// It also listens for connection, discovery and adapter state changes 
	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			BluetoothDevice device;
			String deviceName;
			String devId;
			switch (action){
			case BluetoothDevice.ACTION_FOUND:
				//TODO When discovery finds a device
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				deviceName = device.getName()+"-"+device.getAddress();
				if(!newDevicesList.contains(device) && !ignoredDevicesList.containsKey(deviceName)){

					synchronized (newDevicesList) {
						newDevicesList.add(device);
					}
					Log.v(TAG, "New device found: "+deviceName);
				}

				break;
			case BluetoothDevice.ACTION_ACL_CONNECTED:
				//TODO Low-level (ACL) connection has been established with a remote BT device
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				deviceName = device.getName()+"-"+device.getAddress();
				Log.v(TAG, "connection established with "+deviceName);

				break;
			case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
				//TODO ACL disconnection has been requested for a remote device, and it will soon be disconnected. 
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				deviceName = device.getName()+"-"+device.getAddress();
				Log.v(TAG, "Disconnect requested "+deviceName);

				break;
			case BluetoothDevice.ACTION_ACL_DISCONNECTED:
				//TODO When a remote device has been disconnected, discard it
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				deviceName = device.getName()+"-"+device.getAddress();
				Log.v(TAG, "Disconnected "+deviceName);
				finalizeArduinoThread(device.getAddress());
				devId = deviceName+"-"+device.getAddress();
				myArduinoThreads.remove(devId);

				break;

			case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
				//TODO When discovery is started
				Log.v(TAG, "discovery started");
				break;
			case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
				//TODO When discovery is finished
				Log.v(TAG, "discovery is finished");
				boolean newArduinoAvalable = _discoveryThread.fetchDevices();
				if(newArduinoAvalable)
					_discoveryThread.connectAvalaiableArduinos();

				break;
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
				}
			}
		}
	};

	public String[] getConnectedArduinos(){
		String[] result = null;
		ArrayList<String> devices = new ArrayList<String>(); 

		for(String devId : myArduinoThreads.keySet()){
			BTDeviceThread dev = myArduinoThreads.get(devId);
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

	public void finalize(){
		Log.v(TAG, "finalize()");
		//TODO poner a null receivers

		//Finalize Arduino threads
		for(BTDeviceThread th : myArduinoThreads.values()){
			th.finalizeThread();
		}

		Log.v(TAG, "Unregistering receiver");
		mainCtx.unregisterReceiver(myReceiver);
		Log.v(TAG, "receiver unregistered");

		try {
			Log.v(TAG, "Writing to log...");
			out.close(); //TODO until out.close() the log messages won't be written to the file
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		exit_condition = true;
	}

	private void finalizeArduinoThread(String MAC) {
		for(BTDeviceThread th : myArduinoThreads.values()){
			String devId = th.getDeviceName()+"-"+th.getDeviceMAC();
			if(devId.contains(MAC)){
				Log.v(TAG, "Finalizing thread for "+devId);
				th.finalizeThread();
			}
		}
	}

	private boolean isBTReady(){
		Log.v(TAG, "isBTReady()");
		boolean result = false;
		if(_BluetoothAdapter==null){
			Log.v(TAG, "prepareBTAdapter(): mAdapter is null");
			//this.sendInfo(R.string.BT_NOT_FOUND, "TDM BT not found");		
		}else{
			if(!_BluetoothAdapter.isEnabled()){
				Log.v(TAG, "prepareBTAdapter(): mAdapter is not enabled");
				//this.sendInfo(R.string.BT_NOT_ENABLED, "TDM BT not enabled");
				result=true;
			}else{
				Log.v(TAG, "prepareBTAdapter(): mAdapter is enabled");
				//this.sendInfo(R.string.BT_CONNECTING, "TDM BT connecting");
				result = true;
			}
		}
		return result;
	}

	public void stopDiscovery() {
		Log.v(TAG, "stopDiscovery()");

		mainCtx.unregisterReceiver(myReceiver);
	}

	@Override
	public synchronized void start() {
		// TODO Auto-generated method stub
		super.start();
		myArduinoThreads = new HashMap<String,BTDeviceThread>();
		newDevicesList = new ArrayList<BluetoothDevice>();
		connectableArduinos = new HashMap<String,BluetoothDevice>();
		ignoredDevicesList = new HashMap<String,BluetoothDevice>();

	}

	@Override
	public void run() {
		super.run();

		Log.v(TAG, "startDiscovery()");

		_discoveryThread = new ArduinoPlannerThread((BTManagerThread) this);
		if(this.isBTReady()){
			_discoveryThread.start();
		}

		// Get bonded devices (only the first time) 		
		Set<BluetoothDevice> bondedDevices = _BluetoothAdapter.getBondedDevices();
		newDevicesList.addAll(bondedDevices);

		while(!exit_condition){

		}
	}


	//--------------
	//NESTED CLASS
	//--------------


	private boolean exit_condition;

	private class ArduinoPlannerThread extends Thread {
		BTManagerThread BTmgr;

		private ArduinoPlannerThread(BTManagerThread btMngr) {
			this.BTmgr = btMngr;
		}

		public void run() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Loop to prevent the thread from finalizing and not answering to calls
			long tiempoEspera = 30000; //milisegundos

			while (!exit_condition){
				_BluetoothAdapter.startDiscovery();	// newDevicesList is updated when ACTION_FOUND

				try {
					Thread.sleep(tiempoEspera);
				} catch (InterruptedException e) { e.printStackTrace(); }
			}

		}

		/** 
		 * 1) Look for bonded devices
		 * 2) Discard devices that have been bonded but are no longer
		 * 3) Discover new devices
		 * 4) Select which devices are our arduinos
		 * 5) Request connection with each new arduino
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

						// Procede only if the device is one of our Arduinos
						if (deviceId.contains("ROBOTICA_") || deviceId.contains("BT-SENSOR_")) {

							// Discard Arduinos that are already connected, ignored or due for connection 
							if (!myArduinoThreads.containsKey(deviceId) 
									&& !ignoredDevicesList.containsKey(deviceId)
									&& !connectableArduinos.containsKey(deviceId)) {
								connectableArduinos.put(deviceId, device);
								result = true;
							}
						} else if (!ignoredDevicesList.containsKey(deviceId)) {
							//Put device in the Black-List, as it does not have any interest to us
							Log.v(TAG, "Ignoring device " + deviceId);
							ignoredDevicesList.put(deviceId, device);
						}
					}
				}
			}// synchronized-end
			Log.v(TAG, "fetchDevices() returns : "+ result);
			return result;
		}

		public void connectAvalaiableArduinos(){

			// 5. Request connection with each new Arduino
			if(connectableArduinos.size()>0){
				for(BluetoothDevice arduino: connectableArduinos.values()){
					mainHandler.obtainMessage(MainActivity.MESSAGE_CONNECT_ARDUINO,arduino).sendToTarget();
				}
				connectableArduinos.clear();
			}else{
				Log.v(TAG, "There isn't any new arduino");
			}
		}

		/**
		 * This function decides what should be done when a device misbehaves
		 * @param MAC: the address of the buggy Bluetooth-Arduino device 
		 */
		public void arduinoErrorRecovery(String MAC){

			if(MAC!=null)
				finalizeArduinoThread(MAC);
		}
	}

}


