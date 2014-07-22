package org.egokituz.arduino2android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Abstract class for generic Bluetooth connection and thread management
 * 
 * @author bgamecho
 *
 */
public abstract class BTDeviceThread extends Thread {

	public final static String TAG = "BTDeviceThread";

	//TODO check if this is ok for all Bluetooth devices
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	boolean terminateFlag;
	boolean beforeStart;
	boolean connected;

	Handler myHandler;


	public BluetoothDevice _bluetoothDev = null;

	public BluetoothDevice getBluetoothDevice() {
		return _bluetoothDev;
	}
	public String getDeviceName() {
		return _bluetoothDev.getName();
	}
	public String getDeviceMAC(){
		return _bluetoothDev.getAddress();
	}
	
	public String getDeviceId(){
		String devId = _bluetoothDev.getName()+"-"+_bluetoothDev.getAddress();
		return devId;
	}

	public BluetoothSocket _socket = null;
	// The robot doesn't provide feedback to the App, only the outStream is needed
	public InputStream _inStream = null;
	public OutputStream _outStream = null;


	/**
	 * 
	 * @param myHandler
	 * @throws Exception 
	 */
	public BTDeviceThread(Handler myHandler) throws Exception{
		terminateFlag = false;
		beforeStart = true;
		connected=false;
		this.myHandler = myHandler;
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
		myHandler.sendMessage(msg);  
	}

	/**
	 * Looks for the robot in Paired Devices List
	 *  TODO Upgrade this method to discover new devices, etc...
	 */
	public void setupBT(String macAddress) throws Exception{

		final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

		if(btAdapter==null){
			Log.e(TAG, "BT Adapter is not available");
			throw new Exception("mBluetoothAdapter is null");
		}

		_bluetoothDev = btAdapter.getRemoteDevice(macAddress);

		if(_bluetoothDev == null){
			Log.e(TAG, "Can't obtain a device with that address");
			throw new Exception("_bluetoothDev is null");
		}
	}

	
	/** WARNING: This method will block until a connection is made or the connection fails. 
	 * 	If this method returns without an exception then this socket is now connected.
	 * 
	 * Creating new connections to remote Bluetooth devices should not be attempted 
	 * while device discovery is in progress. Device discovery is a heavyweight procedure 
	 * on the Bluetooth adapter and will significantly slow a device connection.
	 * 
	 * if device is not bonded, an intent will automatically be called and user should enter PIN code
	 */
	public void initComm2() throws IOException{
		try {
			Method m = _bluetoothDev.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
			_socket = (BluetoothSocket) m.invoke(_bluetoothDev, 1);

			_socket.connect(); 

			_inStream = _socket.getInputStream();
			_outStream = _socket.getOutputStream();
			
			if(_socket.isConnected())
				connected = true;
			
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * 
	 */
	public void initialize(){
		beforeStart = false;
	}

	/**
	 * 
	 */
	public abstract void loop();

	/**
	 * 
	 */
	public void close(){

		// After the threads ends close the connection and release the socket connection 
		resetConnection();
		//		try {
		//			_inStream.close();
		//			_outStream.close();
		//			_socket.close();
		//		} catch (IOException e) {
		//			Log.e(TAG, "Closing the connection with the Robot");
		//			e.printStackTrace();
		//		}

		this.sendMessage("OFF", this.getName());
	}

	private void resetConnection() {
		Log.v(TAG, "resetConnection()");
		if (_inStream != null) {
			try {
				_inStream.close();
			} catch (Exception e) {
				Log.e(TAG, "Error closing the input stream");
				e.printStackTrace();
			}
			_inStream = null;
		}

		if (_outStream != null) {
			try {
				_outStream.close();
			} catch (Exception e) {
				Log.e(TAG, "Error closing the output stream");
				e.printStackTrace();
			}
			_outStream = null;
		}

		if (_socket != null) {
			try {_socket.close();
			} catch (Exception e) {
				Log.e(TAG, "Error closing the BT socket");
				e.printStackTrace();
			}
			while(_socket.isConnected()){
				//Do nothing
			}
			_socket = null;
			connected = false;
		}
	}

	public void connectionLost(){
		resetConnection();
		try {
			initComm2();
			
			if(!connected){
					Message sendMsg = new Message();
					Bundle myDataBundle = new Bundle();
					myDataBundle.putString("MAC", this.getDeviceMAC());
					sendMsg.setData(myDataBundle);
					sendMsg.what = BTManagerThread.MESSAGE_CONNECTION_LOST;
					myHandler.sendMessage(sendMsg);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void run() {
		initialize();

		while(!terminateFlag) {
			loop();

		}// end the while loop

	}

	/**
	 * Stops the thread in a safe way
	 */
	public void finalizeThread(){

		// if the thread is alive the socket is alredy open and used
		if(this.isAlive()){
			terminateFlag = true;
			Log.d(TAG, this.getName()+" is passing from alive to death state ");
			close();

			// else the socket is only open but not used
		}else{
			// if the thread hasn't started yet
			if(beforeStart){ //Release the socket connection
				Log.d(TAG, this.getName()+" thread closes without starting the loop");
				close();
				// if the thread is finished the socket is already free
			}else{	
				Log.d(TAG, this.getName()+" thread is finished and in death state");

			}
		}
	}

	public boolean isConnected() {
		// TODO Auto-generated method stub
		if(_bluetoothDev.getBondState() == BluetoothDevice.BOND_BONDED)
			return true;
		return false;
	}

}
