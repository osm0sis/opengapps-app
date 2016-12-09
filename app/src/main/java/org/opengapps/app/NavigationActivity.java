package org.opengapps.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.codekidlabs.storagechooser.utils.DiskUtil;

import org.opengapps.app.download.DownloadProgressView;
import org.opengapps.app.download.Downloader;
import org.opengapps.app.intro.AppIntroActivity;
import org.opengapps.app.prefs.Preferences;
import org.opengapps.app.utils.DialogUtil;

@SuppressWarnings("WrongConstant")
public class NavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public final static int EXIT_CODE = 1;
    public static boolean forcedUpdate = false;
    private DownloadFragment downloadFragment;
    private Toolbar toolbar;
    private NavigationView navigationView;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isRestored", true);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final boolean isFirstStart = getSharedPreferences(Preferences.prefName, MODE_PRIVATE).getBoolean("firstStart", true);
        if (!isFirstStart && AppUpdater.checkAllowed(getSharedPreferences(Preferences.prefName, MODE_PRIVATE))) {
            new AppUpdater().execute(this);
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                //  Create a new boolean and preference and set it to true
                //  If the activity has never started before...
                if (isFirstStart) {
                    //Open Nav-Drawer
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    drawer.openDrawer(Gravity.START, false);

                    //  Launch app intro
                    Intent i = new Intent(getApplicationContext(), AppIntroActivity.class);
                    startActivity(i);
                }

            }
        });

        // Start the thread
        t.start();
        downloadFragment = new DownloadFragment();
        if (savedInstanceState == null) {
            showFragment(downloadFragment);
        }
        toolbar.setTitle(getString(R.string.pref_header_install));
        navigationView.setCheckedItem(R.id.nav_download);

        if (AdBlockDetector.hasAdBlockEnabled(this)) {
            showAdBlockDialog();
        }
    }

    private void showAdBlockDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.label_adblock_detected))
                .setMessage(R.string.explanation_adblock_detected)
                .setPositiveButton(R.string.label_support_opengapps, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent x = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_support_opengapps)));
                        startActivity(x);
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EXIT_CODE && resultCode == 1) {
            finish();
        } else if (requestCode == EXIT_CODE && resultCode == 2) {
            if (downloadFragment != null && downloadFragment.isVisible()) {
                downloadFragment.initDownloader(false);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (downloadFragment.isAdded()) {
            downloadFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (forcedUpdate) {
            Toast.makeText(this, "You have to update in order to continue using the app", Toast.LENGTH_LONG).show();
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_download_update)));
            startActivity(i);
        }
        SharedPreferences prefs = getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(Gravity.START)) {
            drawer.closeDrawer(Gravity.START);
        }

        /*
        * = Downloader check routine =
        *
        * - Checks if startingDownload TextView is visible
        * - Not an ideal way but gives you enough time for the download thread to start
        *
        * */

        Downloader downloader = downloadFragment.getDownloader();
        if (downloader != null) {
            DownloadProgressView progressView = downloader.getProgressView();

            if (progressView != null) {
                if (progressView.startingDownload.getVisibility() == View.VISIBLE) {
//                    showToast("Download not started yet. Wait a sec.");
                    Log.d("NavigationActivity", "User trying to exit before thread created.");
                } else {
                    super.onBackPressed();
                }
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_download) {
            showFragment(downloadFragment);
            toolbar.setTitle(getString(R.string.pref_header_install));
        } else if (id == R.id.nav_settings) {
            startActivityAfterDrawerAnimation(Preferences.class);
        } else if (id == R.id.nav_support) {
            startActivityAfterDrawerAnimation(SupportActivity.class);
        } else if (id == R.id.nav_about) {
            startActivityAfterDrawerAnimation(AboutActivity.class);
        } else if (id == R.id.nav_blog) {
                openURL(this,getString(R.string.url_blog));
        } else if (id == R.id.nav_opengapps) {
                openURL(this,getString(R.string.url_opengapps));
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(Gravity.START);
        return true;
    }

    public static void openURL(Context context, String webUri) {
        PackageManager pm = context.getPackageManager();
        if(pm.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            context.startActivity(i);
        } else {
            DialogUtil.showAlertWithMessage(context,context.getString(R.string.title_webview_not_installed),
                    context.getString(R.string.message_webview_not_installed));
        }
    }


    private void showFragment(Fragment fragment) {
        String tag = null;
        if (fragment instanceof DownloadFragment) {
            tag = DownloadFragment.TAG;
        }
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.replaceme, fragment, tag).commit();
    }

    private void startActivityAfterDrawerAnimation(final Class className) {

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(NavigationActivity.this, className);
                startActivityForResult(i, 99);
            }
        }, 280);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if(s.equals("nightMode")){
            try {
                recreate();
            }catch (Exception e){
                //At this point, we dont really care if the recreate fails. It's rather a best effort thing that *might* work but doesnt need to
                Log.d(getClass().getSimpleName(), "onSharedPreferenceChanged: NightMode-Setting was triggered, but for some reason could not apply it");
            }
        }
    }
}
