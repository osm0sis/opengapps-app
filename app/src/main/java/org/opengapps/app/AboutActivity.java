package org.opengapps.app;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.plattysoft.leonids.ParticleSystem;
import com.plattysoft.leonids.modifiers.AlphaModifier;

import org.opengapps.app.prefs.Preferences;

import de.psdev.licensesdialog.LicensesDialog;

public class AboutActivity extends AppCompatActivity {
    private boolean playGAppsActive = false;
    private int snowTapping = 0;
    private ImageView logoLarge;

    private ParticleSystem psLeft;
    private ParticleSystem psRight;

    private SharedPreferences pref;

    private boolean[] eggsList = {false, false};

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        pref = getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initButtons();
        initLabels();
        updateEasterEggText();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("playGAppsActive", playGAppsActive);
        outState.putInt("snowTaps", snowTapping);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        playGAppsActive = savedInstanceState.getBoolean("playGAppsActive", false);
        snowTapping = savedInstanceState.getInt("snowTaps", 0);
        intitSecretButton();
    }

    private void initLabels() {
        //App-Version-Number
        TextView version = (TextView) findViewById(R.id.app_version);
        version.setText(BuildConfig.VERSION_NAME);

        //If Translators contain a comma, we use the respective plural version
        try {
            if (getString(R.string.translators).contains(",")) {
                ((TextView) findViewById(R.id.label_translators)).setText(R.string.label_translators);
            }
        } catch (Resources.NotFoundException ignored) {

        }
    }

    private void initButtons() {
        initMainDevButton();
        initCoDevButton();
        initLicenseButton();
        initTranslatorButton();
        intitSecretButton();
        initYetiButton();
        initCopyrightButton();
    }

    private void initTranslatorButton() {
        View translators = findViewById(R.id.translators);
        try {
            getString(R.string.translators);
        } catch (Resources.NotFoundException e) {
            translators.setVisibility(View.GONE);
        }
    }

    private void initMainDevButton() {
        LinearLayout mainDev = (LinearLayout) findViewById(R.id.main_dev_button);
        mainDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_main_dev)));
                startActivity(i);
            }
        });
    }

    private void initCoDevButton() {
        LinearLayout coDev = (LinearLayout) findViewById(R.id.co_dev_button);
        coDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_co_dev)));
                startActivity(i);
            }
        });
        coDev.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                snowTapping = snowTapping + 1;
                if (snowTapping > 4){
                    Toast.makeText(AboutActivity.this, "❄", Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
    }

    private void updateEasterEggText() {
        eggsList[0] = pref.getBoolean("eastereggFound", false);
        eggsList[1] = pref.getBoolean("snowFallingFound", false);
        TextView easterEggFound = (TextView) findViewById(R.id.found_indicator);
        easterEggFound.setText(String.valueOf(amountTrue(eggsList)) + "/" + String.valueOf(eggsList.length));
    }

     private int amountTrue(boolean[] list) {
        int count = 0;
        for(int i = 0; i < list.length; i++) {
            if(list[i]) {
             count++;
            }
        }
        return count;
     }


    private void initiateSnowFall() {
        psRight = new ParticleSystem(this, 80, R.drawable.snow, 10000)
                .setSpeedModuleAndAngleRange(0f, 0.15f, 180, 180)
                .setAcceleration(0.000010f, 90)
                .addModifier(new AlphaModifier(0, 255, 2000, 6000));

        psLeft = new ParticleSystem(this, 80, R.drawable.snow, 10000)
                .setSpeedModuleAndAngleRange(0f, 0.15f, 0, 0)
                .setAcceleration(0.000010f, 90)
                .addModifier(new AlphaModifier(0, 255, 2000, 6000));

        psLeft.emit(findViewById(R.id.emiter_top_left), 3);
        psRight.emit(findViewById(R.id.emiter_top_right), 3);
    }

    private void initCopyrightButton() {
        LinearLayout copyright = (LinearLayout) findViewById(R.id.copyright);
        copyright.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new ButtonDisabler(view), 3000);
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_producer)));
                startActivity(i);
            }
        });
    }

    private void initYetiButton() {
        LinearLayout yeti = (LinearLayout) findViewById(R.id.yeti_button);
        yeti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new ButtonDisabler(view), 3000);
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_artwork)));
                startActivity(i);
            }
        });
    }

    private void initLicenseButton() {
        LinearLayout license = (LinearLayout) findViewById(R.id.licenses);
        license.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new ButtonDisabler(view), 3000);
                Dialog dialog = new LicensesDialog.Builder(AboutActivity.this)
                        .setNotices(R.raw.notices)
                        .build().create();
                dialog.show();
                doKeepDialog(dialog);

            }
        });
    }

    private static void doKeepDialog(Dialog dialog) {
        try {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        } catch (Exception ignored) {
        }
    }

    private void intitSecretButton() {
        logoLarge = (ImageView) findViewById(R.id.logo_large);
        if (playGAppsActive) {
            logoLarge.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_playgapps_large));
        }

        logoLarge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(snowTapping > 4) {
                    pref.edit().putBoolean("snowFallingFound", true).apply();
                    updateEasterEggText();
                    initiateSnowFall();
                }
            }
        });

        logoLarge.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                pref.edit().putBoolean("eastereggFound", true).apply();
                updateEasterEggText();
                if (playGAppsActive) {
                    logoLarge.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_opengapps_large));
                } else {
                    logoLarge.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_playgapps_large));
                }
                playGAppsActive = !playGAppsActive;
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(psLeft !=null) {
            psLeft.cancel();
            psRight.cancel();
        }
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
