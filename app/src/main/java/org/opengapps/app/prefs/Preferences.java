package org.opengapps.app.prefs;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;

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

        private StorageChooser.Builder builder;
        private SharedPreferences sharedPreferences;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            Preference p = findPreference("download_dir");
            bindPreferenceSummaryToValue(p);

            configureChooser();
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
            sharedPreferences = getActivity().getSharedPreferences(prefName, MODE_PRIVATE);
            // set summary of current download path
            preference.setSummary(sharedPreferences.getString("download_dir", Environment.getExternalStorageDirectory().getAbsolutePath() + Downloader.OPENGAPPS_PREDEFINED_PATH));

            // Set the listener to watch for value changes.
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    StorageChooser chooser = builder.build();
                    chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
                        @Override
                        public void onSelect(String s) {
                            // set summary after selection of new path
                            preference.setSummary(s);
                            sharedPreferences.edit().putString("download_dir", s).apply();
                        }
                    });

                    chooser.show();

                    return true;
                }
            });
        }

        private void configureChooser() {


            Content content = new Content();
            content.setOverviewHeading(getString(R.string.title_storage_chooser));
            content.setCreateLabel(getString(R.string.label_create));
            content.setCancelLabel(getString(R.string.label_cancel));
            content.setSelectLabel(getString(R.string.label_select));
            content.setInternalStorageText(getString(R.string.title_storage_chooser_internal));
            content.setNewFolderLabel(getString(R.string.new_folder));
            content.setTextfieldHintText(getString(R.string.hint_folder_name));
            content.setTextfieldErrorText(getString(R.string.error_tip_empty_field));
            content.setFolderCreatedToastText(getString(R.string.create_folder_success));
            content.setFolderErrorToastText(getString(R.string.create_folder_error));

            builder = new StorageChooser.Builder()
                    .withActivity(getActivity())
                    .withMemoryBar(true)
                    .withFragmentManager(getFragmentManager())
                    .withPredefinedPath(Downloader.OPENGAPPS_PREDEFINED_PATH)
                    .withContent(content)
                    .actionSave(false)
                    .setType(StorageChooser.DIRECTORY_CHOOSER)
                    .allowCustomPath(true)
                    .allowAddFolder(true)
                    .showHidden(true);

            if (sharedPreferences.getBoolean("nightMode", false)) {
                StorageChooser.Theme theme = new StorageChooser.Theme(getActivity());
                theme.setScheme(getActivity()
                        .getResources()
                        .getIntArray(R.array.opengapps_theme));
                builder.setTheme(theme);
            }
        }
    }
}
