package org.opengapps.opengapps;

import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class AboutActivity extends AppCompatActivity {
    private boolean playGAppsActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initButtons();
    }

    private void initButtons() {
        intitSecretButton();
    }

    private void intitSecretButton() {
        final ImageView logoLarge = (ImageView) findViewById(R.id.logo_large);
        logoLarge.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (playGAppsActive)
                    logoLarge.setImageDrawable(getResources().getDrawable(R.drawable.opengapps_large));
                else
                    logoLarge.setImageDrawable(getResources().getDrawable(R.drawable.playgapps_large));
                playGAppsActive = !playGAppsActive;
                return false;
            }
        });
    }

}
