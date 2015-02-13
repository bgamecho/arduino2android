/**
 * 
 */
package org.egokituz.arduino2android;

import java.util.ArrayList;
import java.util.HashMap;

import org.egokituz.arduino2android.gui.SettingsActivity;
import org.egokituz.utils.ArduinoMessage;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
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

	private ArrayList<String> m_testParameters = new ArrayList<>();

	public boolean m_finishApp; // Flag for managing activity termination

	private HashMap m_testPlanParameters; // Used to store current plan settings


	private Context m_context;
	
	public TestApplication() {
		/**
		 * ##############################################
		 */
		// TODO CHECK IF THIS IS THE MAIN CONTEXT
		m_context = getBaseContext();
	}

	public TestApplication(Context c) {
		m_context = c; 
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		if(m_context == null)
			m_context = getBaseContext();
		
		m_finishApp = false;

		if(!handlerThread.isAlive())
			handlerThread.start();
		createHandler();

		try {
			m_BTManager_thread = new BTManagerThread(m_context, arduinoHandler);
		} catch (Exception e) {
			e.printStackTrace();

			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(m_context, "Could not start the BT manager", duration);
			toast.show();
		}

		m_BatteryMonitor_thread = new BatteryMonitorThread(m_context, arduinoHandler);
		m_cpuMonitor_thread = new CPUMonitorThread(m_context, arduinoHandler);
		m_Logger_thread = new LoggerThread(m_context, arduinoHandler);

		// Start the logger thread
		if(!m_Logger_thread.isAlive())
			m_Logger_thread.start();

		//Start the monitoring modules' threads
		if(!m_BatteryMonitor_thread.isAlive())
			m_BatteryMonitor_thread.start();
		if(!m_cpuMonitor_thread.isAlive())
			m_cpuMonitor_thread.start();
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
			handlerThread.quit();
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
		m_testPlanParameters = (HashMap) SettingsActivity.getCurrentPreferences(m_context);

		// Tell the logger that a new Test has begun  //NOT ANYMORE: a new log folder may be created with the new parameters
		m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_NEW_TEST, m_testPlanParameters).sendToTarget();

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
	private HandlerThread handlerThread = new HandlerThread("MyHandlerThread");
	public Handler arduinoHandler;
	private void createHandler(){
		arduinoHandler = new Handler(handlerThread.getLooper()) {
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

					ArrayList<String> pingQueue = (ArrayList<String>) msg.obj;
					// write to log file
					m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_PING, pingQueue).sendToTarget();
					break;

				case MESSAGE_DATA_READ:
					// Message received from a running Arduino Thread
					// This message implies that 99 well formed DATA messages were read by an Arduino Thread

					ArrayList<String> dataQueue = (ArrayList<String>) msg.obj;
					m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_WRITE_DATA, dataQueue).sendToTarget();
					break;

				case MESSAGE_BATTERY_STATE_CHANGED:
					// Message received from the Battery-Monitor Thread
					// This message implies that the Battery percentage has changed

					Float batteryLoad = (Float) msg.obj;
					timestamp = msg.getData().getLong("TIMESTAMP");

					// call the Logger to write the battery load
					sendMsg = timestamp+" "+batteryLoad;
					m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_WRITE_BATTERY, sendMsg).sendToTarget();

					break;

				case MESSAGE_CPU_USAGE:
					// Message received from a running Arduino Thread
					// This message implies that a malformed message has been read by an Arduino Thread

					Float cpu = (Float) msg.obj;
					timestamp = msg.getData().getLong("TIMESTAMP");

					// call the Logger to write the battery load
					sendMsg = timestamp+" "+cpu;
					m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_CPU, sendMsg).sendToTarget();

					break;

				case MESSAGE_ERROR_READING:
					// Message received from the CPU-Monitor Thread
					// This message implies that the CPU usage has changed

					String error = (String) msg.obj;

					// write to log file
					m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_ERROR, error).sendToTarget();

					break;

				case MESSAGE_BT_EVENT:
					// Message received from the CPU-Monitor Thread
					// This message implies that the CPU usage has changed

					String event = (String) msg.obj;

					// write to log file
					m_Logger_thread.m_logHandler.obtainMessage(LoggerThread.MESSAGE_EVENT, event).sendToTarget();

					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(m_context, event, duration);
					toast.show();
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

	public void setContext(Context c) {
		m_context = c;
	}
}
