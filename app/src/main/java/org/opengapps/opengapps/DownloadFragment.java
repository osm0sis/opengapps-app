package org.opengapps.opengapps;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.opengapps.opengapps.card.DownloadCard;
import org.opengapps.opengapps.card.InstallCard;
import org.opengapps.opengapps.card.PermissionCard;
import org.opengapps.opengapps.download.DownloadProgressView;
import org.opengapps.opengapps.download.Downloader;
import org.opengapps.opengapps.prefs.Preferences;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;


@SuppressWarnings("ConstantConditions")
public class DownloadFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, DownloadProgressView.DownloadStatusListener, SwipeRefreshLayout.OnRefreshListener {
    private final static String interstitialAdId = "ca-app-pub-9489060368971640/9426486679";
    public final static String TAG = "downloadFragment";

    private Downloader downloader;
    private SharedPreferences prefs;
    private DownloadCard downloadCard;
    private InterstitialAd downloadAd;
    private SwipeRefreshLayout refreshLayout;
    private boolean downloaderLoaded = false;
    private static HashMap<String, InstallCard> fileCards = new HashMap<>();
    public static boolean isRestored = false;
    private static String lastTag = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getContext().getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadInstallCards();
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
        downloadAd.setAdUnitId(interstitialAdId);
        requestAd();
        downloadAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestAd();
            }
        });

        prefs = getContext().getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);

        downloadCard = (DownloadCard) getView().findViewById(R.id.download_card);
        downloadCard.init(this);

        if (!prefs.getBoolean("firstStart", true))
            initPermissionCard();
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

    private void initDownloader(boolean isRestored) {
        downloader = new Downloader(this);
        if (!isRestored) {
            downloader.new TagUpdater().execute();
            refreshLayout.setRefreshing(true);
        } else {
            downloader.setTag(lastTag);
            onTagUpdated();
        }
    }

    private void initPermissionCard() {
        PermissionCard permissionCard = (PermissionCard) getView().findViewById(R.id.permission_card);
        permissionCard.init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        SharedPreferences preferences = getContext().getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        if (!preferences.getBoolean("firstStart", true)) {
            initPermissionCard();
            loadInstallCards();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.contains("selection")) {
            downloadCard.initSelections();
            if (!prefs.getBoolean("firstStart", true))
                updateSelection();
        }
        if (s.equals("firstStart")) {
            initDownloader(isRestored);
            downloadCard.setState(DownloadCard.DownloadCardState.NORMAL);
        }
    }

    /**
     * Clears all settings to give the user a fresh start
     */
    private void updateSelection() {
        SharedPreferences.Editor editor = prefs.edit();
        String lastDL = Downloader.getLastDownloadedTag(getContext());
        if (TextUtils.isEmpty(lastDL))
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
                fileCards.put(file.getAbsolutePath(), createAndAddInstallCard(file));
            }
        }
    }

    private InstallCard createAndAddInstallCard(File file) {
        LinearLayout layout = (LinearLayout) getView().findViewById(R.id.main_layout);
        InstallCard card = new InstallCard(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), 0);
        card.setDeleteListener(this);
        card.setFile(file);
        card.setVisibility(View.VISIBLE);
        layout.addView(card, layout.getChildCount() - 1, params);
        return card;
    }

    private File[] findFiles() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            return new File[]{};
        File downloadDir = new File(prefs.getString("download_dir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                boolean nameFits = name.startsWith("open_gapps-") && name.endsWith(".zip");
                boolean isCurrentDownload = name.contains(prefs.getString("selection_arch", "unset").toLowerCase()) && name.contains(prefs.getString("selection_android", "unset")) && name.contains(prefs.getString("selection_variant", "unset").toLowerCase()) && name.contains(prefs.getString("running_download_tag", "unset"));
                return nameFits && !isCurrentDownload;
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

    public void onTagUpdated() {
        lastTag = downloader.getTag();
        prefs.edit().putString("last_downloaded_tag", Downloader.getLastDownloadedTag(getContext())).apply();
        refreshLayout.setRefreshing(false);
        downloadCard.onTagUpdated(lastTag);

    }

    public void downloadStarted(long id, String tag) {
        prefs.edit().putLong("running_download_id", id).apply();
        prefs.edit().putString("running_download_tag", tag).apply();
    }

    @Override
    public void downloadFailed(int reason) {
        downloader = new Downloader(this);
        prefs.edit().putLong("running_download_id", 0).apply();
        prefs.edit().putString("running_download_tag", null).apply();
    }

    @Override
    public void downloadSuccessful(String filePath) {
        loadInstallCards();
        if (prefs.getBoolean("checkMissing", false)) {
            prefs.edit().remove("checkMissing").apply();
            InstallCard fileCard = fileCards.get(filePath);
            if (fileCard != null)
                fileCard.checkMD5();
        }
    }

    @Override
    public void downloadCancelled() {
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
            onTagUpdated();
        } else {
            Toast.makeText(getContext(), "CHECKSUM DOES NOT MATCH", Toast.LENGTH_LONG).show();
        }
        loadInstallCards();
        downloadCancelled();
    }

    @Override
    public void onRefresh() {
        loadInstallCards();
        downloader.new TagUpdater().execute();
    }

    public void showAd() {
        if (downloadAd.isLoaded())
            downloadAd.show();
    }

    public InstallCard getInstallCard(String path) {
        return fileCards.get(path);
    }

    public Downloader getDownloader() {
        return downloader;
    }
}
