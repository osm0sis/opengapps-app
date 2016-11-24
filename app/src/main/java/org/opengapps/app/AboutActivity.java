package org.opengapps.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import org.opengapps.app.prefs.Preferences;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.psdev.licensesdialog.LicensesDialog;

public class AboutActivity extends AppCompatActivity {
    private boolean playGAppsActive = false;
    private int gfCount = 0;
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
        initMainDevButton();
        initCoDevButton();
        initLicenseButton();
        initTranslatorButton();
        intitSecretButton();
        initMausiButton();
        initYetiButton();
        initCopyrightButton();
        initEasterEggFoundButton();
    }

    private void initTranslatorButton() {
        View translators = findViewById(R.id.translators);
        try {
            getString(R.string.translators);
        } catch (Resources.NotFoundException e) {
            translators.setVisibility(View.GONE);
        }
    }

    private void initEasterEggFoundButton() {
        boolean found = getSharedPreferences(Preferences.prefName, MODE_PRIVATE).getBoolean("eastereggFound", false);
        if (found) {
            TextView easterEggFound = (TextView) findViewById(R.id.found_indicator);
            easterEggFound.setText(R.string.label_yes);
        }
    }

    private void initMainDevButton() {
        LinearLayout mainDev = (LinearLayout) findViewById(R.id.main_dev_button);
        mainDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText edit = new EditText(AboutActivity.this);
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String input = edit.getText().toString();

                        if (input.trim().toUpperCase().startsWith("MI")) {
                            Pattern p = Pattern.compile("(mii*\\s){2}(mii*)", Pattern.CASE_INSENSITIVE);
                            Matcher matcher = p.matcher(input);
                            if (matcher.matches()) {
                                Toast.makeText(AboutActivity.this, "Meh❤", Toast.LENGTH_LONG).show();
                            }
                        }
                        if (input.trim().toLowerCase().startsWith("ha")) {
                            Pattern p = Pattern.compile("Haa*sii*", Pattern.CASE_INSENSITIVE);
                            Matcher matcher = p.matcher(input);
                            if (matcher.matches()) {
                                Toast.makeText(AboutActivity.this, "Maauusiiii❤", Toast.LENGTH_LONG).show();
                            }
                        }
                        if (input.contains("Chris") && input.contains("Cross")) {
                            Toast.makeText(AboutActivity.this, "Mein Engel❤︎", Toast.LENGTH_LONG).show();
                        }
                    }
                };
                if (gfCount == 5) {
                    edit.setHint("Enter secret word");
                    new AlertDialog.Builder(AboutActivity.this)
                            .setTitle("This is only meant for my loved one, glad you found it")
                            .setView(edit)
                            .setPositiveButton(android.R.string.ok, listener)
                            .show();
                } else if (gfCount >= 1) {
                    gfCount++;
                } else {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_main_dev)));
                    startActivity(i);
                }
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
    }

    private void initMausiButton() {
        LinearLayout mainDev = (LinearLayout) findViewById(R.id.main_dev_button);
        mainDev.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                gfCount = 1;
                return true;
            }
        });
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
                new LicensesDialog.Builder(AboutActivity.this)
                        .setNotices(R.raw.notices)
                        .build()
                        .show();
            }
        });
    }

    private void intitSecretButton() {
        final ImageView logoLarge = (ImageView) findViewById(R.id.logo_large);
        final SharedPreferences prefs = getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        logoLarge.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                prefs.edit().putBoolean("eastereggFound", true).apply();
                initEasterEggFoundButton();
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
