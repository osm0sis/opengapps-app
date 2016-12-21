package org.opengapps.app.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import org.opengapps.app.R;

public class NumberPickerPreference extends Preference {
    private final AlertDialog alertDialog;
    private final NumberPicker numberPicker;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        numberPicker = new NumberPicker(getContext());
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(10);
        numberPicker.setValue(1);
        final SharedPreferences prefs = getContext().getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
        setSummary(context.getString(R.string.explanation_keep_packages, prefs.getInt(getKey(), 1)));
        alertDialog = new AlertDialog.Builder(context)
                .setTitle(getTitle())
                .setView(numberPicker)
                .setNegativeButton(R.string.label_cancel, null)
                .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        persistInt(numberPicker.getValue());
                        setSummary(getContext().getString(R.string.explanation_keep_packages, getPersistedInt(1)));
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
