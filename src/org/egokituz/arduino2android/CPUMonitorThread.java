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
import java.io.RandomAccessFile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author Xabier Gardeazabal
 */
public class CPUMonitorThread extends Thread{
	
	private static final String TAG = "cpuMonitor";
	
	private Handler mainHandler;
	private Context mainCtx;
	
	private boolean exit_condition = false;
	
	public CPUMonitorThread(Context context, Handler handler){
		this.setName(TAG);
		Log.v(TAG, "CPUMonitorThread Constructor start");
		
		mainHandler = handler;
		mainCtx = context;

	}
	
	private float readUsage() {
	    try {
	        RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
	        String load = reader.readLine();

	        String[] toks = load.split(" ");

	        long idle1 = Long.parseLong(toks[4]);
	        long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
	              + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

	        try {
	            Thread.sleep(360);
	        } catch (Exception e) {}

	        reader.seek(0);
	        load = reader.readLine();
	        reader.close();

	        toks = load.split(" ");

	        long idle2 = Long.parseLong(toks[4]);
	        long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
	            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

	        return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

	    } catch (IOException ex) {
	        ex.printStackTrace();
	    }

	    return 0;
	} 

	@Override
	public void run() {
		super.run();
		
		while(!exit_condition){
			
			Float pct = readUsage();
			
			long timestamp = System.currentTimeMillis();
			
			Message sendMsg = mainHandler.obtainMessage(MainActivity.MESSAGE_CPU_USAGE,pct);
			Bundle myDataBundle = new Bundle();
			myDataBundle.putLong("TIMESTAMP", timestamp);
			sendMsg.setData(myDataBundle);
			sendMsg.sendToTarget();
			
			//Log.v(TAG, "CPU load changed: "+pct);
			
			try {
				Thread.sleep(999);
			} catch (InterruptedException e) {
				Log.e(TAG, "Error waiting in the loop of the CPU monitor");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stops the thread in a safe way
	 */
	public void finalize() {
		exit_condition = true;
	}

}
