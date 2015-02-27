/**
 * 
 */
package org.egokituz.arduino2android.adapters;

import org.egokituz.arduino2android.TestApplication;
import org.egokituz.arduino2android.activities.MainActivity;
import org.egokituz.arduino2android.fragments.ChartFragment;
import org.egokituz.arduino2android.fragments.StatisticsFragment;
import org.egokituz.arduino2android.fragments.TestSectionFragment;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * {@link FragmentPagerAdapter} managing the different tab-fragments of the {@link MainActivity}
 * 
 * @author Xabier Gardeazabal
 *
 */
public class CustomPagerAdapter extends FragmentPagerAdapter{
    private Context mContext;
	private TestApplication m_MainApp;
	
	private TestSectionFragment m_testFragment;
	private ChartFragment m_chart;
	private StatisticsFragment m_statistics;
    
    public CustomPagerAdapter(FragmentManager fm, Context context, TestApplication app) {
        super(fm);
        mContext = context;
        m_MainApp = app;
        
        m_testFragment = new TestSectionFragment();
		m_testFragment.setArguments(mContext, m_MainApp);
		
		m_chart = new ChartFragment();
		m_chart.setArguments(mContext, m_MainApp);
		
		m_statistics = new StatisticsFragment();
		m_statistics.setArguments(mContext, m_MainApp);
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

		case 2:
			return m_statistics;
		default:
	        return null;
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
		case 2:
			return "Statistics";
		default:
			return "Section " + (position + 1);
		}
        
    }

	public void addItem(Fragment f, int i) {
		m_testFragment = (TestSectionFragment) f;
	}
}
