package org.opengapps.app.prefs;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;

import com.codekidlabs.storagechooser.StorageChooserBuilder;
import com.codekidlabs.storagechooser.utils.MemoryUtil;

import org.opengapps.app.BuildConfig;
import org.opengapps.app.R;
import org.opengapps.app.download.Downloader;

public class Preferences extends AppCompatActivity {
    public final static String prefName = BuildConfig.APPLICATION_ID + "_preferences";
    public final static String DOWNLOAD_PATH_KEY = "user_download_path";
    public static FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_toolbar);
        fragmentManager = getSupportFragmentManager();
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @SuppressWarnings("WeakerAccess")
    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            Preference p = findPreference("download_dir");
            if(MemoryUtil.isExternalStoragePresent()) {
                bindPreferenceSummaryToValue(p);
            } else {
                PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("cat_download");
                preferenceCategory.removePreference(p);
            }

            setDarkModeListener();
        }

        private void setDarkModeListener() {
            Preference nightMode = findPreference("nightMode");
            nightMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (((Boolean) o)) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                    getActivity().recreate();
                    return true;
                }
            });
        }

        private void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    StorageChooserBuilder.Builder builder = new StorageChooserBuilder.Builder()
                            .withActivity(getActivity())
                            .withMemoryBar(true)
                            .withFragmentManager(Preferences.fragmentManager)
                            .withPredefinedPath(Downloader.OPENGAPPS_PREDEFINED_PATH)
                            .withPreference(getActivity().getSharedPreferences(prefName, MODE_PRIVATE))
                            .actionSave(true)
                            .setDialogTitle(getActivity().getString(R.string.storage_chooser_title)
                            .setInternalStorageText(getActivity().getString(R.string.storage_chooser_internal_text))
                            .build();

                    builder.show();
                    return false;
                }
            });
        }
    }
}
