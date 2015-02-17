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


package org.egokituz.arduino2android.activities;

import org.egokituz.arduino2android.R;
import org.egokituz.arduino2android.TestApplication;
import org.egokituz.arduino2android.adapters.CustomPagerAdapter;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Application;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * @author xgardeazabal
 */
public class MainActivity extends FragmentActivity implements ActionBar.TabListener {

	public final static String TAG = "MainActivity"; // Tag to identify this class' messages in the console or LogCat

	public static final int SETTINGS_RESULT = 2; 


	/**
	 * Main Context of the app
	 */
	Context m_ActivityContext; // Main Activity Context

	/**
	 * The {@link Application} for centralized data management and test control
	 */
	public TestApplication m_mainApp;
	
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
	 * three primary sections of the app. We use a {@link android.support.v4.app.FragmentPagerAdapter}
	 * derivative, which will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	CustomPagerAdapter m_CustomPagerAdapter;

	/**
	 * The {@link ViewPager} that will display the three primary sections of the app, one at a time.
	 */
	ViewPager mViewPager;
	
	/**
	 * Holder of the main tab/fragmen/section
	 */
	//private TestSectionFragment m_TestFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		m_ActivityContext = this;
	    m_mainApp = (TestApplication)getApplication();	    
	    
		setContentView(R.layout.activity_main);
		
		// Create the adapter that will return a fragment for each of the three primary sections
		// of the app.
        m_CustomPagerAdapter = new CustomPagerAdapter(getSupportFragmentManager(), m_ActivityContext, m_mainApp);
        
        /*
        // find the retained fragment on activity restarts
        m_TestFragment = (TestSectionFragment) m_CustomPagerAdapter.getItem(0);

        if(m_TestFragment == null){
        	m_TestFragment = new TestSectionFragment();
        	m_TestFragment.setArguments(m_ActivityContext, m_mainApp);
        	m_CustomPagerAdapter.addItem(m_TestFragment, 0);
        }*/
 
		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		
		// Specify that the Home/Up button should not be enabled, since there is no hierarchical
		// parent.
		//actionBar.setHomeButtonEnabled(false);
		
		// Specify that we will be displaying tabs in the action bar.
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		// Set up the ViewPager, attaching the adapter and setting up a listener for when the
		// user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(m_CustomPagerAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				// When swiping between different app sections, select the corresponding tab.
				// We can also use ActionBar.Tab#select() to do this if we have a reference to the
				// Tab.
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < m_CustomPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by the adapter.
			// Also specify this Activity object, which implements the TabListener interface, as the
			// listener for when this tab is selected.
			actionBar.addTab(
					actionBar.newTab()
					.setText(m_CustomPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.help:
			//showHelp();
			return true;

		case R.id.preferences:
			// Call SettingsActivity intent
			Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
			startActivityForResult(i, SETTINGS_RESULT);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}



	@Override
	public void onResume(){
		super.onResume();
		Log.v(TAG, "Arduino Activity --OnResume()--");
	}

	@Override
	public void onPause(){
		Log.v(TAG, "Arduino Activity --OnPause()--");
		super.onPause();
	}

	@Override
	public void onStop(){
		Log.v(TAG, "Arduino Activity --OnStop()--");
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		Log.v(TAG, "Arduino Activity --OnBackPressed()--");
		super.onBackPressed();
	}

	@Override
	public void onRestart(){
		super.onRestart();
		Log.v(TAG, "Arduino Activity --OnRestart()--");
	}

	@Override
	public void onDestroy(){
		Log.v(TAG, "Arduino Activity --OnDestroy()--");
		super.onDestroy();
	}


	@Override
	public void finish() {
		Log.v(TAG, "Arduino Activity --OnDestroy()--");
		super.finish();
	}

	

	 



	/**
	 * Updates the ListView containing the connected Arduinos

	private void populateDeviceListView() {
		devicesListView = (ListView) findViewById(R.id.listViewDevices);

		final String[] myDeviceList = getConnectedDevices();

		if(myDeviceList != null){
			ArrayAdapter<String> listViewArrayAdapter = new ArrayAdapter<String>(this, 
					android.R.layout.simple_list_item_1, myDeviceList);
			devicesListView.setAdapter(listViewArrayAdapter);
		}
	}*/






	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// When the given tab is selected, switch to the corresponding page in the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
	}





}
