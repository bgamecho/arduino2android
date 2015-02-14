/**
 * 
 */
package org.egokituz.arduino2android.adapters;

import org.egokituz.arduino2android.TestApplication;
import org.egokituz.arduino2android.fragments.ChartFragment;
import org.egokituz.arduino2android.fragments.DemoFragment;
import org.egokituz.arduino2android.fragments.TestSectionFragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * @author Xabier Gardeazabal
 *
 */
public class CustomPagerAdapter extends FragmentPagerAdapter{
    private Context mContext;
	private TestApplication m_MainApp;
	
	private TestSectionFragment m_testFragment;
	private ChartFragment m_chart;
    
    public CustomPagerAdapter(FragmentManager fm, Context context, TestApplication app) {
        super(fm);
        mContext = context;
        m_MainApp = app;
        
        m_testFragment = new TestSectionFragment();
		m_testFragment.setArguments(mContext, m_MainApp);
		
		m_chart = new ChartFragment();
		m_chart.setArguments(mContext, m_MainApp);
    }
 
    @Override
    // This method returns the fragment associated with
    // the specified position.
    //
    // It is called when the Adapter needs a fragment
    // and it does not exist.
    public Fragment getItem(int position) {
    	
		switch (position) {
		case 0:
			// The first section of the app is the test activity
			return m_testFragment;
		case 1:
			return m_chart;

		default:
	        // Create fragment object
	        Fragment fragment = new DemoFragment();
	 
	        // Attach some data to it that we'll
	        // use to populate our fragment layouts
	        Bundle args = new Bundle();
	        args.putInt("page_position", position + 1);
	 
	        // Set the arguments on the fragment
	        // that will be fetched in DemoFragment@onCreateView
	        fragment.setArguments(args);
	 
	        return fragment;
		}
    }
    
    
 
    @Override
    public int getCount() {
        return 3;
    }
    

    @Override
    public CharSequence getPageTitle(int position) {
    	switch (position) {
		case 0:
			return "Test manager";
		case 1:
			return "Line Chart";
		default:
			return "Section " + (position + 1);
		}
        
    }

	public void addItem(Fragment f, int i) {
		m_testFragment = (TestSectionFragment) f;
	}
}
