package org.opengapps.app.download;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.opengapps.app.R;
import org.opengapps.app.prefs.Preferences;
import org.opengapps.app.utils.DialogUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by Ayoola Ajebeku on 6/29/15.
 * Modified by Christoph Loy
 */
public class DownloadProgressView extends LinearLayout {

    private final ProgressBar downloadProgressBar;
    private final TextView downloadedSizeView, totalSizeView, percentageView, backslash;
    public final TextView startingDownload;
    private final DownloadManager downloadManager;
    private int downloadedSizeColor, totalSizeColor, percentageColor;
    private long downloadID;
    private boolean downloading;
    private DownloadStatusListener listener;

    private Button customize;
    private Button downloadButton;

    private SharedPreferences sharedPreferences;

    public DownloadProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DownloadProgressView, 0, 0);

        try {
            downloadedSizeColor = typedArray.getColor(R.styleable.DownloadProgressView_downloadedSizeColor, Color.BLACK);
            totalSizeColor = typedArray.getColor(R.styleable.DownloadProgressView_totalSizeColor, Color.BLACK);
            percentageColor = typedArray.getColor(R.styleable.DownloadProgressView_totalSizeColor, Color.BLACK);
        } finally {
            typedArray.recycle();
        }

        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER);

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.download_progress_view, this, true);

        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        downloadedSizeView = (TextView) findViewById(R.id.downloaded_size);
        totalSizeView = (TextView) findViewById(R.id.total_size);
        percentageView = (TextView) findViewById(R.id.percentage);
        downloadProgressBar = (ProgressBar) findViewById(R.id.download_progress_bar);
        startingDownload = (TextView) findViewById(R.id.download_starting);
        backslash = (TextView) findViewById(R.id.backslash);

        downloadedSizeView.setTextColor(ColorStateList.valueOf(percentageColor));
        totalSizeView.setTextColor(ColorStateList.valueOf(percentageColor));
        percentageView.setTextColor(ColorStateList.valueOf(percentageColor));

        //hides view.
        setVisibility(View.GONE);

        //init preference
        sharedPreferences = context.getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
    }

    /**
     * This method initialize and shows the downloadProgressView.
     *
     * @param downloadID             the downloadID gotten when a download was enqueued.
     * @param downloadStatusListener the downloadStatusListener to monitor when
     *                               download is successful, failed, cancelled.
     */
    public void show(long downloadID, DownloadStatusListener downloadStatusListener) {
        this.downloadID = downloadID;
        backslash.setText("/");
        startingDownload.setVisibility(GONE);
        listener = downloadStatusListener;
        showDownloadProgress();
    }

    public void begin() {
        View parentView = (View) getParent();
        downloadButton = (Button) parentView.findViewById(R.id.download_button);
        customize = (Button) parentView.findViewById(R.id.change_button);
        setVisibility(VISIBLE);
        startingDownload.setVisibility(VISIBLE);

        downloadProgressBar.setIndeterminate(true);
        downloadProgressBar.setProgress(0);
        downloadedSizeView.setText("");
        backslash.setText("");
        percentageView.setText("");
        totalSizeView.setText("");
        downloadButton.setText(getResources().getString(R.string.label_cancel));
        downloadButton.setEnabled(false);
    }

    public boolean isDownloading() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadID);
        Cursor c = downloadManager.query(query);
        if (c != null && c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_PAUSED || status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING) {
                return true;
            }
        }
        return false;
    }

    private void showDownloadProgress() {
        setVisibility(View.VISIBLE);
        View parentView = (View) getParent();
        final Button downloadButton = (Button) parentView.findViewById(R.id.download_button);
        downloadButton.setText(getResources().getString(R.string.label_cancel));
        downloadButton.setEnabled(true);
        downloadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);
                logCancel(getContext());
                if (downloadManager != null) {
                    downloadManager.remove(downloadID);
                    try {
                        listener.downloadCancelled();
                        customize.setVisibility(View.VISIBLE);
                        onDownloadInterruptedView();
                    } catch (Exception ignored) {
                    }
                }
                setVisibility(View.GONE);
            }
        });
        new Thread() {
            @Override
            public void run() {
                do {
                    downloading = true;

                    final Cursor c;
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadID);
                    //We are actually not sure if all of this fixes a bug where downloadManager would work just fine. We will just leave that in the code now to test out if the bug will reoccur or not
                    DownloadManager dlMan;
                    if (downloadManager == null)
                        dlMan = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    else
                        dlMan = downloadManager;
                    c = dlMan.query(query);
                    if (c != null && c.moveToFirst()) {
                        final int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));//Get download status
                        final int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));//Get download status
                        final long bytes_downloaded = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        final long bytes_total = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        final String filePath = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        final long download_percentage;
                        if (bytes_total != 0)
                            download_percentage = (bytes_downloaded * 100L) / bytes_total;
                        else
                            download_percentage = 0;

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void run() {
                                if (status == DownloadManager.STATUS_RUNNING) {
                                    downloading = true;
                                    downloadProgressBar.setIndeterminate(false);
                                    downloadedSizeView.setText(String.format(Locale.US, "%.0fMB", ((bytes_downloaded * 1.0) / 1024L / 1024L)));
                                    if (bytes_total != -1) {
                                        totalSizeView.setText(String.format(Locale.US, "%.0fMB", ((bytes_total * 1.0) / 1024L / 1024L)));
                                    } else {
                                        totalSizeView.setText("??");
                                    }
                                    percentageView.setText((int) download_percentage + "%");
                                    downloadProgressBar.setProgress((int) download_percentage);
                                } else if (status == DownloadManager.STATUS_FAILED) {
                                    downloading = false;
                                    setVisibility(View.GONE);
                                    //show 'CHANGE SELECTION' button after download failed too
                                    if (customize != null)
                                        customize.setVisibility(View.VISIBLE);
                                    onDownloadInterruptedView();

                                    try {
                                        listener.downloadFailed(reason);
                                    } catch (Exception ignored) {
                                    }
                                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    synchronized (Looper.getMainLooper()) {
                                        downloading = false;
                                        setVisibility(View.GONE);
                                        listener.downloadSuccessful(filePath.substring("file://".length()));

                                        //show 'CHANGE SELECTION' button after download successful too
                                        if (customize != null)
                                            customize.setVisibility(View.VISIBLE);
                                        //since latest version is downloaded successfully the onDownloadInterruptedView() flow
                                        //can be applicable here too
                                        onDownloadInterruptedView();

                                        //rate us dialog
                                        int count = sharedPreferences.getInt("rate_count", 0);
                                        boolean rate_status = sharedPreferences.getBoolean("rate_done",false);
                                        if(count == 10 && !rate_status) {
                                            DialogUtil.showRatingDialog(getContext()).show();
                                        } else {
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            count += 1;
                                            editor.putInt("rate_count", count);
                                            editor.apply();
                                        }
                                    }
                                } else {
                                    downloading = true;
                                    downloadedSizeView.setText("");
                                    totalSizeView.setText("");
                                    percentageView.setText("");
                                    downloadProgressBar.setIndeterminate(true);
                                }
                            }
                        });
                    } else {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                downloading = false;
                                setVisibility(View.GONE);

                                try {
                                    listener.downloadFailed(-1);
                                } catch (Exception ignored) {
                                }
                            }
                        });
                    }
                    if (c != null) {
                        c.close();
                    } else {
                        Log.d(getClass().getSimpleName(), "run: Cursor is already null. Therefore cant close it.");
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (downloading);
            }
        }.start();
    }

    private void logCancel(Context context) {
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
        SharedPreferences prefs = context.getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);

        //Regular, string-based log. Only accssesible via BigQuery (expensive), but still logged in case we have the money later
        Bundle params = new Bundle(1);
        params.putString("selection_arch", prefs.getString("selection_arch", "null"));
        params.putString("selection_android", prefs.getString("selection_android", "null"));
        params.putString("selection_variant", prefs.getString("selection_variant", "null"));
        analytics.logEvent("cancel", params);


        //Calculates an integer out of the selection. allows us to analyze the downloads without paying for bigQuery by using integer-magic.
        //  packagesize (# 1 to 8, from small to big) (pico, micro, mini, ...)
        //  +
        //  sdk integer * 10
        //  +
        //  arch (#1 to 4, arm, arm64, x86, x86_64) * 1000

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
        Bundle int_params = new Bundle(1);
        int_params.putInt(FirebaseAnalytics.Param.VALUE, identifier);
        analytics.logEvent("cancel_int", int_params);
    }

    private void onDownloadInterruptedView() {
        if (downloadButton != null) {
            downloadButton.setText(R.string.label_download);
            downloadButton.setEnabled(false);
            downloadButton.setTextColor(Color.parseColor("#757575"));
        }
    }

    public interface DownloadStatusListener {
        void downloadFailed(int reason);

        void downloadSuccessful(String filePath);

        void downloadCancelled();

    }
}
