package org.opengapps.opengapps.DownloadProgress;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.opengapps.opengapps.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Ayoola Ajebeku on 6/29/15.
 * Modified by Christoph Loy
 */
public class DownloadProgressView extends LinearLayout {

    private final ProgressBar downloadProgressBar;
    private final TextView downloadedSizeView, totalSizeView, percentageView;
    private final DownloadManager downloadManager;
    private final Context context;
    private int downloadedSizeColor, totalSizeColor, percentageColor;
    private long downloadID;
    private boolean downloading;
    private List<DownloadStatusListener> listeners = new ArrayList<>();

    public DownloadProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

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

        downloadedSizeView = (TextView) findViewById(R.id.downloadedSize);
        totalSizeView = (TextView) findViewById(R.id.totalSize);
        percentageView = (TextView) findViewById(R.id.percentage);
        downloadProgressBar = (ProgressBar) findViewById(R.id.downloadProgressBar);

        downloadedSizeView.setTextColor(ColorStateList.valueOf(percentageColor));
        totalSizeView.setTextColor(ColorStateList.valueOf(percentageColor));
        percentageView.setTextColor(ColorStateList.valueOf(percentageColor));

        //hides view.
        setVisibility(View.GONE);
    }

    /**
     * This method sets the color of the downloadedSize TextView.
     *
     * @param downloadedSizeColor the color of the downloadedSize TextView.
     */
    public void setDownloadedSizeColor(int downloadedSizeColor) {
        this.downloadedSizeColor = downloadedSizeColor;
        downloadedSizeView.setTextColor(ColorStateList.valueOf(percentageColor));
        invalidate();
        requestLayout();
    }

    /**
     * This method sets the color of the totalSize TextView.
     *
     * @param totalSizeColor the color of the totalSize TextView.
     */
    public void setTotalSizeColor(int totalSizeColor) {
        this.totalSizeColor = totalSizeColor;
        totalSizeView.setTextColor(ColorStateList.valueOf(percentageColor));
        invalidate();
        requestLayout();
    }

    /**
     * This method sets the color of the percentage TextView.
     *
     * @param percentageColor the color of the percentage TextView.
     */
    public void setPercentageColor(int percentageColor) {
        this.percentageColor = percentageColor;
        percentageView.setTextColor(ColorStateList.valueOf(percentageColor));
        invalidate();
        requestLayout();
    }

    /**
     * This method returns the color of the downloadedSize TextView.
     *
     * @return the color of the downloadedSize TextView.
     */
    public int getDownloadedSizeColor() {
        return downloadedSizeColor;
    }

    /**
     * This method returns the color of the totalSize TextView.
     *
     * @return the color of the totalSize TextView.
     */
    public int getTotalSizeColor() {
        return totalSizeColor;
    }

    /**
     * This method returns the color of the percentage TextView.
     *
     * @return the color of the percentage TextView.
     */
    public int getPercentageColor() {
        return percentageColor;
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
        listeners.add(downloadStatusListener);
        showDownloadProgress();
    }

    private void showDownloadProgress() {
        setVisibility(View.VISIBLE);
        View test = (View) getParent();
        Button downloadButton = (Button) test.findViewById(R.id.download_button);
        downloadButton.setText(getResources().getString(R.string.label_cancel));
        downloadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (downloadManager != null) {
                    downloadManager.remove(downloadID);
                    try {
                        for (DownloadStatusListener downloadStatusListener : listeners) {
                            downloadStatusListener.downloadCancelled();
                        }
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
                    c = downloadManager.query(query);
                    if (c.moveToFirst()) {
                        final int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));//Get download status
                        final int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));//Get download status
                        final long bytes_downloaded = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        final long bytes_total = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        final String filePath = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        final long download_percentage = (bytes_downloaded * 100L) / bytes_total;

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void run() {
                                if (status == DownloadManager.STATUS_RUNNING) {
                                    downloading = true;
                                    downloadProgressBar.setIndeterminate(false);
                                    downloadedSizeView.setText(String.format(Locale.US, "%.0fMB", ((bytes_downloaded * 1.0) / 1024L / 1024L)));
                                    totalSizeView.setText(String.format(Locale.US, "%.0fMB", ((bytes_total * 1.0) / 1024L / 1024L)));
                                    percentageView.setText((int) download_percentage + "%");
                                    downloadProgressBar.setProgress((int) download_percentage);
                                } else if (status == DownloadManager.STATUS_FAILED) {
                                    downloading = false;
                                    setVisibility(View.GONE);

                                    try {
                                        for (DownloadStatusListener downloadStatusListener : listeners) {
                                            downloadStatusListener.downloadFailed(reason);
                                        }
                                    } catch (Exception ignored) {
                                    }
                                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    downloading = false;
                                    setVisibility(View.GONE);

                                    for (DownloadStatusListener downloadStatusListener : listeners) {
                                        downloadStatusListener.downloadSuccessful(filePath.substring(7));
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
                                    for (DownloadStatusListener downloadStatusListener : listeners) {
                                        downloadStatusListener.downloadFailed(-1);
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        });
                    }
                    c.close();
                } while (downloading);
            }
        }.start();
    }

    public interface DownloadStatusListener {
        void downloadFailed(int reason);

        void downloadSuccessful(String filePath);

        void downloadCancelled();

    }
}