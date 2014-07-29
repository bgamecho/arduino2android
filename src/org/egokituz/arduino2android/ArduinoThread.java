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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

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
	 * @param btMngrHandler 
	 * @param macAddress
	 * @throws Exception
	 */
	public ArduinoThread(Handler myHandler, Handler btMngrHandler, String macAddress) throws Exception {
		super(myHandler, btMngrHandler); // BTDeviceThread Constructor 
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
	private int MSG_TYPE;
	private long prevRealtime = 0, elapsedRealTime, elapsedTime;
	private long timestamp;

	private int pingSeqNum=1, dataSeqNum=0;
	
	private long errorCount = 0L, msgCount = 0L;
	private double errorRate;


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
				buffer = new byte[255];
				bufferIndex = 0;

				// Read bytes from the stream until we encounter the the start of message character
				while (( b = _inStream.read()) != STX ) {
					// Keep looking for STX
					Log.v(TAG, "Waiting for "+STX+". Read: "+b);
				}
				
				msgCount++;

				timestamp = System.currentTimeMillis();

				elapsedRealTime =  SystemClock.elapsedRealtime();
				elapsedTime = elapsedRealTime - prevRealtime;
				prevRealtime = elapsedRealTime;

				buffer[bufferIndex++] = (byte) b; // append STX at the beginning of the buffer  

				// The next byte must be the message ID
				b = _inStream.read();
				if (b != MSGID_PING && b != MSGID_DATA){
					Log.e(TAG, "Unexpected MSGID. Received: "+ b);
					errorCount++;
					errorRate=(errorCount/(double) msgCount)*100.0;
					Log.e(TAG, "Error rate: % "+ errorRate);
					return;
				} else {
					MSG_TYPE = b;
				}
				buffer[bufferIndex++] = (byte) b; // append MSGID

				// The next byte must be the frame number ID
				b = _inStream.read();
				buffer[bufferIndex++] = (byte) b; // append Frame Seq. number


				// Check if the frame sequence number is the expected (if not, a message may have been lost)
				switch (MSG_TYPE) {
				case MSGID_PING:
					if(b != pingSeqNum){
						Log.e(TAG, "Unexpected Ping Frame seq. number");
						Log.e(TAG, "Received:"+b+" Expected: "+pingSeqNum);
						pingSeqNum = b+1;
					}else if(pingSeqNum<99)
						pingSeqNum++;
					else
						pingSeqNum = 1;
					break;
				case MSGID_DATA:
					if(b != dataSeqNum){
						Log.e(TAG, "Unexpected Data Frame seq. number");
						Log.e(TAG, "Received:"+b+" Expected: "+dataSeqNum);
						dataSeqNum = b+1;
					}else if(dataSeqNum<99)
						dataSeqNum++;
					else
						dataSeqNum = 1;
					break;
				}

				// The next byte must be the expected data length code
				b = _inStream.read();
				buffer[bufferIndex++] = (byte) b; //append DLC

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
					errorCount++;
					errorRate=(errorCount/(double) msgCount)*100.0;
					Log.e(TAG, "Error rate: % "+ errorRate);
					return;
				}

				buffer[bufferIndex++] = (byte) b; // append ETX at the end of the buffer 

				//Log.d(TAG, "ArduinoThread: read "+Integer.toString(bufferIndex)+" bytes");

				byte[] auxBuff = new byte[bufferIndex];
				System.arraycopy(buffer, 0, auxBuff, 0, bufferIndex);

				// Notify the main activity that a message was read 
				Message sendMsg = new Message();
				Bundle myDataBundle = new Bundle();
				myDataBundle.putString("NAME", this.getDeviceName());
				myDataBundle.putString("MAC", this.getDeviceMAC());
				myDataBundle.putLong("TIMESTAMP", timestamp);
				myDataBundle.putLong("MSG_COUNT", msgCount);
				myDataBundle.putLong("ERROR_COUNT", errorCount);
				if(MSG_TYPE == MSGID_PING){
					myDataBundle.putLong("PINGSENTTIME", pingSentTime);
					sendMsg = mainHandler.obtainMessage(MainActivity.MESSAGE_PING, (int) elapsedTime, bufferIndex, auxBuff);
				}else
					sendMsg = mainHandler.obtainMessage(MainActivity.MESSAGE_READ, (int) elapsedTime, bufferIndex, auxBuff);
				sendMsg.setData(myDataBundle);
				sendMsg.sendToTarget();

			} catch (IOException e) {
				Log.e(TAG, "IOException reading socket for "+myBluetoothDevice.getName());
				//e.printStackTrace();

				// Notify the Bluetooth Manager Thread that the connection was lost, and let it decide the recovery process
				Message sendMsg = btMngrHandler.obtainMessage(BTManagerThread.MESSAGE_CONNECTION_LOST, (int) elapsedTime, bufferIndex, this);
				Bundle myDataBundle = new Bundle();
				myDataBundle.putString("NAME", this.getDeviceName());
				myDataBundle.putString("MAC", this.getDeviceMAC());
				sendMsg.setData(myDataBundle);
				sendMsg.sendToTarget();
				
				connected = false;

			} catch (ArrayIndexOutOfBoundsException e){
				Log.e(TAG, "Message lost. Received too much data");
				e.printStackTrace();
			}

			try {
				Thread.sleep(9);
			} catch (InterruptedException e) {
				Log.e(TAG, "Error waiting in the loop of the ArduinoThread");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Gets the handler to send messages to the robot
	 * @return handler 
	 */
	public Handler getHandler(){
		return arduinoHandler;
	}


	long pingSentTime; 
	int pingFrameSeqNum = 1;

	/**
	 * Sends a command to the connected Arduino via Bluetooth socket
	 * @param cmd: Command to send to the Arduino
	 */
	protected void write(String cmd) {

		//Check if its a PING command
		if(cmd.contentEquals("p")){
			pingSentTime = System.currentTimeMillis();
			int index = 0;
			int size = 0;
			byte[] outBuffer = new byte[16];
			outBuffer[index++] = STX;
			outBuffer[index++] = MSGID_PING;
			outBuffer[index++] = (byte) pingFrameSeqNum;
			size = "".getBytes().length+1;
			outBuffer[index++] = (byte) size;
			for(byte b : "".getBytes())
				outBuffer[index++] = b;
			long CRC = getChecksum("".getBytes());
			for( byte b : longToBytes(CRC))
				outBuffer[index++] = b;
			outBuffer[index++] = ETX;

			try {
				
				//TODO el arrayCopy no resuelve el problema, y ocupa memoria -> quitarlo
				
				byte[] auxBuff = new byte[index];
				System.arraycopy(outBuffer, 0, auxBuff, 0, index);
				
				String aux = Arrays.toString(auxBuff);
				Log.v(TAG, "Sending ping n� "+pingFrameSeqNum+"--"+aux);
				//Log.v(TAG, "Sending ping n� "+pingFrameSeqNum);
				_outStream.write(auxBuff, 0, index);
			} catch (IOException e) {
				Log.e(TAG, "Exception writting to the Arduino socket");
				e.printStackTrace();
			} catch (Exception e) {
				Log.e(TAG, "General exception in the run() method");
				e.printStackTrace();
			}
			
			pingFrameSeqNum++;
			if(pingFrameSeqNum>99)
				pingFrameSeqNum = 1;
		}
	}

	/**
	 * Calculates the CRC (Cyclic Redundancy Check) checksum value of the given bytes
	 * according to the CRC32 algorithm.
	 * @param bytes 
	 * @return The CRC32 checksum
	 */
	private Checksum checksum;
	private long getChecksum(byte bytes[]){

		checksum = new CRC32();

		// update the current checksum with the specified array of bytes
		checksum.update(bytes, 0, bytes.length);

		// get the current checksum value
		long checksumValue = checksum.getValue();

		return checksumValue;
	}

	public byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putLong(x);

		byte[] result = new byte[4];
		byte[] b = buffer.array();
		for(int i=0; i<4; i++)
			result[i] = b[i];
		return result;
	}


}
