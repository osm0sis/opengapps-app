package org.opengapps.opengapps.card;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.opengapps.opengapps.DownloadFragment;
import org.opengapps.opengapps.R;
import org.opengapps.opengapps.Stepper;
import org.opengapps.opengapps.download.DownloadProgressView;
import org.opengapps.opengapps.download.Downloader;
import org.opengapps.opengapps.prefs.Preferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DownloadCard extends CardView {
    private SharedPreferences prefs;
    private DownloadFragment fragment;

    public DownloadCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.download_card, this, true);
        prefs = getContext().getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
    }

    public void init(DownloadFragment fragment) {
        this.fragment = fragment;
        if (!DownloadFragment.isRestored)
            setState(DownloadCardState.DISABLED);
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
                Intent i = new Intent(getContext(), Stepper.class);
                getContext().startActivity(i);
            }
        });
    }

    public void onTagUpdated(String lastAvailableTag) {
        String lastDownloadedTag = Downloader.getLastDownloadedTag(getContext());

        TextView version = (TextView) findViewById(R.id.newest_version);
        version.setText(convertDate(lastAvailableTag));

        if (lastDownloadedTag.equals(lastAvailableTag))
            setState(DownloadCardState.DISABLED);
        else if (!TextUtils.isEmpty(lastDownloadedTag))
            setState(DownloadCardState.UPDATEABLE);
        else
            setState(DownloadCardState.NORMAL);
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

    public void setState(DownloadCardState state) {
        TextView header = (TextView) findViewById(R.id.headline_download);
        Button downloadButton = (Button) findViewById(R.id.download_button);

        switch (state) {
            case NORMAL:
                header.setText(getString(R.string.label_download_package));
                header.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                downloadButton.setText(getString(R.string.label_download));
                downloadButton.setEnabled(true);
                downloadButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                break;
            case UPDATEABLE:
                header.setText(getString(R.string.update_available));
                header.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                downloadButton.setText(getString(R.string.label_update));
                downloadButton.setEnabled(true);
                downloadButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                break;
            case DISABLED:
                header.setText(getString(R.string.label_download_package));
                header.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                downloadButton.setEnabled(false);
                downloadButton.setTextColor(Color.parseColor("#757575"));
                downloadButton.setText(getString(R.string.label_download));
                break;
        }
    }


    private String convertDate(String tag) {
        if (tag == null || tag.equals(""))
            return "";
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date date;
        try {
            date = sdf.parse(tag);
        } catch (ParseException e) {
            return "";
        }
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
        return dateFormat.format(date);
    }

    private String getString(int id) {
        return getResources().getString(id);
    }

    public enum DownloadCardState {
        NORMAL, UPDATEABLE, DISABLED
    }
}