package org.opengapps.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.opengapps.app.intro.AppIntroActivity;
import org.opengapps.app.prefs.Preferences;

public class SplashActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SharedPreferences pref = getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        Handler mHandler = new Handler();

        int SPLASH_DELAY = 2000;
        String SPLASH_KEY = "firstStart";
        if(!pref.contains(SPLASH_KEY) || pref.getBoolean(SPLASH_KEY, true)) {
            mHandler.postDelayed(new DecideFlow(0), SPLASH_DELAY);
        } else {
            mHandler.postDelayed(new DecideFlow(1), SPLASH_DELAY);
        }
    }


    private class DecideFlow implements Runnable {

        int data;

        private DecideFlow(int data) {
            this.data = data;
        }

        @Override
        public void run() {
            if(data == 0) {
                startFlow(AppIntroActivity.class);
            } else {
                startFlow(NavigationActivity.class);
            }

        }
    }

    private void startFlow(Class activityClass) {
        Intent i = new Intent(this, activityClass);
        startActivity(i);
        this.finish();
    }
}
