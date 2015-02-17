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
import java.util.HashMap;

import org.egokituz.arduino2android.activities.SettingsActivity;
import org.egokituz.arduino2android.models.TestData;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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

	// Handler message types
//	public static final int MESSAGE_WRITE_DATA = 0;
//	public static final int MESSAGE_WRITE_BATTERY = 1;
//	public static final int MESSAGE_CPU = 2;
//	public static final int MESSAGE_PING = 3;
//	public static final int MESSAGE_ERROR = 4;
//	public static final int MESSAGE_EVENT = 5;
	public static final int MESSAGE_NEW_TEST = 7;
	
	// Filenames
	private final String FILE_PING = "ping.txt";
	private final String FILE_DATA = "data.txt";
	private final String FILE_BATTERY = "battery.txt";
	private final String FILE_CPU = "cpu.txt";
	private final String FILE_ERROR = "error.txt";
	private final String FILE_EVENTS = "events.txt";
	private final String FILE_PARAMETERS = "testParameters.txt";

	private Context m_AppContext;
	private Handler m_mainHandler;

	private boolean m_exitCondition = false;

	private boolean m_testInProcess = false;

	public Handler loggerThreadHandler = new Handler(){

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			ArrayList<String> textQueue;
			String text;
			switch (msg.what) {
			case TestData.DATA_PING:
				textQueue = TestData.toStringList((ArrayList<TestData>) ((ArrayList<TestData>) msg.obj).clone()); //clone() or otherwise concurrent modification exception
				appendLog(FILE_PING,textQueue);
				break;
			case TestData.DATA_STRESS:
				textQueue = TestData.toStringList((ArrayList<TestData>) ((ArrayList<TestData>) msg.obj).clone());
				appendLog(FILE_DATA,textQueue);
				break;
			case TestData.DATA_BATTERY:
				text = ((TestData) msg.obj).toString();
				textQueue = new ArrayList<String>();
				textQueue.add(text);
				appendLog(FILE_BATTERY,textQueue);
				break;
			case TestData.DATA_CPU:
				text = ((TestData) msg.obj).toString();
				textQueue = new ArrayList<String>();
				textQueue.add(text);
				appendLog(FILE_CPU,textQueue);
				break;
			case TestData.DATA_ERROR:
				text = ((TestData) msg.obj).toString();
				textQueue = new ArrayList<String>();
				textQueue.add(text);
				appendLog(FILE_ERROR,textQueue);
				break;
			case TestData.DATA_EVENT:
				text = ((TestData) msg.obj).toString();
				textQueue = new ArrayList<String>();
				textQueue.add(text);
				appendLog(FILE_EVENTS,textQueue);
				break;
			case MESSAGE_NEW_TEST:
				if(m_testInProcess)
					createNextLogFolder();
				
				HashMap<String, Integer> preferences = (HashMap<String, Integer>) msg.obj;
				
				ArrayList<String> parametersQueue = (ArrayList<String>) SettingsActivity.preferenceListToString(preferences);
				parametersQueue.add(0, getDeviceName());
				appendLog(FILE_PARAMETERS,parametersQueue);
				m_testInProcess = true;
				break;
			}
		}
	};


	public LoggerThread(Context mainCtx, Handler mainHandler) {
		super();
		setName("loggerThread");
		
		m_AppContext = mainCtx;
		m_mainHandler = mainHandler;

		if(!m_RootDir.exists())
			m_RootDir.mkdirs();

		createNextLogFolder();

	}

	/**
	 * Creates a new directory under Root/logs/logX, where X is the next smaller integer available
	 */
	private void createNextLogFolder() {
		String folderName = "log1";
		m_logFolder = new File(m_RootDir+ File.separator + folderName);

		int i = 1;
		while(m_logFolder.isDirectory()){
			i++;
			folderName = "log"+i;
			m_logFolder = new File(m_RootDir + File.separator + folderName);
		}
		m_logFolder.mkdir();
	}

	@Override
	public void run() {
		super.run();

		while(!m_exitCondition){
			try {
				Thread.sleep(999);
			} catch (InterruptedException e) {
				Log.e(TAG, "Error waiting in the loop of the LoggerThread");
				e.printStackTrace();
			}
		}
	}

	// Externalize variables for performance
	private final File m_RootDir = new File(Environment.getExternalStorageDirectory() + File.separator + "logs");
	private File m_logFolder;
	private File m_logFile;
	private BufferedWriter m_buf;
	private FileWriter m_fw;

	public void appendLog(String fileName, ArrayList<String> text){
		//Log.v(TAG, text);
		if(m_logFolder.canWrite()){
			//String filePath = mainCtx.getFilesDir().getPath().toString() + "/logXabi.txt";
			m_logFile = new File(m_logFolder,fileName);
			if (!m_logFile.exists()){
				try{
					m_logFile.createNewFile();
				} 
				catch (IOException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try{
				//BufferedWriter for performance, true to set append to file flag
				m_fw = new FileWriter(m_logFile, true);
				m_buf = new BufferedWriter(m_fw);
				for (String line : text) {
					m_buf.append(line);
					m_buf.newLine();
				}
				m_buf.close();
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
	public void finalize() {
		//sendLogByEmail();
		m_exitCondition = true;
	}

	public String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return model;
		} else {
			return manufacturer + " " + model;
		}
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
			m_AppContext.startActivity(Intent.createChooser(i, "Select application"));
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
