package org.opengapps.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatDelegate;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;

import org.opengapps.app.prefs.Preferences;


public class OGappsApplication extends Application {
    public OGappsApplication() {
        super();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                if (BuildConfig.REPORT_CRASH) {
                    FirebaseCrash.report(throwable);
                }
            }
        });
    }

    @Override
    public void onCreate() {
        SharedPreferences preferences = getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        //DEBUGGING
        boolean a = preferences.getBoolean("nightMode", false);
        boolean b = preferences.getBoolean("nightMode", true);
        if (a && b || (!a && !b)) {
            Toast.makeText(this, "The preference for nightmode is " + a, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "The preference can't be read and the default is " + a, Toast.LENGTH_SHORT).show();
        }


        if (preferences.getBoolean("nightMode", false))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate();
    }
}
