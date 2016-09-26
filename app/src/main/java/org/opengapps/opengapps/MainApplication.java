package org.opengapps.opengapps;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by beatbrot on 26.09.2016.
 */

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this))
            return;
        LeakCanary.install(this);
    }
}
