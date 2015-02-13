/**
 * 
 */
package org.egokituz.arduino2android.gui;

import org.egokituz.arduino2android.TestApplication;

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
	private TestApplication mMainApp;
	
	TestSectionFragment m_testFragment;
    
    public CustomPagerAdapter(FragmentManager fm, Context context, TestApplication app) {
        super(fm);
        mContext = context;
        mMainApp = app;
        
        m_testFragment = new TestSectionFragment();
		m_testFragment.setArguments(mContext, mMainApp);
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

		default:
			return "Section " + (position + 1);
		}
        
    }

	public void addItem(Fragment f, int i) {
		m_testFragment = (TestSectionFragment) f;
	}
}
