package org.egokituz.arduino2android.gui;

import org.egokituz.arduino2android.R;
import org.egokituz.arduino2android.R.id;
import org.egokituz.arduino2android.R.layout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DemoFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_demo, container, false);
 
        // Get the arguments that was supplied when
        // the fragment was instantiated in the
        // CustomPagerAdapter
        Bundle args = getArguments();
        ((TextView) rootView.findViewById(R.id.text)).setText("Page " + args.getInt("page_position"));
 
        return rootView;
    }
}
