package org.opengapps.app.card;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.opengapps.app.DownloadFragment;
import org.opengapps.app.R;
import org.opengapps.app.Stepper;
import org.opengapps.app.download.DownloadProgressView;
import org.opengapps.app.download.Downloader;
import org.opengapps.app.intro.PackageGuesser;
import org.opengapps.app.prefs.Preferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class DownloadCard extends CardView {
    private SharedPreferences prefs;
    @SuppressWarnings("unused")
    private State state;
    private DownloadFragment fragment;
    private Context context;
    private FirebaseAnalytics analytics;

    //make customize button private and globally accessible [for hide/unhide]
    private Button customize;

    public DownloadCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        state = State.NORMAL;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.download_card, this, true);
        prefs = context.getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
    }

    /**
     * Initializes the DownloadCard, gets instance of Analytics and most importantly supplies the reference to DownloadFragment
     *
     * @param fragment The DownloadFragment holding the downloadCard
     */
    public void init(DownloadFragment fragment) {
        this.fragment = fragment;
        analytics = FirebaseAnalytics.getInstance(getContext());
        if (!DownloadFragment.isRestored) {
            setState(State.DISABLED);
        }
        initButtons();
        initSelections();
        restoreDownloadProgress();
    }

    /**
     * Container-Method for initializing text, state and onClickBehaviour of all buttons
     */
    private void initButtons() {
        initDownloadButton();
        initCustomizeButton();
        initSelections();
    }

    /**
     * Initializes "change-selection"-Buttton to start the Selection-Stepper on click
     */
    private void initCustomizeButton() {
        customize = (Button) findViewById(R.id.change_button);
        customize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(context, Stepper.class);
                fragment.startActivityForResult(i, 99);
            }
        });
    }

    /**
     * Checks if a download initiated by the app is currently running
     *
     * @return Returns true if a download that was started by the app is currently pending/running/waiting. Returns false otherwise
     */
    private boolean isDownloading() {
        return ((DownloadProgressView) findViewById(R.id.progress_view)).isDownloading();
    }

    /**
     * Receives updated tag from DownloadFragment via TagUpdater in Downloader. Then writes the latest version as formatted string (dependent on Users locale) in the corresponding textbox
     * and updates the state of the DownloadCard-View
     *
     * @param lastAvailableTag Latest Versionnumber of the OpenGApps package as string in the format "yyyyMMdd"
     */
    public void onTagUpdated(String lastAvailableTag) {
        String lastDownloadedTag = Downloader.getLastDownloadedTag(context);

        TextView version = (TextView) findViewById(R.id.text_rateus);
        //noinspection deprecation,
        Spanned spanned = Html.fromHtml("<b>" + convertDate(lastAvailableTag) + "</b>"); //Thanks to spanned, android-TextViews do support HTML codes
        version.setText(spanned);

        if (!isDownloading()) {
            if (lastDownloadedTag.equals(lastAvailableTag)) {
                setState(State.DISABLED);
            } else if (TextUtils.isEmpty(lastAvailableTag)) {
                setState(State.DISABLED);
                version.setText(convertDate(PackageGuesser.getCurrentlyInstalled()));
            } else if (!TextUtils.isEmpty(lastDownloadedTag)) {
                setState(State.UPDATEABLE);
            } else {
                setState(State.NORMAL);
            }
        }
    }

    /**
     * Initializes the textboxes for architecture, android-version and variant. Fetches the values from SharedPreferences
     */
    public void initSelections() {
        TextView arch_selection = (TextView) findViewById(R.id.selected_architecture);
        TextView android_selection = (TextView) findViewById(R.id.selected_android);
        TextView variant_selection = (TextView) findViewById(R.id.selected_variant);


        arch_selection.setText(prefs.getString("selection_arch", null));
        android_selection.setText(prefs.getString("selection_android", null));
        variant_selection.setText(prefs.getString("selection_variant", null));
    }

    /**
     * Initializes and resets the Download-Button. Also sets onClick-Behaviour.
     */
    private void initDownloadButton() {
        Button downloadButton = (Button) findViewById(R.id.download_button);
        downloadButton.setText(getString(R.string.label_download));
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean downloadWifiOnly = prefs.getBoolean("download_wifi_only", true);
                ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                boolean wifiConnected = networkInfo.isConnectedOrConnecting();
                if (downloadWifiOnly && !wifiConnected) {
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.pref_header_download)
                            .setMessage(R.string.explanation_wifi_needed)
                            .setPositiveButton(R.string.accept, null)
                            .show();
                } else {
                    fragment.showAd();
                    logSelections();
                    if (fragment.getDownloader() == null) {
                        Log.e(getClass().getSimpleName(), "onClick: Could not get downloader. Therefor cant download and downloadButton is useless");
                    }
                    fragment.getDownloader().execute();
                    // hide CHANGE SELECTION button
                    customize.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    /**
     * When downloading, the selection of the user as well as the latest GApps Version gets logged to Firebase Analytics. 2 Events are triggered
     * download: Puts selection and version as String in the Event. Only accessible via BigQuery (paid service)
     * download-int: Encodes selection and version as integer and puts it in the Event. Accessible via free firebase-Version, but has to be decoded
     */
    private void logSelections() {
        //Regular, string-based log. Only accssesible via BigQuery (expensive), but still logged in case we have the money later
        Bundle params = new Bundle(1);
        params.putString("selection_arch", prefs.getString("selection_arch", "null"));
        params.putString("selection_android", prefs.getString("selection_android", "null"));
        params.putString("selection_variant", prefs.getString("selection_variant", "null"));
        analytics.logEvent("download", params);


        //Calculates an integer out of the selection. allows us to analyze the downloads without paying for bigQuery by using integer-magic.
        //  packagesize (# 1 to 8, from small to big) (pico, micro, mini, ...)
        //  +
        //  sdk integer * 10
        //  +
        //  arch (#1 to 4, arm, arm64, x86, x86_64) * 1000
        //  +
        //  date * 10.000

        int identifier = 0;
        String[] variants = context.getResources().getStringArray(R.array.opengapps_variant);
        Collections.reverse(Arrays.asList(variants));
        for (int i = 0; i < variants.length; i++) {
            if (prefs.getString("selection_variant", "null").equalsIgnoreCase(variants[i]))
                identifier += i;
        }
        if (Build.VERSION.SDK_INT != 1000) //PreRelease-Versions of Android usually have SDK_INT=1000 which would corrupt our int-code. So in case of dev-device, SDK_INT==0 instead
            identifier += Build.VERSION.SDK_INT * 10;
        String[] architectures = context.getResources().getStringArray(R.array.architectures);
        for (int i = 0; i < architectures.length; i++) {
            if (prefs.getString("selection_arch", "null").equalsIgnoreCase(architectures[i]))
                identifier += i * 1000;
        }
        if (fragment.getDownloader() == null) {
            Log.d("DownloadCard", "logSelections: unable to get Downloader from downloadFragment. Aborted logging");
            return;
        }
        String tagString = fragment.getDownloader().getTag();
        if (!TextUtils.isEmpty(tagString)) {
            int tag = Integer.parseInt(tagString);
            identifier += tag * 10000;
        }
        Bundle int_params = new Bundle(1);
        int_params.putInt(FirebaseAnalytics.Param.VALUE, identifier);
        analytics.logEvent("download_int", int_params);
    }

    /**
     * Restores the download-Progress when the app was closed during download.
     * This works by saving the download_id as soon as the download is started.
     * This method loads the id and completes the necessary steps/shows the progress
     */
    public void restoreDownloadProgress() {
        int id;
        try {
            id = prefs.getInt("running_download_id", 0);
        } catch (ClassCastException e) {
            id = 0;
        }
        if (id != 0) {
            DownloadProgressView progress = (DownloadProgressView) findViewById(R.id.progress_view);
            progress.show(id, fragment);
        }
    }

    /**
     * Sets the "state" of the DownloadCard. Includes coloring and enabling/disabling of the downloadbutton and appending colored (new) to the version if necessary
     * If Storage-Permission is not granted, state is always disabled
     *
     * @param state State of the downloadcard. (NORMAL, UPDATEABLE or DISABLED).
     */
    public void setState(State state) {
        this.state = state;
        TextView version = (TextView) findViewById(R.id.text_rateus);
        Button downloadButton = (Button) findViewById(R.id.download_button);
        initDownloadButton();

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            state = State.DISABLED;
        }
        switch (state) {
            case NORMAL:
                downloadButton.setEnabled(true);
                downloadButton.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                break;
            case UPDATEABLE:
                if (!TextUtils.isEmpty(version.getText()))
                //noinspection deprecation
                {
                    //noinspection deprecation
                    Configuration configuration = getResources().getConfiguration();
                    if (configuration.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL)
                        version.setText(Html.fromHtml("<font color='red'><i>(" + getString(R.string.label_new) + ")</i></font>" + " <b>" + version.getText() + "</b>"));
                    else
                        version.setText(Html.fromHtml("<b>" + version.getText() + "</b> <font color='red'><i>(" + getString(R.string.label_new) + ")</i></font>"));
                }
                downloadButton.setEnabled(true);
                downloadButton.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                break;
            case DISABLED:
                downloadButton.setEnabled(false);
                downloadButton.setTextColor(Color.parseColor("#757575"));
                break;
        }
    }


    /**
     * Reads a date as String in the format "yyyyMMdd" and converts it to the corresponding local dateformat for easy reading
     *
     * @param tag Date as String in the format yyyyMMdd
     * @return Date as human-readable string dependent on the Locale of the user
     */
    private String convertDate(String tag) {
        if (tag == null || tag.equals("")) {
            return "";
        }
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date date;
        try {
            date = sdf.parse(tag);
        } catch (ParseException e) {
            return "";
        }
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        return dateFormat.format(date);
    }

    /**
     * Helpermethod to avoid using getResources for every little String
     *
     * @param id ID of the needed String
     * @return String
     */
    private String getString(int id) {
        return getResources().getString(id);
    }

    public enum State {
        NORMAL, UPDATEABLE, DISABLED
    }
}
