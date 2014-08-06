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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author Xabier Gardeazabal
 *
 */
public class LoggerThread extends Thread{

	private static final String TAG = "Logger";

	protected static final int MESSAGE_WRITE_DATA = 0;
	protected static final int MESSAGE_WRITE_BATTERY = 1;
	protected static final int MESSAGE_CPU = 2;
	protected static final int MESSAGE_PING = 3;
	protected static final int MESSAGE_ERROR = 4;
	protected static final int MESSAGE_EVENT = 5;
	protected static final int MESSAGE_NEW_LOG_FOLDER = 6;

	private Context mainCtx;
	private Handler mainHandler;

	private boolean exit_condition = false;


	public Handler logHandler = new Handler(){

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			String line;
			switch (msg.what) {
			case MESSAGE_PING:
				ArrayList<String> pingQueue = (ArrayList<String>) ((ArrayList<String>) msg.obj).clone(); //clone() or otherwise concurrent modification exception
				for (String pingLine : pingQueue) {
					appendLog("ping.txt",pingLine);
				}
				break;
			case MESSAGE_WRITE_DATA:
				ArrayList<String> dataQueue = (ArrayList<String>) ((ArrayList<String>) msg.obj).clone();  //clone() or otherwise concurrent modification exception
				for (String dataLine : dataQueue) {
					appendLog("data.txt",dataLine);	
				}
				break;
			case MESSAGE_WRITE_BATTERY:
				line = (String) msg.obj;
				appendLog("battery.txt",line);
				break;
			case MESSAGE_CPU:
				line = (String) msg.obj;
				appendLog("cpu.txt",line);
				break;
			case MESSAGE_ERROR:
				line = (String) msg.obj;
				appendLog("error.txt",line);
				break;
			case MESSAGE_EVENT:
				line = (String) msg.obj;
				appendLog("events.txt",line);
				break;
			case MESSAGE_NEW_LOG_FOLDER:
				createNextLogFolder();
				ArrayList<String> parametersQueue = (ArrayList<String>) msg.obj;
				for (String paramLine : parametersQueue) {
					appendLog("testParameters.txt",paramLine);
				}
				break;
			}
		}
	};


	public LoggerThread(Context mainCtx, Handler mainHandler) {
		super();
		this.setName("loggerThread");
		this.mainCtx = mainCtx;
		this.mainHandler = mainHandler;
		

		
		createNextLogFolder();
	}

	/**
	 * Creates a new directory under Root/logs/logX, where X is the next smaller integer available
	 */
	private void createNextLogFolder() {
		String folderName = "log1";
		logFolder = new File(RootDir+ File.separator + folderName);
		
		int i = 1;
		while(logFolder.isDirectory()){
			i++;
			folderName = "log"+i;
			logFolder = new File(RootDir + File.separator + folderName);
		}
		logFolder.mkdir();
	}

	@Override
	public void run() {
		super.run();

		while(!exit_condition){
			try {
				Thread.sleep(999);
			} catch (InterruptedException e) {
				Log.e(TAG, "Error waiting in the loop of the LoggerThread");
				e.printStackTrace();
			}
		}
	}

	// Externalize variables for performance
	private final File RootDir = new File(Environment.getExternalStorageDirectory() + File.separator + "logs");
	private File logFolder;
	private File logFile;
	private BufferedWriter buf;
	private FileWriter fw;
	
	public void appendLog(String fileName, String text){
		//Log.v(TAG, text);
		if(logFolder.canWrite()){
			//String filePath = mainCtx.getFilesDir().getPath().toString() + "/logXabi.txt";
			logFile = new File(logFolder,fileName);
			if (!logFile.exists()){
				try{
					logFile.createNewFile();
				} 
				catch (IOException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try{
				//BufferedWriter for performance, true to set append to file flag
				fw = new FileWriter(logFile, true);
				buf = new BufferedWriter(fw); 
				buf.append(text);
				buf.newLine();
				buf.close();
			}
			catch (IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stops the thread in a safe way
	 */
	protected void finalize() {
		//sendLogByEmail();
		exit_condition = true;
	}

	private void sendLogByEmail() {
		try 
		{        
			String fileName = URLEncoder.encode("logXabi.txt", "UTF-8");
			String PATH =  Environment.getExternalStorageDirectory()+"/"+fileName.trim().toString();

			Uri uri = Uri.parse("file://"+PATH);
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("text/plain");
			i.putExtra(Intent.EXTRA_EMAIL, new String[] {"xgardeazabal@gamail.com"});
			i.putExtra(Intent.EXTRA_SUBJECT,"android - email with attachment");
			i.putExtra(Intent.EXTRA_TEXT,"");
			i.putExtra(Intent.EXTRA_STREAM, uri);
			mainCtx.startActivity(Intent.createChooser(i, "Select application"));
		} 
		catch (UnsupportedEncodingException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
