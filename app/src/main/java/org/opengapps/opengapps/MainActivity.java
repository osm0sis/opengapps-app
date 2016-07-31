package org.opengapps.opengapps;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import org.opengapps.opengapps.DownloadProgress.DownloadProgressView;
import org.opengapps.opengapps.intro.AppIntroActivity;
import org.opengapps.opengapps.prefs.Preferences;


public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, DownloadProgressView.DownloadStatusListener {
    private Downloader downloader;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //  Initialize SharedPreferences
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());

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


        prefs = getSharedPreferences(getResources().getString(R.string.pref_name), Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
        initButtons();
        initSelections();
    }

    private void restoreDownloadProgress() {
        Long id = prefs.getLong("running_download_id", 0);
        if(id !=0){
        }
    }

    private void initDownloader() {
        downloader = new Downloader(this);
        downloader.new TagUpdater().execute();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!prefs.getBoolean("firstStart", true))
            initDownloader();
        restoreDownloadProgress();
    }

    /**
     * Creates onClickListeners for all buttons
     */
    private void initButtons() {
        initDownloadButton();
        initInfoButton();
        initInstallButton();
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
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloader.execute();
            }
        });
    }

    /**
     * Sets OnClickListener for InfoButton and displays an Dialog
     */
    private void initInfoButton() {
        ImageButton variantInfoButton = (ImageButton) findViewById(R.id.variant_info_button);
        variantInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(view.getContext())
                        .setTitle(R.string.variants)
                        .setMessage(R.string.variants_explanation)
                        .setPositiveButton(R.string.ok, null)
                        .show();
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

    private void setNewVersionAvailable(boolean visible) {
        TextView header = (TextView) findViewById(R.id.headline_download);
        Button downloadButton = (Button) findViewById(R.id.download_button);
        if (visible) {
            header.setText("Update available");
            header.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
            downloadButton.setText("Update");
            downloadButton.setEnabled(true);
        } else {
            header.setText(getResources().getString(R.string.label_download));
            header.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            downloadButton.setText("Package UPDATED");
            downloadButton.setEnabled(false);
        }
        if (prefs.getString("last_downloaded_tag", "unset").equals("unset")) {
            header.setText(getResources().getString(R.string.label_download));
            header.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            downloadButton.setText(getResources().getString(R.string.label_download));
            downloadButton.setEnabled(true);
        }
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
        }

        return super.onOptionsItemSelected(item);
    }

    //Test!
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        initSelections();
        if (!prefs.getBoolean("firstStart", true))
            if (s.equals("selection_android") || s.equals("selection_arch") || s.equals("selection_variant")) {
                clearAll();
            }
        if (s.equals("firstStart"))
            initDownloader();
    }

    private void clearAll() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("last_downloaded_tag");
        editor.apply();
        downloader.deleteLastFile();
        downloader = new Downloader(this);
    }

    public void OnTagUpdated() {
        if (prefs.getString("last_downloaded_tag", "").equals(downloader.getTag()))
            setNewVersionAvailable(false);
        else
            setNewVersionAvailable(true);
    }

    public void downloadStarted(long id, String tag){
        prefs.edit().putLong("running_download_id", id).apply();
        prefs.edit().putString("running_download_tag", tag).apply();
    }

    @Override
    public void downloadFailed(int reason) {
        downloader = new Downloader(this);
        prefs.edit().putLong("running_download_id", 0).apply();
        prefs.edit().putString("running_download_tag", null).apply();
    }

    @Override
    public void downloadSuccessful(String filePath) {
        Log.e("DL", "I AM SUCCESSFUL. ONCE");
        new FileValidator(this).execute(filePath);
    }

    @Override
    public void downloadCancelled() {
        downloader = new Downloader(this);
        downloader.new TagUpdater();
        prefs.edit().putLong("running_download_id", 0).apply();
        prefs.edit().putString("running_download_tag", null).apply();
    }

    public void hashSuccess(Boolean match) {
        if(match){
            String tag = prefs.getString("running_download_tag", null);
            prefs.edit().putString("last_downloaded_tag", tag).apply();
            setNewVersionAvailable(false);
        } else{
            Toast.makeText(this, "CHECKSUM DOES NOT MATCH", Toast.LENGTH_LONG).show();
        }
    }

/*    @Override
    public void downloadFailed(int reason) {
        downloader = new Downloader(this);
        prefs.edit().putLong("running_download_id", 0).apply();
        prefs.edit().putString("running_download_tag", null).apply();
    }

    @Override
    public void downloadSuccessful() {
        String tag = prefs.getString("running_download_tag", null);
        prefs.edit().putString("last_downloaded_tag", tag).apply();
        downloadFailed(0);
        setNewVersionAvailable(false);
    }

    @Override
    public void downloadCancelled() {
        downloader = new Downloader(this);
        downloader.new TagUpdater();
        prefs.edit().putLong("running_download_id", 0).apply();
        prefs.edit().putString("running_download_tag", null).apply();
    }*/
}
