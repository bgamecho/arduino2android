/**
 * 
 */
package org.egokituz.arduino2android.fragments;

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.egokituz.arduino2android.R;
import org.egokituz.arduino2android.TestApplication;
import org.egokituz.arduino2android.models.BatteryData;
import org.egokituz.arduino2android.models.CPUData;
import org.egokituz.arduino2android.models.TestData;
import org.egokituz.arduino2android.models.TestStatistics;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;

/**
 * This class shows the live statistics or values that other components of the system are sharing
 * 
 * @author Xabier Gardeazabal
 *
 */
public class StatisticsFragment extends Fragment {

	private static final String TAG = "StatisticsFragment";
	
	/**
	 * Main context from the MainActivity
	 */
	private Context m_mainContext;

	/**
	 * The main Application for centralized data management and test control
	 */
	private TestApplication m_mainApp;
	
	private ArrayList<Handler> m_listeners = new ArrayList();

	// The text views to show the statistics' data
	private TextView m_battery_view;
	private TextView m_meanBattery_view;
	private TextView m_cpu_view;
	private TextView m_meanCPU_view;
	private TextView m_discoveries_view;
	private TextView m_btSpeed_view;
	private TextView m_errorRate_view;
	private TextView m_errors_view;
	private TextView m_transfers_view;
	private TextView m_meanPing_view;
	private TextView m_messageCountView;
	
	private final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
	

	/**
	 * The handler of the StatisticsFragmen
	 */
	public Handler m_statisticsFragmentHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			Entry e;
			switch (msg.what) {
			case TestData.DATA_BATTERY:
				BatteryData battery = (BatteryData) msg.obj;
				if(m_battery_view != null)
					m_battery_view.setText(DECIMAL_FORMAT.format(battery.batteryLevel)+" %");

				break;
			case TestData.DATA_CPU:
				CPUData cpu = (CPUData) msg.obj;
				
				if(m_cpu_view != null){
					m_cpu_view.setText(DECIMAL_FORMAT.format(cpu.cpuLoad)+" %");
				}
				break;
			case TestData.DATA_PING:

				break;
			case TestData.DATA_ERROR:

				break;
			case TestData.DATA_EVENT:

				break;
			case TestData.DATA_STATISTIC:
				TestStatistics s = (TestStatistics) msg.obj;
				if(m_meanBattery_view != null)
					m_meanBattery_view.setText(DECIMAL_FORMAT.format(s.batteryDrainHour)+"");
				if(m_meanCPU_view != null)
					m_meanCPU_view.setText(DECIMAL_FORMAT.format(s.meanCPU)+"");
				if(m_discoveries_view != null)
					m_discoveries_view.setText(s.totalDiscoveries+"");
				if(m_btSpeed_view != null){
					if(s.btSpeed < 1000)
						m_btSpeed_view.setText(DECIMAL_FORMAT.format(s.btSpeed)+" B/s");
					else if(s.btSpeed < 1000000)
						m_btSpeed_view.setText(DECIMAL_FORMAT.format(s.btSpeed/1000.0)+" KB/s");
					else
						m_btSpeed_view.setText(DECIMAL_FORMAT.format(s.btSpeed/1000000)+" MB/s");
				}
				if(m_errors_view != null)
					m_errors_view.setText(s.totalErrors+"");
				if(m_errorRate_view != null && s.totalMessages != 0){
					String errors = DECIMAL_FORMAT.format(((float) s.totalErrors/(float) s.totalMessages));
					m_errorRate_view.setText(errors + " %");
				}
				if(m_meanPing_view != null)
					m_meanPing_view.setText(DECIMAL_FORMAT.format(s.meanPing)+" ms.");
				if(m_transfers_view != null){
					if(s.transferedBytes < 1000)
						m_transfers_view.setText(s.transferedBytes+" bytes");
					else if (s.transferedBytes < 1000000)
						m_transfers_view.setText(DECIMAL_FORMAT.format(s.transferedBytes/1000.0)+" KB");
					else
						m_transfers_view.setText(DECIMAL_FORMAT.format(s.transferedBytes/1000000.0)+" MB");
				}
				if(m_messageCountView != null)
					m_messageCountView.setText(s.totalMessages+"");
				break;
			default:
				break;
			}
		}
	};
	/**
	 * Constructor
	 */
	public StatisticsFragment() {
		super();
	}

	/**
	 * 
	 * @param c The main context
	 * @param app The main Application for centralized data management and test control
	 */
	public void setArguments(Context c, TestApplication app) {
		m_mainContext = c;
		m_mainApp = app;
		
		m_mainApp.registerTestDataListener(m_statisticsFragmentHandler);
	}
	

	// this method is only called once for this fragment
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// retain this fragment (so that when the activity's state changes, 
		// the configuration of this fragment is not lost
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,	Bundle savedInstanceState) {
		// Load the layout of this fragment
		View rootView = inflater.inflate(R.layout.fragment_statistics, container, false);

		m_battery_view = (TextView) rootView.findViewById(R.id.textView_battery);
		m_meanBattery_view = (TextView) rootView.findViewById(R.id.textView_meanBattery);
		m_cpu_view = (TextView) rootView.findViewById(R.id.textView_cpu);
		m_meanCPU_view = (TextView) rootView.findViewById(R.id.textView_meanCPU);
		m_discoveries_view = (TextView) rootView.findViewById(R.id.textView_discoveries);
		m_btSpeed_view = (TextView) rootView.findViewById(R.id.textView_btSpeed);
		m_errorRate_view = (TextView) rootView.findViewById(R.id.textView_totalErrorRate);
		m_errors_view = (TextView) rootView.findViewById(R.id.textView_totalErrors);
		m_transfers_view = (TextView) rootView.findViewById(R.id.textView_transferred);
		m_meanPing_view= (TextView) rootView.findViewById(R.id.textView_meanPing);
		m_messageCountView= (TextView) rootView.findViewById(R.id.textView_totalMessageCount);
		
		
		return rootView;
	}

	
	

}
