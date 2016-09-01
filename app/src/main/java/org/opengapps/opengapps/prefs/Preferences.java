package org.opengapps.opengapps.prefs;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ContentFrameLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import org.opengapps.opengapps.R;

public class Preferences extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ContentFrameLayout root = (ContentFrameLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        AppBarLayout bar = (AppBarLayout) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0); // insert at top
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    public static class SettingsFragment extends PreferenceFragment{

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            bindPreferenceSummaryToValue(findPreference("download_dir"));
        }

        private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();
                if (preference instanceof ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                            index >= 0
                                    ? listPreference.getEntries()[index]
                                    : null);
                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.setSummary(stringValue);
                }
                return true;
            }
        };

        private void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }
}
