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
 * This module handles the monitorization of the battery level of the device. 
 * Whenever the battery level changes, android broadcasts an Intent.ACTION_BATTERY_CHANGED intent.
 * This class extracts the data and delivers it to the main application which in turn will deliver it to the logger module.
 * 
 * @author Xabier Gardeazabal
 */
public class BatteryMonitorThread extends Thread{
	
	private static final String TAG = "BatteryMonitor";
	
	private Handler m_mainAppHandler;
	private Context m_AppContext;
	
	private Intent m_batteryStatus;
	
	private boolean m_exit_condition = false;
	
	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			switch (action) {
			case Intent.ACTION_BATTERY_CHANGED:
				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);

				float batteryPct = (100*level) / (float)scale;

				long timestamp = System.currentTimeMillis();
				
				Message sendMsg = m_mainAppHandler.obtainMessage(TestApplication.MESSAGE_BATTERY_STATE_CHANGED,batteryPct);
				Bundle myDataBundle = new Bundle();
				myDataBundle.putLong("TIMESTAMP", timestamp);
				sendMsg.setData(myDataBundle);
				sendMsg.sendToTarget();
				
				Log.v(TAG, "Battery Changed: "+batteryPct);
				break;
			}
		}
	};

	/**
	 * Constructor
	 * @param context The context of the main application
	 * @param handler
	 */
	public BatteryMonitorThread(Context context, Handler handler){
		this.setName(TAG);
		//Log.v(TAG, "BatteryMonitorThread Constructor start");
		
		m_mainAppHandler = handler;
		m_AppContext = context;
		
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		m_batteryStatus = m_AppContext.registerReceiver(myReceiver, ifilter);
	}

	@Override
	public void run() {
		super.run();
		
		while(!m_exit_condition){
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
		m_AppContext.unregisterReceiver(myReceiver);
		m_exit_condition = true;
	}

}
