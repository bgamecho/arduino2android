/**
 * 
 */
package org.egokituz.arduino2android.gui;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

/**
 * @author Xabier Gardeazabal
 *
 */
public class NumberPickerPreference extends DialogPreference{

	private static final int MIN_VALUE = 0;
	private static final int MAX_VALUE = 100;
	private static final boolean WRAP_SELECTOR_WHEEL = false;
	private int mSelectedValue;
	private final int mMinValue;
	private final int mMaxValue;
	private final boolean mWrapSelectorWheel;
	private NumberPicker mNumberPicker;
	
	public NumberPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
//		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference);
//		mMinValue = a.getInt(R.styleable.NumberPickerPreference_minValue, NumberPickerPreference.MIN_VALUE);
//		mMaxValue = a.getInt(R.styleable.NumberPickerPreference_maxValue, NumberPickerPreference.MAX_VALUE);
//		mWrapSelectorWheel = a.getBoolean(R.styleable.NumberPickerPreference_setWrapSelectorWheel, NumberPickerPreference.WRAP_SELECTOR_WHEEL);
//		a.recycle();
		
		mMinValue = 0;
		mMaxValue = 100;
		mWrapSelectorWheel = false;
		

		
//		setDialogLayoutResource(R.layout.number_picker_dialog);
//		setPositiveButtonText(android.R.string.ok);
//		setNegativeButtonText(android.R.string.cancel);
//
//		setDialogIcon(null);
	}
	
	@Override
	protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
		mSelectedValue = restoreValue ? this.getPersistedInt(0) : (Integer) defaultValue;
		this.updateSummary();
	}
	@Override
	protected Object onGetDefaultValue(final TypedArray a, final int index) {
		return a.getInteger(index, 0);
	}
	@Override
	protected void onPrepareDialogBuilder(final Builder builder) {
		super.onPrepareDialogBuilder(builder);
		mNumberPicker = new NumberPicker(this.getContext());
		mNumberPicker.setMinValue(mMinValue);
		mNumberPicker.setMaxValue(mMaxValue);
		mNumberPicker.setValue(mSelectedValue);
		mNumberPicker.setWrapSelectorWheel(mWrapSelectorWheel);
		mNumberPicker.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		final LinearLayout linearLayout = new LinearLayout(this.getContext());
		linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		linearLayout.setGravity(Gravity.CENTER);
		linearLayout.addView(mNumberPicker);
		builder.setView(linearLayout);
	}
	@Override
	protected void onDialogClosed(final boolean positiveResult) {
		if (positiveResult && this.shouldPersist()) {
			mSelectedValue = mNumberPicker.getValue();
			this.persistInt(mSelectedValue);
			this.updateSummary();
		}
	}
	private void updateSummary() {
		super.setSummary(super.getTitle() + " " + mSelectedValue);
	}
}