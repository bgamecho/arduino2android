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
public class BatteryMonitorThread extends Thread{
	
	private static final String TAG = "BatteryMonitor";
	
	private Handler mainHandler;
	private Context mainCtx;
	
	private Intent batteryStatus;
	
	private boolean exit_condition = false;
	
	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			switch (action) {
			case Intent.ACTION_BATTERY_CHANGED:
				Float pct = getBatteryPercentage();
				
				long timestamp = System.currentTimeMillis();
				
				Message sendMsg = mainHandler.obtainMessage(MainActivity.MESSAGE_BATTERY_STATE_CHANGED,pct);
				Bundle myDataBundle = new Bundle();
				myDataBundle.putLong("TIMESTAMP", timestamp);
				sendMsg.setData(myDataBundle);
				sendMsg.sendToTarget();
				
				Log.v(TAG, "Battery Changed: "+pct);
				break;
			}
		}
	};

	
	public BatteryMonitorThread(Context context, Handler handler){
		this.setName(TAG);
		//Log.v(TAG, "BatteryMonitorThread Constructor start");
		
		mainHandler = handler;
		mainCtx = context;
		
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		batteryStatus = mainCtx.registerReceiver(myReceiver, ifilter);
	}
	
	public float getBatteryPercentage(){
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		float batteryPct = level / (float)scale;
		
		return batteryPct;
	}

	@Override
	public void run() {
		super.run();
		
		while(!exit_condition){
			try {
				Thread.sleep(999);
			} catch (InterruptedException e) {
				Log.e(TAG, "Error waiting in the loop of the robot");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stops the thread in a safe way
	 */
	public void finalize() {
		mainCtx.unregisterReceiver(myReceiver);
		exit_condition = true;
	}

}
