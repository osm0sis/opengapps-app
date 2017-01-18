package org.opengapps.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatDelegate;

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
        if (preferences.getBoolean("nightMode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        super.onCreate();

        incrementRateCount();
    }

    private void incrementRateCount() {
        //increment rate
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        int count = prefs.getInt("rate_count", 0);
        boolean rate_status = prefs.getBoolean("rate_done",false);
        if(!rate_status) {
            editor.putInt("rate_count", count + 1).apply();
        }
    }
}
