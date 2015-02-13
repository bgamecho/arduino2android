/**
 * 
 */
package org.egokituz.arduino2android.preferences;

import org.egokituz.arduino2android.R;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Xabier Gardeazabal
 *
 */
public class SeekbarPreference extends DialogPreference{

	public SeekbarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

	}

	@Override
	protected View onCreateView( ViewGroup parent )
	{
		LayoutInflater li = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		return li.inflate( R.layout.seekbar_preference, parent, false);
	}
}
