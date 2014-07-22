package org.egokituz.arduino2android;

import java.util.ArrayList;
import java.util.Set;
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
 * 
 * @author xgardeazabal
 *
 */
public class MainActivity extends Activity {

	public final static String TAG = "ArduinoActivity";

	public static final int MESSAGE_TOAST = 0;
	//TODO REQUEST_ENABLE_BT is a request code that we provide (It's really just a number that you provide for onActivityResult)
	private static final int REQUEST_ENABLE_BT = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_CONNECT_ARDUINO = 3;

	public static final String TOAST = "toast";



	Button refreshButton, connectButton, disconnectButton,startButton,finButton;
	Spinner spinnerBluetooth;
	ListView devicesListView;
	Button ledOn;
	TextView tvLdr;

	private String selected_arduinoMAC;

	private BTManagerThread myBTManagerThread;
	private BatteryMonitorThread myBatteryMonitor;

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

		myBTManagerThread = new BTManagerThread(this, arduinoHandler);
		myBatteryMonitor = new BatteryMonitorThread(this, arduinoHandler);

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
		if(!myBTManagerThread.isAlive())
			myBTManagerThread.start();
		if(!myBatteryMonitor.isAlive())
			myBatteryMonitor.start();
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
		myBTManagerThread.finalize();
	}


	@Override
	public void finish() {
		Log.v(TAG, "Arduino Activity --OnDestroy()--");
		super.finish();
		//Finalize threads
		myBTManagerThread.finalize();
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

	private void populateDeviceListView() {
		// TODO Auto-generated method stub
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
			arduino = new ArduinoThread(arduinoHandler, selected_arduinoMAC);
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
	 * Send data to the Arduibo Thread
	 * 
	 * @param str command for the arduino
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
			myBTManagerThread.btHandler.sendMessage(sendMsg);
			//arduino.getHandler().sendMessage(sendMsg);
			//Log.v(TAG, "Command "+str+" sent");

		}

	}

	public String[] getBluetoothDevices(){
		Log.v(TAG, "discoverDevice()");

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

	public String[] getConnectedDevices(){
		String[] result = null;

		if(myBTManagerThread != null && myBTManagerThread.isAlive())
			result = myBTManagerThread.getConnectedArduinos();
		return result;

	}


	public void modifyText(String myLdr){
		tvLdr.setText(myLdr);
	}

	/**
	 * Handler connected with the bluetooth devices Threads: 
	 */
	public Handler arduinoHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			//Bundle myBundle = msg.getData();

			switch (msg.what) {
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);

				//Log.v(TAG, readMessage);
				tvLdr.setText(readMessage);
				break;
			case MESSAGE_CONNECT_ARDUINO:
				BluetoothDevice newDevice = (BluetoothDevice) msg.obj;
				Log.v(TAG, "Dispatching thread creation for "+newDevice.getName());
				BackgroundThreadDispatcher thDispatcher = new BackgroundThreadDispatcher();
				thDispatcher.execute(newDevice);
				populateDeviceListView();
			}
		}

	};


	private class BackgroundThreadDispatcher extends AsyncTask<BluetoothDevice, Void, String> {

		protected String doInBackground(BluetoothDevice... params) {
			ArduinoThread _newArduinoThread = null;

			BluetoothDevice newDevice = params[0];

			String devId = newDevice.getName()+"-"+newDevice.getAddress();

			//TODO check that there is no other thread connected with this device

			try {
				Log.v(TAG, "Trying to connect to "+devId);
				_newArduinoThread = new ArduinoThread(arduinoHandler, newDevice.getAddress());
				_newArduinoThread.start();
				myBTManagerThread.btHandler.obtainMessage(BTManagerThread.MESSAGE_BT_THREAD_CREATED, _newArduinoThread).sendToTarget();
				return "OK";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				myBTManagerThread.btHandler.obtainMessage(BTManagerThread.MESSAGE_ERROR_CREATING_BT_THREAD, newDevice).sendToTarget();
				Log.v(TAG, "Could not create thread for "+devId);
				if(_newArduinoThread != null){
					_newArduinoThread.finalizeThread();
					e.printStackTrace();
				}
				return "OK";
			}
		}
	}

}
