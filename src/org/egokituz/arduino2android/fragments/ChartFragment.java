/**
 * 
 */
package org.egokituz.arduino2android.fragments;

import java.util.ArrayList;
import java.util.HashMap;

import org.egokituz.arduino2android.R;
import org.egokituz.arduino2android.TestApplication;
import org.egokituz.arduino2android.activities.SettingsActivity;
import org.egokituz.arduino2android.models.BatteryData;
import org.egokituz.arduino2android.models.CPUData;
import org.egokituz.arduino2android.models.TestData;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

/**
 * This fragment shows a line-graph with the values of CPU usage and Battery level
 * @author Xabier Gardeazabal
 *
 */
public class ChartFragment extends Fragment implements OnChartValueSelectedListener{

	private static final String TAG = "ChartFragment";

	// Chart data-set index
	private static final int DATASET_BATTERY = 0;
	private static final int DATASET_CPU = 1;
	private static final int DATASET_PING = 2;
	private static final int DATASET_ERROR = 3;
	private static final int DATASET_EVENT = 4;

	// Chart data
	private LineDataSet m_cpuDataSet;
	private LineDataSet m_batteryDataSet;

	private int max_x_values = 50;


	/**
	 * Main context from the MainActivity
	 */
	private Context m_mainContext;

	/**
	 * The main Application for centralized data management and test control
	 */
	private TestApplication m_mainApp;

	private LineChart mChart;

	public Handler m_chartHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			Entry e;
			switch (msg.what) {
			case TestData.DATA_BATTERY:
				BatteryData battery = (BatteryData) msg.obj;
				e = new Entry(battery.batteryLevel, m_batteryDataSet.getEntryCount());
				addEntryToDataSets(DATASET_BATTERY, e);
				break;
				
			case TestData.DATA_CPU:
				CPUData cpu = (CPUData) msg.obj;
				e = new Entry(cpu.cpuLoad, m_cpuDataSet.getEntryCount());
				addEntryToDataSets(DATASET_CPU, e);
				break;
			case TestData.DATA_PING:

				break;
			case TestData.DATA_ERROR:

				break;
			case TestData.DATA_EVENT:

				break;
			default:
				break;
			}
		}
	};

	/**
	 * Constructor
	 */
	public ChartFragment() {
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

		m_mainApp.registerTestDataListener(m_chartHandler);
	}

	// this method is only called once for this fragment
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.v(TAG, "onCreate() chart fragment");
		
		// retain this fragment (so that when the activity's state changes, 
		// the configuration of this fragment is not lost
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,	Bundle savedInstanceState) {
		// Load the layout of this fragment
		View rootView = inflater.inflate(R.layout.fragment_chart, container, false);

		Log.v(TAG, "onCreateView() chart fragment");
		
		mChart = (LineChart) rootView.findViewById(R.id.chart1);

		mChart.setOnChartValueSelectedListener(this);
		mChart.setDrawYValues(false);
		mChart.setDrawGridBackground(false);
		mChart.setDescription("");
		mChart.setYRange(0, 100, false);


		createDataSets();
		initChart();

		mChart.invalidate(); // invalidates the whole view

		return rootView;
	}
	
	/**
	 * 
	 * @param dsIndex
	 * @param e
	 */
	private void addEntryToDataSets(int dsIndex, Entry e) {
		switch (dsIndex) {
		case DATASET_BATTERY:
			m_batteryDataSet.addEntry(e);
			break;

		case DATASET_CPU:
			m_cpuDataSet.addEntry(e);
			break;
		default:
			break;
		}

		// let the chart know it's data has changed
		mChart.notifyDataSetChanged();

		// redraw the chart
		mChart.invalidate();
	}

	/**
	 * Creates and initializes the data-sets of the chart
	 */
	private void createDataSets() {

		if(m_cpuDataSet == null){
			m_cpuDataSet = new LineDataSet(null, "CPU");
			m_cpuDataSet.setLineWidth(2.5f);
			m_cpuDataSet.setCircleSize(4.5f);
			m_cpuDataSet.setColor(Color.rgb(240, 99, 99));
			m_cpuDataSet.setCircleColor(Color.rgb(240, 99, 99));
			m_cpuDataSet.setHighLightColor(Color.rgb(190, 190, 190));
		}
		
		if(m_batteryDataSet == null){
			m_batteryDataSet = new LineDataSet(null, "Battery"); 
			m_batteryDataSet.setCircleSize(4.5f);
			m_batteryDataSet.setColor(Color.CYAN);
			m_batteryDataSet.setCircleColor(Color.CYAN);
			m_batteryDataSet.setHighLightColor(Color.LTGRAY);
		}
	}

	/**
	 * Loads the data-sets into the chart
	 */
	private void initChart(){
		// Retrieve the test parameters from the app's settings/preferences
		HashMap<String, Integer> m_testPlanParameters = (HashMap<String, Integer>) SettingsActivity.getCurrentPreferences(m_mainContext);
		max_x_values = m_testPlanParameters.get(SettingsActivity.PREF_CHART_X_VALUES);
		
		// create 30 x-vals
		String[] xVals = new String[max_x_values];

		for (int i = 0; i < max_x_values; i++)
			xVals[i] = "" + i;

		// create a chartdata object that contains only the x-axis labels (no entries or datasets)
		LineData data = new LineData(xVals);

		// Add datasets. WARNING: the order of addition matters!!!
		data.addDataSet(m_batteryDataSet);
		data.addDataSet(m_cpuDataSet);

		mChart.setData(data);
		mChart.invalidate();
	}


	@Override
	public void onValueSelected(Entry e, int dataSetIndex) {
		Toast.makeText(m_mainContext, e.toString(), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onNothingSelected() {

	}


	private LineDataSet createSet() {

		LineDataSet set = new LineDataSet(null, "DataSet 1");
		set.setLineWidth(2.5f);
		set.setCircleSize(4.5f);
		set.setColor(Color.rgb(240, 99, 99));
		set.setCircleColor(Color.rgb(240, 99, 99));
		set.setHighLightColor(Color.rgb(190, 190, 190));

		return set;
	}

	
	public void setMaxXAxisValues(int x){
		max_x_values = x;
		initChart();
	}






}
