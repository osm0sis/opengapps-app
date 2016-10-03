package org.opengapps.opengapps;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import org.opengapps.opengapps.card.DownloadCard;
import org.opengapps.opengapps.card.InstallCard;
import org.opengapps.opengapps.card.PermissionCard;
import org.opengapps.opengapps.download.DownloadProgressView;
import org.opengapps.opengapps.download.Downloader;
import org.opengapps.opengapps.prefs.Preferences;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static android.content.Context.MODE_PRIVATE;


@SuppressWarnings("ConstantConditions")
public class DownloadFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, DownloadProgressView.DownloadStatusListener, SwipeRefreshLayout.OnRefreshListener {
    public final static String TAG = "downloadFragment";
    private final static String interstitialAdId = "ca-app-pub-9489060368971640/9426486679";
    public static boolean isRestored = false;
    private static String lastTag = "";
    private ConcurrentHashMap<String, InstallCard> fileCards = new ConcurrentHashMap<>();
    private Downloader downloader;
    private SharedPreferences prefs;
    private Context globalContext;
    private DownloadCard downloadCard;
    private InterstitialAd downloadAd;
    private SwipeRefreshLayout refreshLayout;
    private boolean downloaderLoaded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        globalContext = getActivity().getApplicationContext();
        prefs = getActivity().getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
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
        prefs.edit().putString("last_downloaded_tag", Downloader.getLastDownloadedTag(getActivity())).apply();
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
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.colorPrimary));

        refreshLayout.setOnRefreshListener(this);
        downloadAd = new InterstitialAd(getActivity());
        downloadAd.setAdUnitId(interstitialAdId);
        requestAd();
        downloadAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestAd();
            }
        });

        prefs = getActivity().getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
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

    public void initDownloader(boolean isRestored) {
        downloader = new Downloader(this);
        if (!isRestored || TextUtils.isEmpty(lastTag)) {
            refreshLayout.setRefreshing(true);
            downloader.new TagUpdater().execute();
        } else {
            downloader.setTag(lastTag);
            onTagUpdated();
        }
    }

    private void initPermissionCard() {
        ((PermissionCard) getView().findViewById(R.id.permission_card)).init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        SharedPreferences preferences = getActivity().getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
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
            downloadCard.setState(DownloadCard.State.NORMAL);
        }
    }

    /**
     * Clears all settings to give the user a fresh start
     */
    private void updateSelection() {
        SharedPreferences.Editor editor = prefs.edit();
        String lastDL = Downloader.getLastDownloadedTag(globalContext);
        if (TextUtils.isEmpty(lastDL))
            editor.remove("last_downloaded_tag").apply();
        else
            editor.putString("last_downloaded_tag", lastDL).apply();
        Downloader.setLastFile(globalContext, false);
        lastTag = "";
        initDownloader(false);
    }

    private void loadInstallCards() {
        for (InstallCard card : fileCards.values()) {
            if (!card.exists()) {
                ((LinearLayout) getView().findViewById(R.id.main_layout)).removeView(card);
                onDeleteFile(card.getGappsFile());
                fileCards.remove(card.getGappsFile().getAbsolutePath());
            }
        }
        for (File file : findFiles()) {
            if (!fileCards.containsKey(file.getAbsolutePath())) {
                fileCards.put(file.getAbsolutePath(), createAndAddInstallCard(file));
            }
        }
    }

    private InstallCard createAndAddInstallCard(File file) {
        InstallCard card = createInstallCard(file);
        addInstallCard(card);
        return card;
    }

    private InstallCard createInstallCard(File file) {
        InstallCard card = new InstallCard(getActivity());
        card.setDeleteListener(this);
        card.setFile(file);
        card.setVisibility(View.VISIBLE);
        return card;
    }

    private void addInstallCard(InstallCard installCard) {
        LinearLayout layout = (LinearLayout) getView().findViewById(R.id.main_layout);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(getActivity(), 8), dpToPx(getActivity(), 8), dpToPx(getActivity(), 8), 0);
        installCard.setVisibility(View.VISIBLE);
        layout.addView(installCard, layout.getChildCount() - 1, params);
    }

    private File[] findFiles() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            return new File[]{};
        File downloadDir = new File(prefs.getString("download_dir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/OpenGApps/"));

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                boolean nameFits = name.startsWith("open_gapps-") && name.endsWith(".zip");
                boolean runningDownload = name.contains(prefs.getString("selection_arch", "unset").toLowerCase()) &&
                        name.contains(prefs.getString("selection_android", "unset")) && name.contains(prefs.getString("selection_variant", "unset"));
                if (runningDownload)
                    return nameFits && !Downloader.runningDownload(getActivity(), name);
                return nameFits;
            }
        };

        File[] files = downloadDir.listFiles(filter);
        if (files != null)
            Arrays.sort(files);
        else
            files = new File[0];
        return files;
    }

    public static int dpToPx(Context context, int dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public void onTagUpdated() {
        if (downloader != null)
            lastTag = downloader.getTag();
        prefs.edit().putString("last_downloaded_tag", Downloader.getLastDownloadedTag(getActivity())).apply();
        refreshLayout.setRefreshing(false);
        if (downloader != null)
            downloadCard.onTagUpdated(lastTag);

    }

    public void downloadStarted(long id, String tag) {
        prefs.edit().putLong("running_download_id", id).apply();
        prefs.edit().putString("running_download_tag", tag).apply();
    }

    @Override
    public void downloadFailed(int reason) {
        downloadFinished();
    }

    @Override
    public void downloadSuccessful(String filePath) {
        loadInstallCards();
        if (prefs.getBoolean("download_delete_old_files", true))
            Downloader.deleteOldFiles(getActivity());
        if (prefs.getBoolean("checkMissing", false)) {
            prefs.edit().remove("checkMissing").apply();
            InstallCard fileCard = fileCards.get(filePath);
            if (fileCard != null)
                fileCard.checkMD5();
        }
    }

    @Override
    public void downloadCancelled() {
        downloadFinished();
    }

    public void downloadFinished() {
        downloader = new Downloader(this);
        onRefresh();
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
            Toast.makeText(getActivity(), R.string.label_checksum_invalid, Toast.LENGTH_LONG).show();
        }
        downloadFinished();
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
