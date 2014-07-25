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

	protected boolean terminateFlag;
	protected boolean beforeStart;
	protected boolean connected;

	Handler myHandler;
	
	protected String devName, devMac;

	protected BluetoothDevice myBluetoothDevice = null;
	
	protected BluetoothSocket _socket = null;
	protected InputStream _inStream = null;
	protected OutputStream _outStream = null;

	/**
	 * New abstract BTDeviceThread
	 * @param myHandler
	 * @throws Exception 
	 */
	public BTDeviceThread(Handler myHandler){
		this.setName(TAG);
		terminateFlag = false;
		beforeStart = true;
		connected=false;
		this.myHandler = myHandler;
	}
	
	public BluetoothDevice getBluetoothDevice() {
		return myBluetoothDevice;
	}
	public String getDeviceName() {
		return devName;
	}
	public String getDeviceMAC(){
		return devMac;
	}
	public String getDeviceId(){
		return devName+"-"+devMac;
	}

	/**
	 * Send messages to the class from we received the myHandler handler
	 * @param code: message code
	 * @param value: message value
	 */
	public void sendMessage(String code, String value){
		Message msg = new Message();
		Bundle myDataBundle = new Bundle();
		myDataBundle.putString(code,value);
		msg.setData(myDataBundle);
		myHandler.sendMessage(msg);  
	}

	/**
	 * Looks for the device in Paired Devices List
	 *  TODO Upgrade this method to discover new devices, etc...
	 * @throws Exception "mBluetoothAdapter is null"
	 * @throws Exception "_bluetoothDev is null" 
	 */
	protected void setupBT(String macAddress) throws Exception {

		final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

		if(btAdapter==null){
			//  Bluetooth is not supported on this hardware platform 
			Log.e(TAG, "BT Adapter is not available");
			throw new Exception("mBluetoothAdapter is null");
		}

		myBluetoothDevice = btAdapter.getRemoteDevice(macAddress);
		devName = myBluetoothDevice.getName();
		devMac = myBluetoothDevice.getAddress();

		if(myBluetoothDevice == null){
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
	public void openConnection() throws IOException{
		try {
			Method m = myBluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
			_socket = (BluetoothSocket) m.invoke(myBluetoothDevice, 1);

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
	 * Sets "beforeStart" flag to false
	 */
	public void initialize(){
		beforeStart = false;
	}

	/**
	 * 
	 */
	protected abstract void loop();

	/**
	 * Close this thread by liberating resources
	 */
	public void close(){

		// After the threads ends close the connection and release the socket connection 
		resetConnection();

		this.sendMessage("OFF", this.getName());
	}

	/**
	 * Resets the connection: closes input & output streams and the socket.
	 * Warning: this function will block until the socket is disconnected.
	 */
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
				// Wait until socket is disconnected completely
			}
			_socket = null;
			connected = false;
		}
	}


	/**
	 * Main run
	 */
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
		if(myBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED)
			return true;
		return false;
	}

}
