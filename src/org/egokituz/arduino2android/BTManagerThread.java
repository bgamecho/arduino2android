/**
 * This class manages the phone's Bluetooth antenna
 * For each device discovered, it launches an "ArduinoThread" thread 
 */
package org.egokituz.arduino2android;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Xabier Gardeazabal
 *
 */
public class BTManagerThread extends Thread{

	private static final String TAG = "BTManager";

	//TODO REQUEST_ENABLE_BT is a request code that we provide (It's really just a number that you provide for onActivityResult)
	private static final int REQUEST_ENABLE_BT = 1;
	public static final int MESSAGE_BT_THREAD_CREATED = 2;
	public static final int MESSAGE_SEND_COMMAND = 3;

	private Handler mainHandler;
	private Context mainCtx;

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

	public BTManagerThread(Context context, Handler handler) {
		Log.v(TAG, "BTManagerThread Constructor start");

		mainHandler = handler;
		mainCtx = context;

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
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		IntentFilter filter4 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		IntentFilter filter5 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		IntentFilter filter6 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		mainCtx.registerReceiver(myReceiver, filter);
		mainCtx.registerReceiver(myReceiver, filter1);
		mainCtx.registerReceiver(myReceiver, filter2);
		mainCtx.registerReceiver(myReceiver, filter3);
		mainCtx.registerReceiver(myReceiver, filter4);
		mainCtx.registerReceiver(myReceiver, filter5);
		mainCtx.registerReceiver(myReceiver, filter6);
	}


	// The BroadcastReceiver that listens for discovered devices and connected devices
	// It also listens for connection, discovery and adapter state changes 
	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			BluetoothDevice device;
			String deviceName;
			switch (action){
			case BluetoothDevice.ACTION_FOUND:
				//TODO When discovery finds a device
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(newDevicesList.contains(device))
					newDevicesList.add(device);

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

				break;


			case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
				//TODO When discovery is finished
				Log.v(TAG, "discovery is finished");

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
	
	/**
	 * Handler for receiving commands from the Activities
	 */
	public Handler btHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_BT_THREAD_CREATED:
				ArduinoThread aux = (ArduinoThread) msg.obj;
				String devName = aux.getBluetoothDeviceName().getName();
				myArduinoThreads.put(devName, aux);
				Log.v(TAG, "Thread was successfully created");
				break;
			case MESSAGE_SEND_COMMAND:
				Bundle mBundle = msg.getData();
				String command = mBundle.getString("COMMAND");
				String MAC =  mBundle.getString("MAC");
				
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
				
			default:
				break;
			}
		}
	};
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

	public void startDiscovery() {
		Log.v(TAG, "startDiscovery()");

		Discovery discovery = new Discovery((BTManagerThread) this);
		if(this.isBTReady()){
			discovery.start();
		}
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
		startDiscovery();
		
		while(!exit_condition){}
	}


	//--------------
	//NESTED CLASS
	//--------------


	private boolean stop_discovery;
	private boolean exit_condition;
	private boolean firstDiscovery = true;

	class Discovery extends Thread {
		BTManagerThread BTmgr;

		private Discovery(BTManagerThread btMngr) {
			this.BTmgr = btMngr;
		}

		public void run() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Log.v(TAG, "run() ");
			//BTmgr.sendInfo(R.string.TDM_LAUNCH, "TDM Launched");

			// Loop to prevent the thread from finalizing and not answering to calls
			Log.v(TAG, "run: looping until exit condition");
			long tiempoEspera = 30000; //milisegundos
			do{
				//If it's time for a new discovery, and the Bluetooth adapter is enabled
				if(!stop_discovery){
					if (_BluetoothAdapter.isEnabled()) {
						discoverDevice();
						stop_discovery = true;
						// Send a runnable to the handler but delay its execution in X seconds
						mainHandler.postDelayed(new Runnable() {
							@Override
							public void run() {
								//Log.v(TAG, "Tiempo agotado");
								stop_discovery = false;
							}
						}, tiempoEspera);
					} else{
						//TODO Bluetooth Adapter is not enabled
						//Log.v(TAG, "Warning: Couldn't start discovery! Bluetooth is not enabled!");
					}
				}
			}while (!exit_condition);

		}

		/** 
		 * 1) Look for bonded devices
		 * 2) Discard devices that have been bonded but are no longer
		 * 3) Discover new devices
		 * 4) Select which devices are our arduinos
		 * 5) Request connection with each new arduino
		 */
		private boolean discoverDevice(){
			Log.v(TAG, "discoverDevice()");
			boolean result = false;
			newDevicesList.clear();	//TODO Check if this clear() is really necessary

			// 0. Get bonded devices (only the first time) 
			if(firstDiscovery){
				Set<BluetoothDevice> bondedDevices = _BluetoothAdapter.getBondedDevices();
				newDevicesList.addAll(bondedDevices);
				firstDiscovery = false;
			}

			//TODO 1. Check if previously bonded devices are no longer bonded (in which case, discard them)

			// 2. Discover devices (new devices are stored in the newDevicesList
			_BluetoothAdapter.startDiscovery();	// updates newDevicesList
			while(_BluetoothAdapter.isDiscovering()){
			}

			// 3. Select which devices are our Arduinos, and put them in the connectableArduinos list
			connectableArduinos.clear();
			Iterator<BluetoothDevice> myIterator = newDevicesList.iterator();
			BluetoothDevice device;
			if(newDevicesList.size()>0){
				while(myIterator.hasNext()){
					device = myIterator.next();
					String deviceName = device.getName()+"-"+device.getAddress();

					if(deviceName.contains("ROBOTICA_") || deviceName.contains("BT-SENSOR_")){

						// Discard devices that are already connected, ignored or due for connection 
						if(!myArduinoThreads.containsKey(deviceName) && !ignoredDevicesList.containsKey(deviceName) && !connectableArduinos.containsKey(deviceName)){
							connectableArduinos.put(deviceName,device);
						}
					}
				}
			}

			//TODO Preguntarle al usuario mediante un Dialog si quiere emparejar el nuevo dispositivo,
			// 		y si no quiere emparejarlo, meterlo en la lista ignoredDevicesList 

			// 5. Request connection with each new arduino
			if(connectableArduinos.size()>0){
				result = true;
				for(BluetoothDevice arduino: connectableArduinos.values()){
					mainHandler.obtainMessage(MainActivity.MESSAGE_CONNECT_ARDUINO,arduino).sendToTarget();
				}
			}else{
				Log.v(TAG, "There isn't any new arduino");
			}

			Log.v(TAG, "discoverDevice() returns : "+ result);
			return result;		
		}
	}
}


