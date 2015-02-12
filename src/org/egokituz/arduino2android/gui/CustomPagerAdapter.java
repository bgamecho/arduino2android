/**
 * 
 */
package org.egokituz.arduino2android.gui;

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
    protected Context mContext;
    
    public CustomPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }
 
    @Override
    // This method returns the fragment associated with
    // the specified position.
    //
    // It is called when the Adapter needs a fragment
    // and it does not exists.
    public Fragment getItem(int position) {
 
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
 
    @Override
    public int getCount() {
        return 3;
    }
    

//    @Override
//    public CharSequence getPageTitle(int position) {
//        return "Section " + (position + 1);
//    }
}
