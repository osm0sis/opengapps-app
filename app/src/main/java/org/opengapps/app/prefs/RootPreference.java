package org.opengapps.app.prefs;

import android.content.Context;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import org.opengapps.app.R;

import eu.chainfire.libsuperuser.Shell;

public class RootPreference extends SwitchPreference implements Preference.OnPreferenceChangeListener {
    public RootPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean success = !((Boolean) newValue) || Shell.SU.run("") != null;
        if (!success) {
            Toast.makeText(getContext(), R.string.label_root_is_need_for_rootmode, Toast.LENGTH_SHORT).show();
        }
        return success;
    }
}
