/**
 * 
 */
package org.egokituz.arduino2android;

import java.util.ArrayList;
import java.util.HashMap;

import org.egokituz.arduino2android.activities.SettingsActivity;
import org.egokituz.arduino2android.models.ArduinoMessage;
import org.egokituz.arduino2android.models.BatteryData;
import org.egokituz.arduino2android.models.CPUData;
import org.egokituz.arduino2android.models.TestData;
import org.egokituz.arduino2android.models.TestError;
import org.egokituz.arduino2android.models.TestEvent;
import org.egokituz.arduino2android.models.TestStatistics;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.widget.Toast;

/**
 * @author Xabier Gardeazabal
 *
 */
public class TestApplication extends Application {

	public final static String TAG = "TestApplication"; // Tag to identify this class' messages in the console or LogCat

	
	//TODO REQUEST_ENABLE_BT is a request code that we provide (It's really just a number that you provide for onActivityResult)
	public static final int REQUEST_ENABLE_BT = 1;
	public static final int MESSAGE_DATA_READ = 2;
	public static final int MESSAGE_BATTERY_STATE_CHANGED = 4;
	public static final int MESSAGE_CPU_USAGE = 5;
	public static final int MESSAGE_PING_READ = 6;
	public static final int MESSAGE_ERROR_READING = 7;
	public static final int MESSAGE_BT_EVENT = 8;
	
	

	//// Module threads ///////////////////
	private BTManagerThread m_BTManager_thread;
	private BatteryMonitorThread m_BatteryMonitor_thread;
	private LoggerThread m_Logger_thread;
	private CPUMonitorThread m_cpuMonitor_thread;
	public StatisticsThread m_statistics_thread;
	
	public boolean m_finishApp; // Flag for managing activity termination

	private HashMap<String, Integer> m_testPlanParameters; // Used to store current plan settings


	private ArrayList<Handler> m_dataListeners;

	private Context m_AppContext;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		m_AppContext = getApplicationContext();

		// set flags
		m_finishApp = false;

		// set handler
		if(!m_handlerThread.isAlive())
			m_handlerThread.start();
		createHandler();

		// Instantiate modules
		try {
			m_BTManager_thread = new BTManagerThread(m_AppContext, mainAppHandler);
			//Toast.makeText(m_AppContext, "BT manager started", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			e.printStackTrace();

			// show a small message shortly (a Toast)
			Toast.makeText(m_AppContext, "Could not start the BT manager", Toast.LENGTH_SHORT).show();
		}

		m_BatteryMonitor_thread = new BatteryMonitorThread(m_AppContext, mainAppHandler);
		m_cpuMonitor_thread = new CPUMonitorThread(m_AppContext, mainAppHandler);
		m_Logger_thread = new LoggerThread(m_AppContext, mainAppHandler);
		m_statistics_thread = new StatisticsThread(m_AppContext, mainAppHandler);

		// Start the logger thread
		if(!m_Logger_thread.isAlive())
			m_Logger_thread.start();

		//Start the monitoring modules' threads
		if(!m_BatteryMonitor_thread.isAlive())
			m_BatteryMonitor_thread.start();
		if(!m_cpuMonitor_thread.isAlive())
			m_cpuMonitor_thread.start();
		if(!m_statistics_thread.isAlive())
			m_statistics_thread.start();
		
		m_dataListeners = new ArrayList<Handler>();
		registerTestDataListener(m_Logger_thread.loggerThreadHandler);
		registerTestDataListener(m_statistics_thread.statisticsThreadHandler);
	}
	
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		super.finalize();
		
		//Finalize threads
		if(!m_finishApp){
			m_BTManager_thread.finalize();
			m_BatteryMonitor_thread.finalize();
			m_cpuMonitor_thread.finalize();
			m_Logger_thread.finalize();
			//Shut down the HandlerThread
			m_handlerThread.quit();
		}
		m_finishApp = true;
	}
	

	/**
	 * Method called with the onClick event of the "Begin Test" button.
	 * <p>Retrieves the current test parapemeters from the app's preferences, 
	 * notifies the logger to begin its work, sends the test parameters to the
	 * BluetoothManager module, and finally starts said module.
	 */
	public void beginTest(){
		// Retrieve the test parameters from the app's settings/preferences
		m_testPlanParameters = (HashMap<String, Integer>) SettingsActivity.getCurrentPreferences(m_AppContext);

		// Tell the logger that a new Test has begun  //NOT ANYMORE: a new log folder may be created with the new parameters
		m_Logger_thread.loggerThreadHandler.obtainMessage(LoggerThread.MESSAGE_NEW_TEST, m_testPlanParameters).sendToTarget();

		//TODO: send the test parameters to the BluetoothManager-thread
		// Set the Bluetooth Manager's plan with the selected parameters
		Message sendMsg;
		sendMsg = m_BTManager_thread.btHandler.obtainMessage(BTManagerThread.MESSAGE_SET_SCENARIO,m_testPlanParameters); // TODO change the obj of the message
		sendMsg.sendToTarget();

		// Begin a new test
		m_BTManager_thread.start();
	}
	

	/**
	 * Handler connected with the BTManager Threads: 
	 */
	private HandlerThread m_handlerThread = new HandlerThread("MyHandlerThread");
	public Handler mainAppHandler;



	private void createHandler(){
		mainAppHandler = new Handler(m_handlerThread.getLooper()) {
			String sendMsg;
			byte[] readBuf;
			int elapsedMilis;
			int bytes;
			String devName, devMAC;
			long timestamp;
			long msgCount, errCount;
			ArduinoMessage msgReading;

			@SuppressLint("NewApi")
			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {
				case MESSAGE_PING_READ:
					// Message received from a running Arduino Thread
					// This message implies that 99 well formed PING messages were read by an Arduino Thread

					ArrayList<TestData> pingQueue = (ArrayList<TestData>) msg.obj;
					
					communicateToDataListeners(TestData.DATA_PING, pingQueue);
					
					// write to log file
					//m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_PING, pingQueue).sendToTarget();
					
					break;

				case MESSAGE_DATA_READ:
					// Message received from a running Arduino Thread
					// This message implies that 99 well formed DATA messages were read by an Arduino Thread

					ArrayList<TestData> dataQueue = (ArrayList<TestData>) msg.obj;
					communicateToDataListeners(TestData.DATA_STRESS, dataQueue);
					
					//m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_WRITE_DATA, dataQueue).sendToTarget();
					break;

				case MESSAGE_BATTERY_STATE_CHANGED:
					// Message received from the Battery-Monitor Thread
					// This message implies that the Battery percentage has changed

					/*
					Float batteryLoad = (Float) msg.obj;
					timestamp = msg.getData().getLong("TIMESTAMP");

					// call the Logger to write the battery load
					sendMsg = timestamp+" "+batteryLoad;
					m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_WRITE_BATTERY, sendMsg).sendToTarget();
					*/
					
					BatteryData battery = (BatteryData) msg.obj;
					communicateToDataListeners(TestData.DATA_BATTERY, battery);
					//m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_WRITE_BATTERY, battery).sendToTarget();
					//m_dataListener.obtainMessage(TestData.DATA_BATTERY, battery).sendToTarget();

					break;

				case MESSAGE_CPU_USAGE:
					// Message received from a running Arduino Thread
					// This message implies that a malformed message has been read by an Arduino Thread

					/*
					Float cpu = (Float) msg.obj;
					timestamp = msg.getData().getLong("TIMESTAMP");

					// call the Logger to write the battery load
					sendMsg = timestamp+" "+cpu;
					m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_CPU, sendMsg).sendToTarget();
					*/
					
					// send data to the ChartFragment 
					CPUData cpu = (CPUData) msg.obj;
					communicateToDataListeners(TestData.DATA_CPU, cpu);
					//m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_CPU, data).sendToTarget();
					//m_dataListener.obtainMessage(TestData.DATA_CPU, data).sendToTarget();

					break;

				case MESSAGE_ERROR_READING:
					// Message received from the CPU-Monitor Thread
					// This message implies that the CPU usage has changed

					TestError error = (TestError) msg.obj;

					communicateToDataListeners(TestData.DATA_ERROR, error);
					
					// write to log file
					//m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_ERROR, error).sendToTarget();

					break;

				case MESSAGE_BT_EVENT:
					// Message received from the CPU-Monitor Thread
					// This message implies that the CPU usage has changed

					TestEvent event = (TestEvent) msg.obj;
					communicateToDataListeners(TestData.DATA_EVENT, event);
					// write to log file
					//m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_EVENT, event).sendToTarget();

					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(m_AppContext, event.toString(), duration);
					toast.show();
					break;
					
				case TestData.DATA_STATISTIC:
					TestStatistics statistics = (TestStatistics) msg.obj;

					communicateToDataListeners(TestData.DATA_STATISTIC, statistics);
					break;
				}
			}
		};
	}
	
	/**
	 * @return the m_BTManager
	 */
	public BTManagerThread getBTManager() {
		return m_BTManager_thread;
	}

	/**
	 * @return the m_BatteryMonitor
	 */
	public BatteryMonitorThread getBatteryMonitor() {
		return m_BatteryMonitor_thread;
	}

	/**
	 * @return the m_Logger
	 */
	public LoggerThread getLogger() {
		return m_Logger_thread;
	}

	/**
	 * @return the m_cpuMonitor
	 */
	public CPUMonitorThread getCPUMonitor() {
		return m_cpuMonitor_thread;
	}
	
	public void registerTestDataListener(Handler h){
		m_dataListeners.add(h);
	}
	
	private void communicateToDataListeners(int what, Object o){
		for(Handler h : m_dataListeners){
			h.obtainMessage(what, o).sendToTarget();
		}
	}

}
