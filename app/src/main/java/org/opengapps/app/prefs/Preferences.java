package org.opengapps.app.prefs;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.codekidlabs.storagechooser.StorageChooser;
import com.codekidlabs.storagechooser.StorageChooserView;
import com.codekidlabs.storagechooser.utils.DiskUtil;

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
            bindPreferenceSummaryToValue(p);

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
            final SharedPreferences sharedPreferences = getActivity().getSharedPreferences(prefName, MODE_PRIVATE);
            // set summary of current download path
            preference.setSummary(sharedPreferences.getString(DiskUtil.SC_PREFERENCE_KEY,""));

            // Set the listener to watch for value changes.
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    StorageChooser chooser = new StorageChooser.Builder()
                            .withActivity(getActivity())
                            .withMemoryBar(true)
                            .withFragmentManager(Preferences.fragmentManager)
                            .withPredefinedPath(Downloader.OPENGAPPS_PREDEFINED_PATH)
                            .withPreference(sharedPreferences)
                            .actionSave(true)
                            .allowCustomPath(true)
                            .allowAddFolder(true)
                            .build();

                    chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
                        @Override
                        public void onSelect(String s) {
                            // set summary after selection of new path
                            preference.setSummary(s);
                        }
                    });

                    // set view and strings for localization
                    StorageChooserView.setViewSc(StorageChooserView.SC_LAYOUT_SHEET);
                    // overview texts
                    StorageChooserView.setChooserHeading(getString(R.string.title_storage_chooser));
                    StorageChooserView.setInternalStorageText(getString(R.string.title_storage_chooser_internal));
                    // button labels
                    StorageChooserView.setLabelCreate(getString(R.string.label_create));
                    StorageChooserView.setLabelSelect(getString(R.string.label_select));
                    StorageChooserView.setLabelNewFolder(getString(R.string.new_folder));
                    // textfield strings
                    StorageChooserView.setTextfieldHint(getString(R.string.hint_folder_name));
                    StorageChooserView.setTextfieldError(getString(R.string.error_tip_empty_field));
                    // toast strings
                    StorageChooserView.setToastFolderCreated(getString(R.string.create_folder_success));
                    StorageChooserView.setToastFolderError(getString(R.string.create_folder_error));

                    // { memory text, memory bar, new folder label, select label, cancel label }
                    int[] nightColors = {R.color.white_fifty_seven, R.color.colorAccent, android.R.color.white,
                            R.color.colorAccent,android.R.color.white};
                    StorageChooserView.setNightColors(nightColors);

                    chooserDayNightSetup(chooser);

                    chooser.show();
                    return true;
                }
            });
        }

        private void chooserDayNightSetup(StorageChooser chooser) {
            int currentNightMode = getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_NO:
                    // Night mode is not active, we're in day time
                    chooser.setMode(StorageChooser.DAY_MODE);
                    break;
                case Configuration.UI_MODE_NIGHT_YES:
                    // Night mode is active, we're at night!
                    chooser.setMode(StorageChooser.NIGHT_MODE);
                    break;
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    // We don't know what mode we're in, assume notnight
                    chooser.setMode(StorageChooser.DAY_MODE);
                    break;
            }
        }
    }
}
