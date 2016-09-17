package org.opengapps.opengapps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.opengapps.opengapps.card.InstallCard;
import org.opengapps.opengapps.download.DownloadProgressView;
import org.opengapps.opengapps.download.Downloader;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;


@SuppressWarnings("ConstantConditions")
public class DownloadFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, DownloadProgressView.DownloadStatusListener, SwipeRefreshLayout.OnRefreshListener {
    private Downloader downloader;
    private SharedPreferences prefs;
    private InterstitialAd downloadAd;
    private SwipeRefreshLayout refreshLayout;
    private boolean downloaderLoaded = false;
    private static HashMap<String, InstallCard> fileCards = new HashMap<>();
    private static boolean isRestored = false;
    private static String lastTag = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getContext().getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (downloader == null) {
            initDownloader(isRestored);
        }
        if (!downloader.fileExists() && prefs.getLong("running_download_id", 0) == 0) {
            onDeleteFile();
        }
        isRestored = true;
    }


    public void onDeleteFile() {
        onDeleteFile(null);
    }

    public void onDeleteFile(@Nullable File gappsFile) {
        if (gappsFile == null)
            loadInstallCards();
        else {
            InstallCard card = fileCards.get(gappsFile.getAbsolutePath());
            if (card != null) {
                LinearLayout layout = (LinearLayout) getView().findViewById(R.id.main_layout);
                layout.removeView(card);
            }
            fileCards.remove(gappsFile.getAbsolutePath());
        }
        prefs.edit().putString("last_downloaded_tag", Downloader.getLastDownloadedTag(getContext())).apply();
        initDownloader(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!prefs.getBoolean("firstStart", true) && !downloaderLoaded) {
            initDownloader(isRestored);
            downloaderLoaded = true;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.content_main, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.dl_refresher);
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(), R.color.colorPrimary));

        refreshLayout.setOnRefreshListener(this);
        FirebaseAnalytics.getInstance(getContext());
        downloadAd = new InterstitialAd(getContext());
        downloadAd.setAdUnitId(getString(R.string.download_interstitial));
        requestAd();
        downloadAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestAd();
            }
        });

        prefs = getContext().getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
        initButtons();
        initSelections();

        if (!prefs.getBoolean("firstStart", true))
            initPermissionCard();
        loadInstallCards();
    }

    private void requestAd() {
        AdRequest request;
        if (BuildConfig.DEBUG)
            request = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice("F98ACBE481522BAE0A91AC208FDF938F")
                    .addTestDevice("CAAA7C86D5955208EF75484D93E09948")
                    .build();
        else
            request = new AdRequest.Builder()
                    .build();
        downloadAd.loadAd(request);
    }

    private void restoreDownloadProgress() {
        Long id = prefs.getLong("running_download_id", 0);
        if (id != 0) {
            DownloadProgressView progress = (DownloadProgressView) getView().findViewById(R.id.progress_view);
            progress.show(id, this);
        }
    }

    private void initDownloader(boolean isRestored) {
        downloader = new Downloader(this);
        if (!isRestored) {
            downloader.new TagUpdater().execute();
            refreshLayout.setRefreshing(true);
        } else {
            downloader.setTag(lastTag);
            OnTagUpdated();
        }
    }

    private void initPermissionCard() {
        CardView permissionCard = (CardView) getView().findViewById(R.id.permission_card);
        int permissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED)
            permissionCard.setVisibility(View.GONE);
        else {
            permissionCard.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Creates onClickListeners for all buttons
     */
    private void initButtons() {
        initDownloadButton();
        initCustomizeButton();
    }

    private void initCustomizeButton() {
        Button customize = (Button) getView().findViewById(R.id.change_button);
        customize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), Stepper.class);
                startActivity(i);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        SharedPreferences preferences = getContext().getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        if (!preferences.getBoolean("firstStart", true)) {
            initPermissionCard();
            loadInstallCards();
        }
    }


    /**
     * Create OnClickListner for DownloadButton
     */
    private void initDownloadButton() {
        Button downloadButton = (Button) getView().findViewById(R.id.download_button);
        downloadButton.setText(getString(R.string.label_download));
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (downloadAd.isLoaded())
                    downloadAd.show();
                downloader.execute();
            }
        });
    }



    /**
     * Sets up all the spinners, fills them with entries and initializes the validation
     */
    private void initSelections() {
        if (isAdded()) {
            TextView arch_selection = (TextView) getView().findViewById(R.id.selected_architecture);
            TextView android_selection = (TextView) getView().findViewById(R.id.selected_android);
            TextView variant_selection = (TextView) getView().findViewById(R.id.selected_variant);


            arch_selection.setText(prefs.getString("selection_arch", null));
            android_selection.setText(prefs.getString("selection_android", null));
            variant_selection.setText(prefs.getString("selection_variant", null));
        }
    }

    /**
     * Is responsible for changing the UI when a new Version gets available
     *
     * @param updateAvailable true if a new Version is available
     */
    private void setNewVersionAvailable(boolean updateAvailable) {
        CardView card = (CardView) getView().findViewById(R.id.download_card);
        TextView header = (TextView) getView().findViewById(R.id.headline_download);
        Button downloadButton = (Button) getView().findViewById(R.id.download_button);

        card.setVisibility(View.VISIBLE);
        if (updateAvailable) {
            header.setText(getString(R.string.update_available));
            header.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            downloadButton.setText(getString(R.string.label_update));
            downloadButton.setEnabled(true);
            downloadButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        } else {
            header.setText(getString(R.string.label_download_package));
            header.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            downloadButton.setEnabled(false);
            downloadButton.setTextColor(Color.parseColor("#757575"));
        }
        boolean unset = Downloader.getLastDownloadedTag(getContext()).equals("");
        if (unset && prefs.getString("running_download_tag", "unset").equals("unset")) {
            header.setText(getString(R.string.label_download_package));
            header.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            downloadButton.setText(getString(R.string.label_download));
            downloadButton.setEnabled(true);
            downloadButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        }
        restoreDownloadProgress();
    }

    /**
     * Mostly handles special cases like change of GApps-Selection and firstRun-Behaviour
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.contains("selection") || s.contains("last_downloaded"))
            initSelections();
        if (!prefs.getBoolean("firstStart", true)) {
            if (s.equals("selection_android") || s.equals("selection_arch") || s.equals("selection_variant")) {
                updateSelection();
            }
        }
        if (s.equals("firstStart")) {
            initDownloader(isRestored);
            setNewVersionAvailable(false);
        }
    }

    /**
     * Clears all settings to give the user a fresh start
     */
    private void updateSelection() {
        SharedPreferences.Editor editor = prefs.edit();
        String lastDL = Downloader.getLastDownloadedTag(getContext());
        if (lastDL.equals(""))
            editor.remove("last_downloaded_tag").apply();
        else
            editor.putString("last_downloaded_tag", Downloader.getLastDownloadedTag(getContext())).apply();
        Downloader.setLastFile(getContext(), false);
        lastTag = "";
        initDownloader(false);
    }

    private void loadInstallCards() {
        for (File file : findFiles()) {
            if (!fileCards.containsKey(file.getAbsolutePath())) {
                fileCards.put(file.getAbsolutePath(), addInstallCard(file));
            }
        }
    }

    private InstallCard addInstallCard(File file) {
        LinearLayout layout = (LinearLayout) getView().findViewById(R.id.main_layout);
        InstallCard card = new InstallCard(getContext());
        card.setDeleteListener(this);
        card.setFile(file);
        card.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) card.getLayoutParams();
        params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), 0);
        card.setLayoutParams(params);
        card.setVisibility(View.VISIBLE);
        layout.addView(card, layout.getChildCount() - 1);
        return card;
    }

    private File[] findFiles() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            return new File[]{};
        File downloadDir = new File(prefs.getString("download_dir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.startsWith("open_gapps-") && name.endsWith(".zip");
            }
        };

        File[] files = downloadDir.listFiles(filter);
        Arrays.sort(files);
        return files;

    }

    private int dpToPx(@SuppressWarnings("SameParameterValue") int dp) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public void OnTagUpdated() {
        lastTag = downloader.getTag();
        prefs.edit().putString("last_downloaded_tag", Downloader.getLastDownloadedTag(getContext())).apply();
        refreshLayout.setRefreshing(false);
        TextView version = (TextView) getView().findViewById(R.id.newest_version);
        version.setText(convertDate(lastTag));
        if (Downloader.getLastDownloadedTag(getContext()).equals(lastTag))
            setNewVersionAvailable(false);
        else
            setNewVersionAvailable(true);

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

    public void downloadStarted(long id, String tag) {
        prefs.edit().putLong("running_download_id", id).apply();
        prefs.edit().putString("running_download_tag", tag).apply();
    }

    @Override
    public void downloadFailed(int reason) {
        initDownloadButton();
        downloader = new Downloader(this);
        prefs.edit().putLong("running_download_id", 0).apply();
        prefs.edit().putString("running_download_tag", null).apply();
    }

    @Override
    public void downloadSuccessful(String filePath) {
        initDownloadButton();
        loadInstallCards();
        if (prefs.getBoolean("checkMissing", false)) {
            prefs.edit().remove("checkMissing").apply();
            fileCards.get(filePath).checkMD5();
        }
    }

    @Override
    public void downloadCancelled() {
        initDownloadButton();
        downloader = new Downloader(this);
        downloader.new TagUpdater();
        prefs.edit().putLong("running_download_id", 0).apply();
        prefs.edit().putString("running_download_tag", null).apply();
    }

    public void hashSuccess(Boolean match) {
        if (match) {
            String tag = prefs.getString("running_download_tag", "failed");
            if (!tag.equals("failed")) // dirty hack :(
                prefs.edit().putString("last_downloaded_tag", tag).apply();
            setNewVersionAvailable(false);
        } else {
            Toast.makeText(getContext(), "CHECKSUM DOES NOT MATCH", Toast.LENGTH_LONG).show();
        }
        loadInstallCards();
        downloadCancelled();
    }

    @Override
    public void onRefresh() {
//        loadInstallCards();
        downloader.new TagUpdater().execute();
    }
}
