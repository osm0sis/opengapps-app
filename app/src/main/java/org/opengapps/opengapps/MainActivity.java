package org.opengapps.opengapps;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.opengapps.opengapps.DownloadProgress.DownloadProgressView;
import org.opengapps.opengapps.intro.AppIntroActivity;
import org.opengapps.opengapps.prefs.Preferences;

import eu.chainfire.libsuperuser.Shell;


public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, DownloadProgressView.DownloadStatusListener {
    private Downloader downloader;
    private SharedPreferences prefs;
    private InterstitialAd downloadAd;
    private FirebaseAnalytics analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        analytics = FirebaseAnalytics.getInstance(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        downloadAd = new InterstitialAd(this);
        downloadAd.setAdUnitId(getString(R.string.download_interstitial));
        requestAd();
        downloadAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestAd();
            }
        });
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                analytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, new Bundle());
                //  Initialize SharedPreferences
                SharedPreferences getPrefs = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);

                //  Create a new boolean and preference and set it to true
                boolean isFirstStart = getPrefs.getBoolean("firstStart", true);

                //  If the activity has never started before...
                if (isFirstStart) {

                    //  Launch app intro
                    Intent i = new Intent(MainActivity.this, AppIntroActivity.class);
                    startActivity(i);

                    //  Make a new preferences editor
                    SharedPreferences.Editor e = getPrefs.edit();

                    //  Edit preference to make it false because we don't want this to run again
                    //  Apply changes
                    e.apply();
                }

            }
        });

        // Start the thread
        t.start();

        prefs = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
        initButtons();
        initSelections();
    }

    private void requestAd() {
        AdRequest request = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("4AAD86A2F6F9FBC35B94E952288382AC")
                .addTestDevice("05814904E0308580F4ECF981062E5079")
                .build();
        downloadAd.loadAd(request);
    }

    private void restoreDownloadProgress() {
        Long id = prefs.getLong("running_download_id", 0);
        if (id != 0) {
            DownloadProgressView progress = (DownloadProgressView) findViewById(R.id.progressView);
            progress.show(id, this);
        }
    }

    private void initDownloader() {
        downloader = new Downloader(this);
        downloader.new TagUpdater().execute();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!prefs.getBoolean("firstStart", true)) {
            initDownloader();
            initPermissionCard();
        }
    }

    private void initPermissionCard() {
        CardView permssionCard = (CardView) findViewById(R.id.permission_card);
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED)
            permssionCard.setVisibility(View.GONE);
        else {
            permssionCard.setVisibility(View.VISIBLE);
            initPermissionButton();
        }
    }

    /**
     * Creates onClickListeners for all buttons
     */
    private void initButtons() {
        initDownloadButton();
        initInstallButton();
        initFab();
    }

    private void initFab() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.refresh_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloader.new TagUpdater();
            }
        });
    }

    private void initPermissionButton() {
        Button permissionButton = (Button) findViewById(R.id.grant_permission_button);
        final Activity mainActivity = this;
        permissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!prefs.getBoolean("firstStart", true))
            initPermissionCard();
    }

    private void initInstallButton() {
        Button install_button = (Button) findViewById(R.id.install_button);
        install_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ZipInstaller(getApplicationContext()).installZip();
            }
        });
    }

    /**
     * Create OnClickListner for DownloadButton
     */
    private void initDownloadButton() {
        Button downloadButton = (Button) findViewById(R.id.download_button);
        downloadButton.setText(getString(R.string.label_download));
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (downloadAd.isLoaded())
                    downloadAd.show();
                downloader.execute();
            }
        });
    }


    /**
     * Sets up all the spinners, fills them with entries and initializes the validation
     */
    private void initSelections() {
        TextView arch_selection = (TextView) findViewById(R.id.selected_architecture);
        TextView android_selection = (TextView) findViewById(R.id.selected_android);
        TextView variant_selection = (TextView) findViewById(R.id.selected_variant);
        TextView version = (TextView) findViewById(R.id.selected_version);


        arch_selection.setText(prefs.getString("selection_arch", "Err"));
        android_selection.setText(prefs.getString("selection_android", null));
        variant_selection.setText(prefs.getString("selection_variant", null));
        version.setText(prefs.getString("last_downloaded_tag", null));
    }

    /**
     * Is responsible for changing the UI when a new Version gets available
     *
     * @param updateAvailable true if a new Version is available
     */
    private void setNewVersionAvailable(boolean updateAvailable) {
        CardView card = (CardView) findViewById(R.id.cardView);
        TextView header = (TextView) findViewById(R.id.headline_download);
        Button downloadButton = (Button) findViewById(R.id.download_button);
        Button installButton = (Button) findViewById(R.id.install_button);

        card.setVisibility(View.VISIBLE);
        if (updateAvailable) {
            header.setText(getString(R.string.update_available));
            header.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
            downloadButton.setText(getString(R.string.label_update));
            downloadButton.setEnabled(true);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) installButton.getLayoutParams();
            params.setMarginStart(0);
            params.setMarginEnd(8);
            installButton.setLayoutParams(params);
            installButton.setVisibility(View.VISIBLE);
        } else {
            header.setText(getString(R.string.package_updated));
            header.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) installButton.getLayoutParams();
            params.setMarginStart(0);
            params.setMarginEnd(8);
            downloadButton.setVisibility(View.GONE);
            installButton.setLayoutParams(params);
            installButton.setVisibility(View.VISIBLE);
        }
        if (prefs.getString("last_downloaded_tag", "unset").equals("unset")) {
            header.setText(getString(R.string.label_download));
            header.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            downloadButton.setText(getString(R.string.label_download));
            downloadButton.setEnabled(true);
            downloadButton.setVisibility(View.VISIBLE);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) installButton.getLayoutParams();
            params.setMarginStart(0);
            params.setMarginEnd(0);
            installButton.setLayoutParams(params);
            installButton.setVisibility(View.GONE);
        }
        restoreDownloadProgress();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent t = new Intent(this, Preferences.class);
            startActivity(t);
            return true;
        } else if (id == R.id.about) {
            Intent t = new Intent(this, AboutActivity.class);
            startActivity(t);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Mostly handles special cases like change of GApps-Selection and firstRun-Behaviour
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        initSelections();
        if (!prefs.getBoolean("firstStart", true)) {
            if (s.equals("selection_android") || s.equals("selection_arch") || s.equals("selection_variant")) {
                clearAll();
            }
        }
        if (s.equals("firstStart")) {
            initDownloader();
            setNewVersionAvailable(false);
        }
    }

    /**
     * Clears all settings to give the user a fresh start
     */
    private void clearAll() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("last_downloaded_tag");
        editor.apply();
        downloader.deleteLastFile();
        downloader = new Downloader(this);
        downloader.new TagUpdater();
    }

    void OnTagUpdated() {
        if (prefs.getString("last_downloaded_tag", "").equals(downloader.getTag()))
            setNewVersionAvailable(false);
        else
            setNewVersionAvailable(true);
    }

    void downloadStarted(long id, String tag) {
        prefs.edit().putLong("running_download_id", id).apply();
        prefs.edit().putString("running_download_tag", tag).apply();
    }

    @Override
    public void downloadFailed(int reason) {
        initDownloadButton();
        downloader = new Downloader(this);
        prefs.edit().putLong("running_download_id", 0).apply();
        prefs.edit().putString("running_download_tag", null).apply();
    }

    @Override
    public void downloadSuccessful(String filePath) {
        initDownloadButton();
        Log.e("DL", "I AM SUCCESSFUL. ONCE");
        new FileValidator(this).execute(filePath);
    }

    @Override
    public void downloadCancelled() {
        initDownloadButton();
        downloader = new Downloader(this);
        downloader.new TagUpdater();
        prefs.edit().putLong("running_download_id", 0).apply();
        prefs.edit().putString("running_download_tag", null).apply();
    }

    void hashSuccess(Boolean match) {
        if (match) {
            String tag = prefs.getString("running_download_tag", "failed");
            if (!tag.equals("failed")) // dirty hack :(
                prefs.edit().putString("last_downloaded_tag", tag).apply();
            setNewVersionAvailable(false);
        } else {
            Toast.makeText(this, "CHECKSUM DOES NOT MATCH", Toast.LENGTH_LONG).show();
        }
        downloadCancelled();
    }
}
