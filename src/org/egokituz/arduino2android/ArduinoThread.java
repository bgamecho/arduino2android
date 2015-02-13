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
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egokituz.utils.ArduinoMessage;
import org.egokituz.utils.BadMessageFrameFormat;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

/**
 *  
 * @author Xabier Gardeazabal
 * 
 * Each thread of this Class handles the connection with an Arduino
 * It writes and reads data to/from the socket 
 *
 */
public class ArduinoThread extends Thread{

	public final static String TAG ="ArduinoThread";

	private boolean terminateFlag;
	private boolean connected;

	protected Handler mainHandler;
	protected Handler btMngrHandler;

	private BluetoothDevice myBluetoothDevice = null;
	private String devName, devMAC;

	private BluetoothSocket _socket = null;
	private InputStream _inStream = null;
	private OutputStream _outStream = null;
	
	/**
	 * New ArduinoThread that handles the connection with an Arduino with the provided MAC address
	 * It writes and reads data to/from the connected socket of the device
	 * @param myHandler
	 * @param btMngrHandler 
	 * @param macAddress
	 * @throws Exception
	 */
	public ArduinoThread(Handler myHandler, Handler btMngrHandler, BluetoothDevice device) throws Exception {
		terminateFlag = false;
		connected=false;
		this.mainHandler = myHandler;
		this.btMngrHandler = btMngrHandler;

		myBluetoothDevice = device;
		devMAC = myBluetoothDevice.getAddress();
		devName = myBluetoothDevice.getName();

		this.setName("ArduinoThread_"+devName); // Set this thread's name
		openConnection();

	}

	public BluetoothDevice getBluetoothDevice() {
		return myBluetoothDevice;
	}
	public String getDeviceName() {
		return devName;
	}
	public String getDeviceMAC(){
		return devMAC;
	}
	public String getDeviceId(){
		return devName+"-"+devMAC;
	}
	public boolean isConnected() {
		return connected;
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
	public void openConnection() throws Exception{
		Method m = myBluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
		_socket = (BluetoothSocket) m.invoke(myBluetoothDevice, 1);
		_socket.connect();
		_inStream = _socket.getInputStream();
		_outStream = _socket.getOutputStream();

		if(_socket.isConnected())
			connected = true;
	}

	/**
	 * Handler for receiving commands from the Activities
	 */
	public Handler arduinoHandler = new Handler(Looper.getMainLooper()) {
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

	/**
	 * Close this thread by liberating resources: closes input & output streams and the socket.
	 * Warning: this function will block until the socket is disconnected.
	 */
	public void close(){
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
	 * Stops the thread in a safe way
	 */
	public void finalizeThread(){
		terminateFlag = true;
		Log.d(TAG, this.getName()+" is passing from alive to death state ");
		close();
	}

	@Override
	public void run() {
		while(!terminateFlag) {
			loop();
		}
	}



	private byte[] buffer = new byte[1024]; // Read buffer
	private int b = 0; // Read byte
	private int bufferIndex = 0;
	private int payloadBytesRemaining; // DLC parameter counter (received payload length)
	private int MSG_TYPE;
	private long prevRealtime = SystemClock.elapsedRealtime(), elapsedRealTime, elapsedTime;
	private long timestamp;

	private int expectedPingSeqNum=1, expectedDataSeqNum=1;

	private long errorCount = 0L, msgCount = 0L;
	private double errorRate;
	private long ping;


	/**
	 * Reads from input-stream socket
	 * When a well formed message is read, sends a MESSAGE_READ message 
	 * through the Main Activity's handler.
	 */
	protected void loop() {
		// Keep listening to the InputStream while connected
		if(connected){

			try {
				//buffer = new byte[1024];
				bufferIndex = 0;

				// Read bytes from the stream until we encounter the the start of message character
				while (( b = _inStream.read()) != ArduinoMessage.STX ) {
					// Keep looking for STX (implies that a frame may have been lost)
					//Log.v(TAG, "Waiting for STX. Read: "+b);
				}

				// Perform time calculations as soon as the STX is read
				timestamp = System.currentTimeMillis();
				elapsedRealTime =  SystemClock.elapsedRealtime();
				elapsedTime = elapsedRealTime - prevRealtime;
				prevRealtime = elapsedRealTime;

				msgCount++;

				buffer[bufferIndex++] = (byte) b; // append STX at the beginning of the buffer  

				// The next byte must be the message ID
				b = _inStream.read();
				if (b != ArduinoMessage.MSGID_PING && b != ArduinoMessage.MSGID_DATA){
					String detailMessage = "Unexpected MSGID. Received: "+ b;
					throw new BadMessageFrameFormat(detailMessage);
				} else {
					MSG_TYPE = b;
				}
				buffer[bufferIndex++] = (byte) b; // append MSGID

				// The next byte must be the frame number ID
				b = _inStream.read();
				buffer[bufferIndex++] = (byte) b; // append Frame Seq. number

				int seqNum = b;
				// Check if the frame sequence number is the expected (if not, a message may have been lost)
				switch (MSG_TYPE) {
				case ArduinoMessage.MSGID_PING:
					try {
						ping = timestamp-pingSentTimes.get(seqNum);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(seqNum != expectedPingSeqNum){
						//Log.e(TAG, "Unexpected Ping Frame seq. number");
						//Log.e(TAG, "Received:"+seqNum+" Expected: "+expectedPingSeqNum);
						expectedPingSeqNum = seqNum+1;
					}else if(seqNum<99)
						expectedPingSeqNum = seqNum+1;
					else
						expectedPingSeqNum = 1;
					break;
				case ArduinoMessage.MSGID_DATA:
					if(seqNum != expectedDataSeqNum){
						//Log.e(TAG, "Unexpected Data Frame seq. number");
						//Log.e(TAG, "Received:"+b+" Expected: "+expectedDataSeqNum);
						expectedDataSeqNum = seqNum+1;
					}else if(seqNum<99)
						expectedDataSeqNum = seqNum+1;
					else
						expectedDataSeqNum = 1;
					break;
				}

				//The next four bytes must be the expected data length code (DLC)
				for(int i=0; i<4; i++)
					buffer[bufferIndex++] = (byte) (b = _inStream.read());
				
				byte[] dlcBytes = new byte[8];
				System.arraycopy(buffer, 3, dlcBytes, 0, 4);
				
				ByteBuffer dlcBuff = ByteBuffer.wrap(dlcBytes);
				dlcBuff.order(ByteOrder.BIG_ENDIAN);
				//payloadBytesRemaining = (int) (auxBuffer.getInt() & 0xFFFFFFFFL);
				payloadBytesRemaining = (int) dlcBuff.getInt();
			    //Log.v(TAG, "expected "+payloadBytesRemaining+ " bytes to read");
				
			    /*
				auxBuffer.order(ByteOrder.BIG_ENDIAN);
				//payloadBytesRemaining = (int) (auxBuffer.getInt() & 0xFFFFFFFFL);
				payloadBytesRemaining = (int) auxBuffer.getLong();
			    Log.v(TAG, "expected "+payloadBytesRemaining+ " bytes to read");
				*/
			    
				//payloadBytesRemaining = b;

				while ( (payloadBytesRemaining--) > 0 ) {
					buffer[bufferIndex++] = (byte) (b = _inStream.read());
				}

				//The next four bytes must be the checksum
				for(int i=0; i<4; i++)
					buffer[bufferIndex++] = (byte) (b = _inStream.read());

				// The next byte must be the end of text indicator 
				if ((b = _inStream.read()) != ArduinoMessage.ETX ){
					String detailMessage = "ETX incorrect. Received: "+b;
					throw new BadMessageFrameFormat(detailMessage);
				}

				buffer[bufferIndex++] = (byte) b; // append ETX at the end of the buffer 

				byte[] auxBuff = new byte[bufferIndex];
				System.arraycopy(buffer, 0, auxBuff, 0, bufferIndex);

				ArduinoMessage readMessage = new ArduinoMessage(devName, auxBuff);
				readMessage.devName = this.devName;
				readMessage.devMAC = this.devMAC;
				readMessage.timestamp = timestamp;

				processMessage(readMessage, ping);

			} catch (BadMessageFrameFormat e){
				errorCount++;
				errorRate=(errorCount/(double) msgCount)*100.0;
				Log.e(TAG, devName+" error rate: % "+ errorRate);
				
				// Notify the main activity that a frame was dropped
				String errorLine = timestamp+" "+devName;
				mainHandler.obtainMessage(TestApplication.MESSAGE_ERROR_READING, errorLine).sendToTarget();
			} catch (IOException e) {
				Log.e(TAG, "IOException reading socket for "+myBluetoothDevice.getName());
				//e.printStackTrace();

				// Notify the Bluetooth Manager Thread that the connection was lost, and let it decide the recovery process
				Message sendMsg = btMngrHandler.obtainMessage(BTManagerThread.MESSAGE_CONNECTION_LOST, this);
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


	private ArrayList<String> dataQueue = new ArrayList<String>();
	private ArrayList<String> pingQueue = new ArrayList<String>();
	
	/**
	 * Takes both DATA and PING type messages, and stores each of them in an array-list. 
	 * Once there is enough messages per type, it sends them to the main activity's handler. 
	 * @param readMessage
	 * @param ping: only read/needed if readMessage is of type MSGID_PING
	 */
	private void processMessage(ArduinoMessage readMessage, long ping) {
		int msgType = readMessage.getMessageID();
		String msg = readMessage.timestamp + " "+this.devName+" "+readMessage.size();
		
		switch (msgType) {
		case ArduinoMessage.MSGID_PING:
			msg +=" "+ping;
			pingQueue.add(msg);
			if(pingQueue.size()>99){
				mainHandler.obtainMessage(TestApplication.MESSAGE_PING_READ, pingQueue.clone()).sendToTarget();
				pingQueue.clear();
			}
			break;
		case ArduinoMessage.MSGID_DATA:
			dataQueue.add(msg);
			if(dataQueue.size()>99){
				mainHandler.obtainMessage(TestApplication.MESSAGE_DATA_READ, dataQueue.clone()).sendToTarget();
				dataQueue.clear();
			}
			break;
		}
		
		//Log.v(TAG,msg);
	}
	

	//private long pingSentTime; 
	private int pingFrameSeqNum = 1;
	private Map<Integer,Long> pingSentTimes = new HashMap<Integer, Long>();

	/**
	 * Sends a command to the connected Arduino via Bluetooth socket
	 * @param cmd: Command to send to the Arduino
	 */
	protected void write(String cmd) {

		//Check if its a PING command
		if(cmd.contentEquals("p")){
			pingSentTimes.put(pingFrameSeqNum, System.currentTimeMillis());
			ArduinoMessage myMsg = new ArduinoMessage();
			List<Byte> outBuffer = myMsg.pingMessage(pingFrameSeqNum);

			byte[] auxBuff = new byte[outBuffer.size()];
			for(int i=0; i<outBuffer.size(); i++)
				auxBuff[i] = outBuffer.get(i);
			try {
				String aux = Arrays.toString(auxBuff);
				//Log.v(TAG, "Sending ping nº "+pingFrameSeqNum+"--"+aux);

				_outStream.write(auxBuff, 0, outBuffer.size());
			} catch (IOException e) {
				Log.e(TAG, "Exception writting to the Arduino socket");
				e.printStackTrace();
			}

			pingFrameSeqNum++;
			if(pingFrameSeqNum>99)
				pingFrameSeqNum = 1;
		}
	}





}
