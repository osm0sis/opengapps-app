package org.opengapps.app.prefs;

import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import org.opengapps.app.DownloadFragment;
import org.opengapps.app.R;

public class NumberPickerPreference extends Preference {
    private final AlertDialog alertDialog;
    private final NumberPicker numberPicker;
    private final SharedPreferences prefs;

    public NumberPickerPreference(final Context context, AttributeSet attrs) {
        super(context, attrs);
        prefs = getContext().getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        numberPicker = new NumberPicker(getContext());
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(10);
        numberPicker.setValue(1);
        numberPicker.setWrapSelectorWheel(false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(DownloadFragment.dpToPx(context, 24), 0, DownloadFragment.dpToPx(context, 24), 0);
        numberPicker.setLayoutParams(layoutParams);
        linearLayout.addView(numberPicker);
        if (false) { //THIS WILL GET ADDED IN 1.1.1!
            final CheckBox checkBox = new CheckBox(context);
            checkBox.setVisibility(View.INVISIBLE);
            checkBox.setText(R.string.explanation_cleanup_now);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(DownloadFragment.dpToPx(context, 8), 0, DownloadFragment.dpToPx(context, 8), 0);
            checkBox.setLayoutParams(params);
            numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    if (newVal >= prefs.getInt(getKey(), Integer.MIN_VALUE)) {
                        checkBox.setVisibility(View.INVISIBLE);
                    } else {
                        checkBox.setVisibility(View.VISIBLE);
                    }
                }
            });
            linearLayout.addView(checkBox);
        }
        setSummary(context.getString(R.string.explanation_keep_packages, prefs.getInt(getKey(), 1)));
        alertDialog = new AlertDialog.Builder(context)
                .setTitle(getTitle())
                .setView(linearLayout)
                .setNegativeButton(R.string.label_cancel, null)
                .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (numberPicker.getValue() < prefs.getInt(getKey(), Integer.MIN_VALUE)) {
                            new AlertDialog.Builder(context)
                                    .setMessage(R.string.explanation_cleanup_now)
                                    .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            DownloadFragment.cleanUp(context);
                                        }
                                    })
                                    .setNegativeButton(R.string.label_cancel, null)
                                    .show();
                        }
                        persistInt(numberPicker.getValue());
                        setSummary(getContext().getString(R.string.explanation_keep_packages, getPersistedInt(1)));
//                        if (checkBox.isChecked()) {
//                            DownloadFragment.cleanUp(context);
//                        }
                    }
                })
                .create();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        view.setPaddingRelative(100, 0, 0, 0);
    }

    @Override
    protected void onClick() {
        super.onClick();
        numberPicker.setValue(getPersistedInt(1));
        alertDialog.show();
    }
}
