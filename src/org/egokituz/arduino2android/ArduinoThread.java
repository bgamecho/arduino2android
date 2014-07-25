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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.Log;

/**
 *  
 * @author Xabier Gardeazabal
 * 
 * Each thread of this Class handles the connection with an Arduino
 * It writes and reads data to/from the socket 
 *
 */
public class ArduinoThread extends BTDeviceThread{

	public final static String TAG ="ArduinoThread";


	public String recvLine;

	/**
	 * New ArduinoThread that handles the connection with an Arduino with the provided MAC address
	 * It writes and reads data to/from the connected socket of the device
	 * @param myHandler
	 * @param macAddress
	 * @throws Exception
	 */
	public ArduinoThread(Handler myHandler, String macAddress) throws Exception {
		super(myHandler); // BTDeviceThread Constructor 
		this.setName("ArduinoThread"); // Set this thread's name
		super.setupBT(macAddress); // try to get the default Bluetooth adapter and remote device

		try {
			super.openConnection();
		} catch (IOException e) {
			if(!this.connected){
				boolean bonded = false;
				BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
				for(BluetoothDevice btDev : btAdapter.getBondedDevices()){
					if(btDev.getAddress().equalsIgnoreCase(macAddress))
						bonded = true;
				}
				if(bonded)
					throw new Exception("could not connect (the device may be out of range or off)");
				else
					throw new Exception("could not connect because device is not paired");
			}
		}
	}
	
	/**
	 * Handler for receiving commands from the Activities
	 */
	private Handler arduinoHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			Bundle myBundle = msg.getData();

			if(myBundle.containsKey("COMMAND")){
				String cmd = myBundle.getString("COMMAND");
				synchronized(this){	
					write(cmd);
				}
			}
		}
	};

	@Override
	public void initialize() {
		recvLine = "";
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#start()
	 */
	@Override
	public synchronized void start() {
		super.start();

		try {
			int unread = _inStream.available();
			_inStream.skip(unread);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private final int STX = 0x02; // Start of Text flag 
	private final int MSGID_PING = 0x26; // Message ID: ping type
	private final int MSGID_DATA = 0x27; // Message ID: Data type
	private final int ETX = 0x03; // End of Text flag

	private byte[] buffer = new byte[1024]; // Read buffer
	private int b = 0; // Read byte
	private int bufferIndex = 0;
	private int payloadBytesRemaining; // DLC parameter counter (received payload length)
	private long prevRealtime = 0, elapsedRealTime, elapsedTime;
	private Time timestamp;


	/**
	 * Reads from input-stream socket
	 * When a well formed message is read, sends a MESSAGE_READ message 
	 * through the Main Activity's handler.
	 */
	@Override
	protected void loop() {
		// Keep listening to the InputStream while connected
		if(connected){

			try {
				buffer = new byte[1024];
				bufferIndex = 0;

				// Read bytes from the stream until we encounter the the start of message character
				while (( b = _inStream.read()) != STX ) {
					// Keep looking for STX
					Log.v(TAG, "Waiting for "+STX+". Read: "+b);
				}

				Time timestamp = new Time();
				timestamp.setToNow();
				
				elapsedRealTime =  SystemClock.elapsedRealtime();
				elapsedTime = elapsedRealTime - prevRealtime;
				prevRealtime = elapsedRealTime;

				buffer[bufferIndex++] = (byte) b; // append STX at the beginning of the buffer  

				// The next byte must be the message ID, see the basic message format in the document
				b = _inStream.read();
				if (b != MSGID_PING && b != MSGID_DATA){
					Log.v(TAG, "Unexpected MSGID. Received: "+ b);
				}
				buffer[bufferIndex++] = (byte) b; // append MSGID

				// The next byte must be the expected data length code
				b = _inStream.read();
				buffer[bufferIndex++] = (byte) b; //DLC

				payloadBytesRemaining = --b; // Arduino UNO's problem?? WTF
				//payloadBytesRemaining = b;

				//Log.v(TAG, "Expected payload length: "+DLC+" bytes");

				while ( (payloadBytesRemaining--) > 0 ) {
					buffer[bufferIndex++] = (byte) (b = _inStream.read());
				}

				//The next four bytes must be the checksum
				for(int i=0; i<4; i++)
					buffer[bufferIndex++] = (byte) (b = _inStream.read());

				// The next byte must be the end of text indicator 
				if ((b = _inStream.read()) != ETX ){
					Log.e(TAG, "ETX incorrect. Received: "+b+". Expected"+ETX);
					//return;
				}

				buffer[bufferIndex++] = (byte) b; // append ETX at the end of the buffer 

				//Log.d(TAG, "ArduinoThread: read "+Integer.toString(bufferIndex)+" bytes");

				byte[] auxBuff = new byte[bufferIndex];
				System.arraycopy(buffer, 0, auxBuff, 0, bufferIndex);

				// Notify the main activity that a message was read 
				Message sendMsg = myHandler.obtainMessage(MainActivity.MESSAGE_READ, (int) elapsedTime, bufferIndex, auxBuff);
				Bundle myDataBundle = new Bundle();
				myDataBundle.putString("NAME", this.getDeviceName());
				myDataBundle.putString("MAC", this.getDeviceMAC());
				myDataBundle.putString("TIMESTAMP", timestamp.format("H%:M%:S"));
				sendMsg.setData(myDataBundle);
				sendMsg.sendToTarget();

			} catch (IOException e) {
				Log.e(TAG, "IOException reading socket for "+myBluetoothDevice.getName());
				e.printStackTrace();
				
				// Notify the Bluetooth Manager Thread that the connection was lost, and let it decide the recovery process
				Message sendMsg = myHandler.obtainMessage(BTManagerThread.MESSAGE_CONNECTION_LOST, (int) elapsedTime, bufferIndex, this);
				Bundle myDataBundle = new Bundle();
				myDataBundle.putString("NAME", this.getDeviceName());
				myDataBundle.putString("MAC", this.getDeviceMAC());
				sendMsg.setData(myDataBundle);
				sendMsg.sendToTarget();

			} catch (ArrayIndexOutOfBoundsException e){
				Log.e(TAG, "Message lost. Received too much data");
				e.printStackTrace();
			}
			
			/*
			try {
				Thread.sleep(9);
			} catch (InterruptedException e) {
				Log.e(TAG, "Error waiting in the loop of the ArduinoThread");
				e.printStackTrace();
			}*/
		}
	}

	/**
	 * Gets the handler to send messages to the robot
	 * @return handler 
	 */
	public Handler getHandler(){
		return arduinoHandler;
	}

	/**
	 * Sends a command to the connected Arduino via Bluetooth socket
	 * @param cmd: Command to send to the Arduino
	 */
	protected void write(String cmd) {

		buffer = cmd.getBytes();
		try {
			//buffer[0]= (byte) currentCommand.charAt(0);
			Log.v(TAG, "Data to send: "+buffer[0]);
			_outStream.write(buffer);
		} catch (IOException e) {
			Log.e(TAG, "Exception writting to the Arduino socket");
			e.printStackTrace();
		} catch (Exception e) {
			Log.e(TAG, "General exception in the run() method");
			e.printStackTrace();
		}

		// Delay time between commands
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			Log.e(TAG, "Error waiting in the loop of the robot");
			e.printStackTrace();
		}
	}


}
