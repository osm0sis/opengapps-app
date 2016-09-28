package org.opengapps.opengapps.prefs;

import android.content.Context;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import eu.chainfire.libsuperuser.Shell;

public class RootPreference extends SwitchPreference implements Preference.OnPreferenceChangeListener {
    public RootPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onClick() {
        super.onClick();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return !((Boolean) newValue) || Shell.SU.run("") != null;
    }
}
