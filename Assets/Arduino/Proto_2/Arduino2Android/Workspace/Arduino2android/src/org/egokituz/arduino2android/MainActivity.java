package org.egokituz.arduino2android;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author bgamecho
 *
 */
public class MainActivity extends Activity {

	public final static String TAG = "ArduinoActivity";
	
	Spinner spinnerBluetooth;
	Button connectButton;
	Button ledOn;
	TextView tvLdr;
	
	String arduinoMAC; 
	
	private ArduinoThread arduino;
	boolean ardionoOn;
	
	public boolean finishApp; 
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ardionoOn = false;
        finishApp = false;
        
        spinnerBluetooth = (Spinner) findViewById(R.id.spinnerBluetooth);
		String[] myDeviceList = this.getBluetoothDevices();


		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_spinner_item, myDeviceList);
		spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
		spinnerBluetooth.setAdapter(spinnerArrayAdapter);
		spinnerBluetooth.setSelection(getIndex(spinnerBluetooth, "BT-"));
		
		spinnerBluetooth.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				
				String myMAC = spinnerBluetooth.getSelectedItem().toString();
				arduinoMAC = myMAC.substring(myMAC.length()-17);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}

		});

    	connectButton = (Button) findViewById(R.id.buttonConnect);
    	connectButton.setOnClickListener(
				new OnClickListener() {
					 
					@Override
					public void onClick(View v) {
						connectToArduino();
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
	}
	
	
	/**
	 * 
	 */
	public void connectToArduino(){
		try {
			arduino = new ArduinoThread(arduinoHandler, arduinoMAC);
			arduino.start();
			ardionoOn = true;
			sendCommandArduino("s");			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	public void disconnectAduino(){
		sendCommandArduino("f");

		new Handler().postDelayed(new Runnable(){
              public void run() {
            	  arduino.finalizeThread();
          		new Handler().postDelayed(new Runnable(){
                    public void run() {
                  	  finish();
                  	  
                    }                   
                }, 1000);
              }                   
          }, 1000);
		
	}
	
	/**
	 * 
	 */
	public void switchLed(){
		sendCommandArduino("l");
	}
	
	/**
	 * Send data to the Arduibo Thread
	 * 
	 * @param str command for the arduino
	 */
	public void sendCommandArduino(String str) {

		if(ardionoOn){
			Message sendMsg = new Message();
			Bundle myDataBundle = new Bundle();
			myDataBundle.putString("COMMAND", str);
			sendMsg.setData(myDataBundle);
//			try {
//				Thread.sleep(50);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			// Obtain the handler from the Thread and send the command in a Bundle
			arduino.getHandler().sendMessage(sendMsg);
			Log.v(TAG, "Command "+str+" sent");

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

	public String[] getBluetoothDevices(){
		String[] result = null;
		ArrayList<String> devices = new ArrayList<String>(); 
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();    
		if (!mBluetoothAdapter.isEnabled()){
			Log.e(TAG, "Bluetooth disabled");
		}else{			
			Set<BluetoothDevice> devList = mBluetoothAdapter.getBondedDevices();

			for( BluetoothDevice device : devList)
				devices.add(device.getName() + "-"+ device.getAddress());	  	

			String[] aux_items = new String[devices.size()];
			final String[] items = devices.toArray(aux_items);
			result = items;
		}
		return result;

	}

	public void easyToast(String message){
		Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();	  
	}
	
	public void modifyText(String myLdr){
		tvLdr.setText(myLdr);
	}
	
	/**
	 * Handler connected with the bluetooth devices Threads: 
	 * 	- OK : Get the Bluetooth address of the Arduino+
	 *  - LDR_data : String value of LDR
	 */
	public Handler arduinoHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			Bundle myBundle = msg.getData();

			if (myBundle.containsKey("OK")) {
				Log.v(TAG, myBundle.getString("OK"));
				easyToast(myBundle.getString("OK"));
				
			}else if (myBundle.containsKey("LDR_data")){
				Log.v(TAG, myBundle.getString("LDR_data"));
				tvLdr.setText(myBundle.getString("LDR_data"));
				
			}

		}

	};
	

}
