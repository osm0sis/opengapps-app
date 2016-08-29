package org.opengapps.opengapps;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;

import org.opengapps.opengapps.intro.AppIntroActivity;
import org.opengapps.opengapps.prefs.Preferences;

public class NavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DownloadFragment downloadFragment;
    private Toolbar toolbar;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //  Initialize SharedPreferences
                SharedPreferences getPrefs = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);

                //  Create a new boolean and preference and set it to true
                boolean isFirstStart = getPrefs.getBoolean("firstStart", true);
                //  If the activity has never started before...
                if (isFirstStart) {
                    //Open Nav-Drawe
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    drawer.openDrawer(Gravity.LEFT, false);

                    //  Launch app intro
                    Intent i = new Intent(getApplicationContext(), AppIntroActivity.class);
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
        downloadFragment = new DownloadFragment();
        showFragment(downloadFragment);
        toolbar.setTitle(getString(R.string.label_download_install));
        navigationView.setCheckedItem(R.id.nav_download);

        if(AdBlockDetector.hasAdBlockEnabled(this))
            showAdBlockDialog();
    }

    private void showAdBlockDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.label_adblock_detected))
                .setMessage(R.string.explanation_adblock_detected)
                .setPositiveButton(R.string.label_donate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent x = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_donate)));
                        startActivity(x);
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        downloadFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
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
            toolbar.setTitle(getString(R.string.label_download_install));
        } else if (id == R.id.nav_settings) {
            Intent i = new Intent(this, Preferences.class);
            startActivity(i);
        } else if (id == R.id.nav_about) {
            Intent i = new Intent(this, AboutActivity.class);
            startActivity(i);
        } else if (id == R.id.nav_github){
            Intent i  = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)));
            startActivity(i);
        } else if (id == R.id.nav_blog){
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_blog)));
            startActivity(i);
        } else if (id == R.id.nav_donate){
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_donate)));
            startActivity(i);
        } else if(id == R.id.nav_opengapps){
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_opengapps)));
            startActivity(i);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    private void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.replaceme, fragment).commit();
        fragmentManager.executePendingTransactions();
    }
}
