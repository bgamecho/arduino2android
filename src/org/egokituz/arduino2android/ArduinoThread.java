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
		//super.initComm2();
		super.initComm2();
		super.sendMessage("OK", "Connected to Arduino device at: "+_bluetoothDev.getAddress());
	}

	@Override
	public void initialize() {
		recvLine = "";
	}


	byte[] buffer = new byte[1024];
	int bytes;
	String currentCommand;

	@Override
	public void loop() {
		synchronized(this){
			try {
				bytes = _inStream.read(buffer);
				
				for(byte aux : buffer){
					if((char) aux == '\n' ){
						Log.v(TAG, recvLine);
						//this.sendMessage("Received message", aux);
						//myHandler.obtainMessage(MainActivity.MESSAGE_READ, recvLine.length(), -1, recvLine).sendToTarget();
						myHandler.obtainMessage(MainActivity.MESSAGE_READ, recvLine).sendToTarget();
						recvLine = "";
					}else{
						recvLine += (char) aux;
					}
				}
			} catch (IOException e) {
				Log.e(TAG, "IOException reading socket");
				e.printStackTrace();
			} catch (Exception e){
				Log.e(TAG, "other exception");
				e.printStackTrace();
			}
		}// end the synchronized code
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