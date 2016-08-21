package org.opengapps.opengapps;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.psdev.licensesdialog.LicensesDialog;

public class AboutActivity extends AppCompatActivity {
    private boolean playGAppsActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initButtons();
        initLabels();
    }

    private void initLabels() {
        TextView version = (TextView) findViewById(R.id.app_version);
        version.setText(BuildConfig.VERSION_NAME);
    }

    private void initButtons() {
        initLicenseButton();
        intitSecretButton();
        initYetiButton();
        initCopyrightButton();
    }

    private void initCopyrightButton() {
        LinearLayout copyright = (LinearLayout) findViewById(R.id.copyright);
        final AppCompatActivity ac = this;
        copyright.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(ac)
                        .setTitle(R.string.label_copyright)
                        .setMessage(R.string.app_copyright)
                        .show();
            }
        });
    }

    private void initYetiButton() {
        LinearLayout yeti = (LinearLayout) findViewById(R.id.yeti_button);
        yeti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new ButtonDisabler(view), 3000);
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_yeti)));
                startActivity(i);
            }
        });
    }

    private void initLicenseButton() {
        LinearLayout license = (LinearLayout) findViewById(R.id.licenses);
        final Activity thisAc = this;
        license.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new ButtonDisabler(view), 3000);
                new LicensesDialog.Builder(thisAc)
                        .setNotices(R.raw.notices)
                        .build()
                        .show();
            }
        });
    }

    private void intitSecretButton() {
        final ImageView logoLarge = (ImageView) findViewById(R.id.logo_large);
        logoLarge.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (playGAppsActive)
                    logoLarge.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.opengapps_large));
                else
                    logoLarge.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.playgapps_large));
                playGAppsActive = !playGAppsActive;
                return false;
            }
        });
    }

    private static class ButtonDisabler implements Runnable {
        private View view;

        private ButtonDisabler(View v) {
            view = v;
            v.setEnabled(false);
        }

        @Override
        public void run() {
            view.setEnabled(true);
        }
    }
}
