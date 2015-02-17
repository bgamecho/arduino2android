/**
 * 
 */
package org.egokituz.arduino2android;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

import org.egokituz.arduino2android.activities.SettingsActivity;
import org.egokituz.arduino2android.models.BatteryData;
import org.egokituz.arduino2android.models.CPUData;
import org.egokituz.arduino2android.models.PingData;
import org.egokituz.arduino2android.models.StressData;
import org.egokituz.arduino2android.models.TestData;
import org.egokituz.arduino2android.models.TestEvent;
import org.egokituz.arduino2android.models.TestStatistics;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author Xabier Gardeazabal
 *
 */
public class StatisticsThread extends Thread {

	private static final String TAG = "StatisticsThread";

	private Handler m_mainAppHandler;
	private Context m_AppContext;

	private boolean m_exit_condition = false;

	public StatisticsThread(Context context, Handler handler){
		setName(TAG);

		m_mainAppHandler = handler;
		m_AppContext = context;

		m_startTime = System.currentTimeMillis();
	}

	private final long m_startTime;
	private Float m_startBattery;
	private float m_batteryDrainHour = 0f;
	private long m_transferedBytes = 0;
	private long m_totalErrors = 0;
	private int m_totalDiscoveries = 0;
	private long m_totalPings = 0;
	private double m_totalPingTime = 0;
	private long m_totalMessages = 0;
	private float m_meanPing = 0f;
	private float m_meanCPU = 0f;
	private double m_totalCPUusage = 0f;
	private long m_CPUreadings = 0;
	private float m_throughput = 0f;
	private int m_foundDevices = 0;
	private int m_connectedDevices = 0;
	
	public Handler statisticsThreadHandler = new Handler(){


		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			ArrayList<String> textQueue;
			String text;
			long currentTime;
			switch (msg.what) {
			case TestData.DATA_PING:
				ArrayList<PingData> l = (ArrayList<PingData>) msg.obj;
				for(PingData ping : l){
					m_totalMessages++;
					m_transferedBytes += ping.msgSize;
					m_throughput += ping.msgSize;
					m_totalPings++;
					m_totalPingTime += ping.pingTime;
					m_meanPing = (float) (m_totalPingTime/m_totalPings);
				}
				break;
			case TestData.DATA_STRESS:
				ArrayList<StressData> sList = (ArrayList<StressData>) msg.obj;
				for(StressData stress : sList){
					m_totalMessages++;
					m_transferedBytes += stress.msgSize;
					m_throughput += stress.msgSize;
				}
				break;
			case TestData.DATA_BATTERY:
				BatteryData bData = (BatteryData) msg.obj;
				if(m_startBattery == null)
					m_startBattery = bData.batteryLevel;
				currentTime = System.currentTimeMillis();
				m_batteryDrainHour = 3600000*(bData.batteryLevel-m_startBattery)/(currentTime-m_startTime);

				break;
			case TestData.DATA_CPU:
				CPUData cpu = (CPUData) msg.obj;
				m_totalCPUusage += cpu.cpuLoad;
				m_CPUreadings++;
				m_meanCPU = (float) (m_totalCPUusage/m_CPUreadings);

				break;
			case TestData.DATA_ERROR:
				m_totalErrors++;
				break;
			case TestData.DATA_EVENT:
				TestEvent event = (TestEvent) msg.obj;
				switch (event.eventID) {
				case TestEvent.EVENT_NEW_DISCOVERY_STARTED:
					m_totalDiscoveries++;
				case TestEvent.EVENT_DISCOVERY_FINISHED:
					break;
				case TestEvent.EVENT_NEW_DEVICE_FOUND:
					m_foundDevices++;
					break;
				case TestEvent.EVENT_NEW_DEVICE_CONNECTED:
					m_connectedDevices++;
					break;
				case TestEvent.EVENT_DEVICE_DISCONNECTED:
					m_connectedDevices--;
					break;
				}
				break;
			default:
				break;
			}
		}
	};

	@Override
	public void run() {
		super.run();
		while(!m_exit_condition){
			
			communicateStatistics();
			m_throughput = 0f;

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Log.e(TAG, "Error waiting in the loop of the CPU monitor");
				e.printStackTrace();
			}
		}
	}

	private void communicateStatistics() {
		TestStatistics ts = new TestStatistics();
		ts.startTime = m_startTime;
		ts.startBattery = m_startBattery;
		ts.batteryDrainHour = m_batteryDrainHour;
		ts.meanCPU = m_meanCPU;
		ts.meanPing = m_meanPing;
		ts.totalDiscoveries = m_totalDiscoveries;
		ts.totalErrors = m_totalErrors;
		ts.transferedBytes = m_transferedBytes;
		ts.btSpeed = m_throughput;
		ts.totalMessages = m_totalMessages;

		m_mainAppHandler.obtainMessage(TestData.DATA_STATISTIC, ts).sendToTarget();

	}

	/**
	 * Stops the thread in a safe way
	 */
	public void finalize() {
		m_exit_condition = true;
	}

}
