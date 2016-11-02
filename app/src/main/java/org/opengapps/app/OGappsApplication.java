package org.opengapps.app;

import android.app.Application;

import com.google.firebase.crash.FirebaseCrash;


public class OGappsApplication extends Application {
    public OGappsApplication() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                if (BuildConfig.REPORT_CRASH) {
                    FirebaseCrash.report(throwable);
                }
            }
        });
    }
}
