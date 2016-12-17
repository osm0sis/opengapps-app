package org.opengapps.app;

import android.app.Application;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
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
        if (preferences.getBoolean("nightMode", false)) {
            if(Build.VERSION.SDK_INT>=23) {
                UiModeManager uiModeManager = (android.app.UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
                uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        }
        super.onCreate();
    }
}
