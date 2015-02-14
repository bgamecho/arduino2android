/**
 * 
 */
package org.egokituz.arduino2android.fragments;

import java.util.ArrayList;

import org.egokituz.arduino2android.R;
import org.egokituz.arduino2android.TestApplication;
import org.egokituz.arduino2android.models.CPUData;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * @author Xabier Gardeazabal
 *
 */
public class ChartFragment extends Fragment implements OnChartValueSelectedListener{

	private static final String TAG = "ChartFragment";

	// Handler message types
	public static final int DATA_BATTERY = 1;
	public static final int DATA_CPU = 2;
	public static final int DATA_PING = 3;
	public static final int DATA_ERROR = 4;
	public static final int DATA_EVENT = 5;

	// Chart data-set index
	private final int DATASET_BATTERY = 0;
	private final int DATASET_CPU = 1;
	private final int DATASET_PING = 2;
	private final int DATASET_ERROR = 3;
	private final int DATASET_EVENT = 4;

	// Chart data
	private LineDataSet m_cpuDataSet;
	private LineDataSet m_batteryDataSet;



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
			switch (msg.what) {
			case DATA_BATTERY:

				break;
			case DATA_CPU:
				CPUData cpu = (CPUData) msg.obj;
				Entry e = new Entry(cpu.cpuLoad, m_cpuDataSet.getEntryCount());
				addEntry(DATASET_CPU, e);
				break;
			case DATA_PING:

				break;
			case DATA_ERROR:

				break;
			case DATA_EVENT:

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

		// retain this fragment (so that when the activity's state changes, 
		// the configuration of this fragment is not lost
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,	Bundle savedInstanceState) {
		// Load the layout of this fragment
		View rootView = inflater.inflate(R.layout.activity_linechart_noseekbar, container, false);

		mChart = (LineChart) rootView.findViewById(R.id.chart1);

		mChart.setOnChartValueSelectedListener(this);
		mChart.setDrawYValues(false);
		mChart.setDrawGridBackground(false);
		mChart.setDescription("");

		createDataSets();
		
		/*
		addEmptyData();

		addEntry();
		addEntry();
		addEntry();addEntry();
		addDataSet();
		addEntry();
		addEntry();
		 */
		mChart.invalidate();

		return rootView;
	}


	private void addEntry(int dsIndex, Entry e) {

		LineData data = mChart.getData();

		if(data != null) {

			LineDataSet set = data.getDataSetByIndex(dsIndex);
			// set.addEntry(...);

			//data.addEntry(new Entry((float) (Math.random() * 50) + 50f, set.getEntryCount()), 0);
			data.addEntry(e, dsIndex);

			// let the chart know it's data has changed
			mChart.notifyDataSetChanged();

			// redraw the chart
			mChart.invalidate();   
		}
	}

	private void createDataSets() {

		m_cpuDataSet = new LineDataSet(null, "CPU");
		m_cpuDataSet.setLineWidth(2.5f);
		m_cpuDataSet.setCircleSize(4.5f);
		m_cpuDataSet.setColor(Color.rgb(240, 99, 99));
		m_cpuDataSet.setCircleColor(Color.rgb(240, 99, 99));
		m_cpuDataSet.setHighLightColor(Color.rgb(190, 190, 190));

		m_batteryDataSet = new LineDataSet(null, "Battery"); 
		m_batteryDataSet.setCircleSize(4.5f);
		m_batteryDataSet.setColor(Color.CYAN);
		m_batteryDataSet.setCircleColor(Color.CYAN);
		m_batteryDataSet.setHighLightColor(Color.LTGRAY);
		
		// create 30 x-vals
		String[] xVals = new String[300];

		for (int i = 0; i < 300; i++)
			xVals[i] = "" + i;

		// create a chartdata object that contains only the x-axis labels (no entries or datasets)
		LineData data = new LineData(xVals);
		
		// Add datasets. WARNING: the order of addition matters!!!
		data.addDataSet(m_batteryDataSet);
		data.addDataSet(m_cpuDataSet);
		
		mChart.setData(data);
		mChart.invalidate();
		
		
	}


	//----------------------------------------


	private void addEmptyData() {

		// create 30 x-vals
		String[] xVals = new String[30];

		for (int i = 0; i < 30; i++)
			xVals[i] = "" + i;

		// create a chartdata object that contains only the x-axis labels (no entries or datasets)
		LineData data = new LineData(xVals);

		mChart.setData(data);
		mChart.invalidate();
	}




	int[] mColors = ColorTemplate.VORDIPLOM_COLORS;

	private void addEntry() {

		LineData data = mChart.getData();

		if(data != null) {

			LineDataSet set = data.getDataSetByIndex(0);
			// set.addEntry(...);

			if (set == null) {
				set = createSet();
				data.addDataSet(set);
			}

			data.addEntry(new Entry((float) (Math.random() * 50) + 50f, set.getEntryCount()), 0);

			// let the chart know it's data has changed
			mChart.notifyDataSetChanged();

			// redraw the chart
			mChart.invalidate();   
		}
	}

	private void addDataSet() {

		LineData data = mChart.getData();

		if(data != null) {

			int count = (data.getDataSetCount() + 1);

			// create 10 y-vals
			ArrayList<Entry> yVals = new ArrayList<Entry>();

			for (int i = 0; i < data.getXValCount(); i++)
				yVals.add(new Entry((float) (Math.random() * 50f) + 50f * count, i));

			LineDataSet set = new LineDataSet(yVals, "DataSet " + count);
			set.setLineWidth(2.5f);
			set.setCircleSize(4.5f);

			int color = mColors[count % mColors.length];

			set.setColor(color);
			set.setCircleColor(color);
			set.setHighLightColor(color);

			data.addDataSet(set);
			mChart.notifyDataSetChanged();
			mChart.invalidate();   
		}
	}

	private void removeDataSet() {

		LineData data = mChart.getData();

		if(data != null) {

			data.removeDataSet(data.getDataSetByIndex(data.getDataSetCount() - 1));

			mChart.notifyDataSetChanged();
			mChart.invalidate();   
		}
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








}