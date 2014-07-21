package org.egokituz.arduino2android;

import java.io.IOException;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 *  
 * @author bgamecho
 *
 */
public class ArduinoThread extends BTDeviceThread{

	public final static String TAG ="ArduinoThread";


	public String recvLine;

	public ArduinoThread(Handler myHandler, String remoteAddress) throws Exception{

		super(myHandler);

		this.setName("ArduinoThread");

		super.setupBT(remoteAddress);
		//super.initComm();
		super.initComm2();

		if(!this.connected){
			throw new Exception("could not connect");
		}
	}

	@Override
	public void initialize() {
		recvLine = "";
	}


	byte[] buffer = new byte[1024];
	int bytes;
	String currentCommand;
	int b = 0;
	int bufferIndex = 0;
	int payloadBytesRemaining;

	@Override
	public void loop() {
		//synchronized(this){
		if(connected){

			try {

				bufferIndex = 0;
				// Read bytes from the stream until we encounter the the start of message character
				while ( (char)( b = _inStream.read()) != '\n' )
					buffer[bufferIndex++] = (byte) b;

				byte[] auxBuff = new byte[--bufferIndex];
				System.arraycopy(buffer, 0, auxBuff, 0, bufferIndex);
				myHandler.obtainMessage(MainActivity.MESSAGE_READ, bufferIndex, -1, auxBuff)
				.sendToTarget();

			} catch (IOException e) {
				Log.e(TAG, "IOException reading socket for "+_bluetoothDev.getName());
				e.printStackTrace();
				connectionLost();
			} catch (Exception e){
				Log.e(TAG, "Exception reading socket for "+_bluetoothDev.getName());
				e.printStackTrace();
			}
			//}// end the synchronized code

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Log.e(TAG, "Error waiting in the loop of the robot");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Handler for receiving commands from the Activities
	 */
	public Handler arduinoHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle myBundle = msg.getData();

			if(myBundle.containsKey("COMMAND")){
				String cmd = myBundle.getString("COMMAND");
				synchronized(this){	
					//commandArray.add(cmd);
					write(cmd);
				}
			}
		}
	};

	/**
	 * Get the handler to send messages to the robot
	 * @return handler 
	 */
	public Handler getHandler(){
		return arduinoHandler;
	}

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