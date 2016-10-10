package org.opengapps.app.card;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.opengapps.app.DownloadFragment;
import org.opengapps.app.R;
import org.opengapps.app.Stepper;
import org.opengapps.app.download.DownloadProgressView;
import org.opengapps.app.download.Downloader;
import org.opengapps.app.prefs.Preferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DownloadCard extends CardView {
    private SharedPreferences prefs;
    @SuppressWarnings("unused")
    private State state;
    private DownloadFragment fragment;
    private Context context;

    public DownloadCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        state = State.NORMAL;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.download_card, this, true);
        prefs = context.getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
    }

    public void init(DownloadFragment fragment) {
        this.fragment = fragment;
        if (!DownloadFragment.isRestored) {
            setState(State.DISABLED);
        }
        initButtons();
        initSelections();
        restoreDownloadProgress();
    }

    private void initButtons() {
        initDownloadButton();
        initCustomizeButton();
        initSelections();
    }

    private void initCustomizeButton() {
        Button customize = (Button) findViewById(R.id.change_button);
        customize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(context, Stepper.class);
                context.startActivity(i);
            }
        });
    }

    public void onTagUpdated(String lastAvailableTag) {
        String lastDownloadedTag = Downloader.getLastDownloadedTag(context);

        TextView version = (TextView) findViewById(R.id.newest_version);
        //noinspection deprecation,
        Spanned spanned = Html.fromHtml("<b>" + convertDate(lastAvailableTag) + "</b>");
        version.setText(spanned);

        if (lastDownloadedTag.equals(lastAvailableTag)) {
            setState(State.DISABLED);
        } else if (!TextUtils.isEmpty(lastDownloadedTag)) {
            setState(State.UPDATEABLE);
        } else {
            setState(State.NORMAL);
        }
    }

    public void initSelections() {
        TextView arch_selection = (TextView) findViewById(R.id.selected_architecture);
        TextView android_selection = (TextView) findViewById(R.id.selected_android);
        TextView variant_selection = (TextView) findViewById(R.id.selected_variant);


        arch_selection.setText(prefs.getString("selection_arch", null));
        android_selection.setText(prefs.getString("selection_android", null));
        variant_selection.setText(prefs.getString("selection_variant", null));
    }

    private void initDownloadButton() {
        Button downloadButton = (Button) findViewById(R.id.download_button);
        downloadButton.setText(getString(R.string.label_download));
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragment.showAd();
                fragment.getDownloader().execute();
            }
        });
    }

    public void restoreDownloadProgress() {
        Long id = prefs.getLong("running_download_id", 0);
        if (id != 0) {
            DownloadProgressView progress = (DownloadProgressView) findViewById(R.id.progress_view);
            progress.show(id, fragment);
        }
    }

    public void setState(State state) {
        this.state = state;
        TextView version = (TextView) findViewById(R.id.newest_version);
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
                    version.setText(Html.fromHtml("<b>" + version.getText() + "</b> <font color='red'><i color=\"#E53935\">(" + getString(R.string.label_new) + ")</i></font>"));
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

    private String getString(int id) {
        return getResources().getString(id);
    }

    public enum State {
        NORMAL, UPDATEABLE, DISABLED
    }
}
