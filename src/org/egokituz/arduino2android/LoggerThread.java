/**
 * Copyright (C) 2014 Xabier Gardeazabal
 * 
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

/**
 * @author Xabier Gardeazabal
 *
 */
public class LoggerThread extends Thread{
	
	private static final String TAG = "BatteryMonitor";
	
	protected static final int MESSAGE_WRITE_TO_LOG_FILE = 0;
	
	private Context mainCtx;
	private Handler mainHandler;
	
	private boolean exit_condition = false;
	
	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			switch (action) {
			case "sth":

				break;
			}
		}
	};
	
	public LoggerThread(Context mainCtx, Handler mainHandler) {
		super();
		this.mainCtx = mainCtx;
		this.mainHandler = mainHandler;
	}
	
	@Override
	public void run() {
		super.run();
		
		while(!exit_condition){
			try {
				Thread.sleep(99);
			} catch (InterruptedException e) {
				Log.e(TAG, "Error waiting in the loop of the LoggerThread");
				e.printStackTrace();
			}
		}
	}
	
	public void appendLog(String text)
	{       
	   File logFile = new File("sdcard/log.file");
	   if (!logFile.exists())
	   {
	      try
	      {
	         logFile.createNewFile();
	      } 
	      catch (IOException e)
	      {
	         // TODO Auto-generated catch block
	         e.printStackTrace();
	      }
	   }
	   try
	   {
	      //BufferedWriter for performance, true to set append to file flag
	      BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
	      buf.append(text);
	      buf.newLine();
	      buf.close();
	   }
	   catch (IOException e)
	   {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	   }
	}

	/**
	 * Stops the thread in a safe way
	 */
	protected void finalize() {
		mainCtx.unregisterReceiver(myReceiver);
		exit_condition = true;
	}
	
	
	/*
	 	public void writeToFile(String message){
		try {
			createFileOnDevice(true);
			out.write(message+"\n");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Function to initially create the log file and it also writes the time of creation to file.
	private void createFileOnDevice(Boolean append) throws IOException {

		File Root = Environment.getExternalStorageDirectory();
		if(Root.canWrite()){
			File  LogFile = new File(Root, "LogXABI.txt");
			FileWriter LogWriter = new FileWriter(LogFile, append);
			out = new BufferedWriter(LogWriter);
			Date date = new Date();
			out.write("Logged at" + String.valueOf(date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds() + "\n"));
			//TODO remember to call out.close() or otherwise it won't write to the file
		}
	}
	 */



}
