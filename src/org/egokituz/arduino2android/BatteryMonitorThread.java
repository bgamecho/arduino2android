/**
 * 
 */
package org.egokituz.arduino2android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;

/**
 * @author Sensores
 *
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
				float pct = getBatteryPercentage();
				Log.v(TAG, "Battery Changed: "+pct);
				break;
			}
		}
	};

	
	public BatteryMonitorThread(Context context, Handler handler){
		Log.v(TAG, "BatteryMonitorThread Constructor start");
		
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
		// TODO Auto-generated method stub
		super.run();
		
		while(!exit_condition){}
	}

	protected void finalize() {
		mainCtx.unregisterReceiver(myReceiver);
		exit_condition = true;
	}

}
