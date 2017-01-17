package org.opengapps.app;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.codekidlabs.storagechooser.utils.DiskUtil;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import org.opengapps.app.card.DownloadCard;
import org.opengapps.app.card.InstallCard;
import org.opengapps.app.card.PermissionCard;
import org.opengapps.app.card.RateUsCard;
import org.opengapps.app.card.SupportCard;
import org.opengapps.app.download.DownloadProgressView;
import org.opengapps.app.download.Downloader;
import org.opengapps.app.prefs.Preferences;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static android.content.Context.MODE_PRIVATE;


@SuppressWarnings("ConstantConditions")
public class DownloadFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, DownloadProgressView.DownloadStatusListener, SwipeRefreshLayout.OnRefreshListener {
    public final static String TAG = "downloadFragment";
    private final static String interstitialAdId = "ca-app-pub-9489060368971640/1070663473";
    public final static String md5FileExtension = ".md5";
    public final static String versionlogFileExtension = ".versionlog.txt";
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

    private RateUsCard rateUsCard;
    private SupportCard supportCard;

    // status booleans used for checking if rate status is set
    // and show them during onResume
    private boolean isRateUsCardSet = false;
    private boolean isSupportCardSet = false;


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
        loadSupportCard();
        loadRateUsCard();
        isRestored = true;

        if (isRateUsCardSet) {
            showRateUsCard(prefs.getInt("rate_later_count", 0));
        }
        if (isSupportCardSet) {
            showSupportCard(prefs.getInt("support_later_count", 0));
        }

    }

    @Override
    public Context getContext() {
        return globalContext;
    }

    public void onDeleteFile(@Nullable File gappsFile) {
        if (gappsFile == null) {
            loadInstallCards();
        } else {
            InstallCard card = fileCards.get(gappsFile.getAbsolutePath());
            if (card != null) {
                LinearLayout layout = (LinearLayout) getView().findViewById(R.id.main_layout);
                layout.removeView(card);
            }
            fileCards.remove(gappsFile.getAbsolutePath());
        }
        prefs.edit().putString("last_downloaded_tag", Downloader.getLastDownloadedTag(globalContext)).apply();
        initDownloader(false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.content_main, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        cleanUp();
        refreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.dl_refresher);
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(globalContext, R.color.colorPrimary));

        refreshLayout.setOnRefreshListener(this);
        downloadAd = new InterstitialAd(globalContext);
        downloadAd.setAdUnitId(interstitialAdId);
        requestAd();
        downloadAd.setAdListener(ifAdLoadedListener);

        prefs = globalContext.getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);

        downloadCard = (DownloadCard) getView().findViewById(R.id.download_card);
        downloadCard.init(this);

        if (!prefs.getBoolean("firstStart", true)) {
            initPermissionCard();
        }
        if (!prefs.getBoolean("firstStart", true) && !downloaderLoaded) {
            initDownloader(isRestored);
            downloaderLoaded = true;
        }

        // counts to turn later to never
        if (!prefs.contains("rate_later_count")) {
            prefs.edit().putInt("rate_later_count", 0).apply();
        }
        if (!prefs.contains("support_later_count")) {
            prefs.edit().putInt("support_later_count", 0).apply();
        }
    }

    public static String getDownloadDir(Context context) {
        return context.getSharedPreferences(Preferences.prefName, MODE_PRIVATE).getString(DiskUtil.SC_PREFERENCE_KEY, Environment.getExternalStorageDirectory().getAbsolutePath() + Downloader.OPENGAPPS_PREDEFINED_PATH);
    }

    private void requestAd() {
        AdRequest request;
        if (BuildConfig.DEBUG) {
            request = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice("FD3ACD8C90A01E155D99775579EC45A9")
                    .addTestDevice("CAAA7C86D5955208EF75484D93E09948")
                    .build();
        } else {
            request = new AdRequest.Builder()
                    .build();
        }
        downloadAd.loadAd(request);
    }

    public void initDownloader(boolean isRestored) {
        downloader = new Downloader(this);
        refreshLayout.setProgressViewOffset(false, 100, 150);
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(true);
            }
        });
        if (!isRestored || TextUtils.isEmpty(lastTag)) {
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
        SharedPreferences preferences = globalContext.getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        if (!preferences.getBoolean("firstStart", true)) {
            initPermissionCard();
            loadInstallCards();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.d(TAG, "onSharedPreferenceChanged: " + s);
        if (s.contains("selection")) {
            downloadCard.initSelections();
            if (!prefs.getBoolean("firstStart", true)) {
                updateSelection();
            }
        } else if (s.equals("firstStart")) {
            initDownloader(isRestored);
            downloaderLoaded = true;
            downloadCard.setState(DownloadCard.State.NORMAL);
        } else if (s.equals("download_dir")) {
            InstallCard.invalidate = true;
        }
    }

    /**
     * Clears all settings to give the user a fresh start
     */
    private void updateSelection() {
        SharedPreferences.Editor editor = prefs.edit();
        String lastDL = Downloader.getLastDownloadedTag(globalContext);
        if (TextUtils.isEmpty(lastDL)) {
            editor.remove("last_downloaded_tag").apply();
        } else {
            editor.putString("last_downloaded_tag", lastDL).apply();
        }
        lastTag = "";
        initDownloader(false);
    }

    private void loadInstallCards() {
        if (getActivity() == null)
            return;
        for (InstallCard card : fileCards.values()) {
            if (!card.exists() || InstallCard.invalidate) {
                ((LinearLayout) getView().findViewById(R.id.main_layout)).removeView(card);
                onDeleteFile(card.getGappsFile());
                fileCards.remove(card.getGappsFile().getAbsolutePath());
            }
        }
        InstallCard.invalidate = false;
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

    private void loadRateUsCard() {
        int count = prefs.getInt("rate_count", 1);
        boolean rate_status = prefs.getBoolean("rate_done", false);
        int rateLaterCount = prefs.getInt("rate_later_count", 0);

        if (rateLaterCount < 2) {
            if (prefs.getBoolean("show_rate_card", false) && rateUsCard == null) {
                showRateUsCard(rateLaterCount);
            }

            if (count % 9 == 0 && !rate_status) {
                if (rateUsCard == null) {
                    showRateUsCard(rateLaterCount);
                }
            }
        }
    }

    private void showRateUsCard(final int laterCount) {
        final SharedPreferences.Editor editor = prefs.edit();

        if (rateUsCard == null) {
            isRateUsCardSet = true;
            editor.putBoolean("show_rate_card", isRateUsCardSet);
            editor.apply();
            rateUsCard = new RateUsCard(getActivity());
            if (laterCount == 1) {
                rateUsCard.setLaterLabel(getString(R.string.label_never));
            }
            rateUsCard.setRateListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    NavigationActivity.openURL(getActivity(), getString(R.string.url_ogapps_market));
                    editor.putBoolean("rate_done", true);
                    editor.apply();
                    rateUsCard.setVisibility(View.GONE);
                }
            });
            rateUsCard.setLaterListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    editor.putBoolean("rate_done", false);
                    editor.apply();
                    rateUsCard.setVisibility(View.GONE);
                    rateUsCard = null;
                    isRateUsCardSet = false;
                    editor.putBoolean("show_rate_card", isRateUsCardSet);
                    editor.putInt("rate_later_count", laterCount + 1);
                    editor.apply();
                }
            });
            LinearLayout layout = (LinearLayout) getView().findViewById(R.id.main_layout);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(dpToPx(globalContext, 8), dpToPx(globalContext, 8), dpToPx(globalContext, 8), 0);
            rateUsCard.setVisibility(View.VISIBLE);

            layout.addView(rateUsCard, 2, params);
        }
    }

    private void loadSupportCard() {
        Random random = new Random();
        int randomMin = 1;
        int randomMax = 20;
        int randomNum = random.nextInt((randomMax - randomMin) + 1) + randomMin;
        boolean support_status = prefs.getBoolean("support_done", false);
        boolean rate_status = prefs.getBoolean("rate_done", false);
        int supportLaterCount = prefs.getInt("support_later_count", 0);
        int rateLaterCount = prefs.getInt("rate_later_count", 0);

        if (supportLaterCount < 2 && !isRateUsCardSet) {
            // show only if user has rated OR user has clicked the later button atleast once
            if (rate_status || rateLaterCount > 0) {
                // this if -->
                // - checks if there was support card shown and there was no interaction by the
                // user only then show the card
                if (prefs.getBoolean("show_support_card", false) && supportCard == null) {
                    showSupportCard(supportLaterCount);
                }

                // general if --> shows the card if user didn't click the support button and the
                // random number is 11
                if (randomNum == 11 && !support_status) {
                    showSupportCard(supportLaterCount);
                }
            }
        }
    }

    private void showSupportCard(final int laterCount) {
        final SharedPreferences.Editor editor = prefs.edit();

        if (supportCard == null) {
            isSupportCardSet = true;
            editor.putBoolean("show_support_card", isSupportCardSet);
            editor.apply();
            supportCard = new SupportCard(getActivity());
            if (laterCount == 1) {
                supportCard.setLaterLabel(getString(R.string.label_never));
            }
            supportCard.setSupportButton(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    NavigationActivity.openURL(globalContext, getString(R.string.url_support_opengapps));
                    editor.putBoolean("support_done", true);
                    editor.apply();
                    supportCard.setVisibility(View.GONE);
                }
            });
            supportCard.setLaterListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    supportCard.setVisibility(View.GONE);
                    supportCard = null;
                    isSupportCardSet = false;
                    editor.putBoolean("show_support_card", isSupportCardSet);
                    editor.putInt("support_later_count", laterCount + 1);
                    editor.apply();
                }
            });
            supportCard.setVisibility(View.VISIBLE);

            LinearLayout layout = (LinearLayout) getView().findViewById(R.id.main_layout);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(dpToPx(globalContext, 8), dpToPx(globalContext, 8), dpToPx(globalContext, 8), 0);

            layout.addView(supportCard, 2, params);
        }
    }

    private void addInstallCard(InstallCard installCard) {
        LinearLayout layout = (LinearLayout) getView().findViewById(R.id.main_layout);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(globalContext, 8), dpToPx(globalContext, 8), dpToPx(globalContext, 8), 0);
        installCard.setVisibility(View.VISIBLE);
        layout.addView(installCard, layout.getChildCount() - 1, params);
    }

    private File[] findFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getContext() == null || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                return new File[]{};
            }
        }
        File downloadDir = new File(prefs.getString("download_dir", Downloader.defaultDownloadDir));

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                boolean nameFits = name.startsWith("open_gapps-") && name.endsWith(".zip");
                boolean runningDownload = name.contains(prefs.getString("selection_arch", "unset").toLowerCase()) &&
                        name.contains(prefs.getString("selection_android", "unset")) && name.contains(prefs.getString("selection_variant", "unset"));
                if (runningDownload) {
                    return nameFits && !Downloader.runningDownload(globalContext, name);
                }
                return nameFits;
            }
        };

        File[] files = downloadDir.listFiles(filter);
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (o1.lastModified() < o2.lastModified()) {
                        return 1;
                    } else if (o1.lastModified() > o2.lastModified()) {
                        return -1;
                    } else {
                        return o1.compareTo(o2);
                    }
                }
            });
        } else {
            files = new File[0];
        }
        return files;
    }

    public static int dpToPx(Context context, int dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static void cleanUp(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                return;
            }
        }
        SharedPreferences prefs = context.getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        File downloadDir = new File(prefs.getString("download_dir", getDownloadDir(context)));

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("open_gapps-") && (name.endsWith(md5FileExtension) || name.endsWith(versionlogFileExtension));
            }
        };
        File[] files = downloadDir.listFiles(filter);
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.getName().endsWith(md5FileExtension)) {
                File gappsFile = new File(file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - md5FileExtension.length()));
                File tmpGappsFile = new File(gappsFile.getAbsolutePath() + ".tmp");
                if (!gappsFile.exists() && !tmpGappsFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                    Log.d(TAG, "cleanUp: orphaned file \"" + file.getName() + "\" found. Deleting.");
                }
            } else if (file.getName().endsWith(versionlogFileExtension)) {
                File gappsFile = new File(file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - versionlogFileExtension.length()) + ".zip");
                File tmpGappsFile = new File(gappsFile.getAbsolutePath() + ".tmp");
                if (!gappsFile.exists() && !tmpGappsFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                    Log.d(TAG, "cleanUp: orphaned file \"" + file.getName() + "\" found. Deleting.");
                }
            }
        }

    }

    private void cleanUp() {
        cleanUp(globalContext);
    }

    public void onTagUpdated() {
        if (downloader != null) {
            lastTag = downloader.getTag();
        }
        prefs.edit().putString("last_downloaded_tag", Downloader.getLastDownloadedTag(globalContext)).apply();
        if (downloader != null) {
            downloadCard.onTagUpdated(lastTag);
        }
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(false);
            }
        });
    }

    public void downloadStarted(long id, String tag) {
        prefs.edit().putLong("running_download_id", id).apply();
        prefs.edit().putString("running_download_tag", tag).apply();
    }

    private void addFreshPackage(@Nullable final File result) {
        if (result == null || !result.exists()) {
            return;
        }
        LinearLayout layout = (LinearLayout) getView().findViewById(R.id.main_layout);
        if (layout == null) {
            return;
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(globalContext, 8), dpToPx(globalContext, 8), dpToPx(globalContext, 8), 0);
        InstallCard card = createInstallCard(result);
        fileCards.put(result.getAbsolutePath(), card);
        layout.addView(card, 2, params); // IMPROVEME -> The 2 here is set because 0 = Permissioncard, 1 = Downloadcard. This should actually be set dynamically but it works for now
    }

    @Override
    public void downloadFailed(int reason) {
        downloadFinished();
    }

    @Override
    public void downloadSuccessful(String filePath) {
        File gapps = new File(filePath);
        File result = null;
        if (gapps.exists()) {
            result = new File(filePath.substring(0, filePath.length() - ".tmp".length()));
            //noinspection ResultOfMethodCallIgnored
            gapps.renameTo(result);
        }
        addFreshPackage(result);
        if (prefs.getBoolean("download_delete_old_files", true)) {
            Downloader.deleteOldFiles(globalContext);
        }
        if (prefs.getBoolean("checkMissing", false)) {
            prefs.edit().remove("checkMissing").apply();
            InstallCard fileCard = fileCards.get(filePath);
            if (fileCard != null) {
                fileCard.checkMD5();
            }
        }
    }

    @Override
    public void downloadCancelled() {
        downloadFinished();
    }

    public void downloadFinished() {
        cleanUp();
        downloader = new Downloader(this);
        refreshLayout.setRefreshing(true);
        onRefresh();
        prefs.edit().putLong("running_download_id", 0).apply();
        prefs.edit().putString("running_download_tag", null).apply();
    }

    public void hashSuccess(Boolean match) {
        if (match) {
            String tag = prefs.getString("running_download_tag", "failed");
            if (!tag.equals("failed")) // dirty hack :(
            {
                prefs.edit().putString("last_downloaded_tag", tag).apply();
            }
            onTagUpdated();
        } else {
            Toast.makeText(globalContext, R.string.label_checksum_invalid, Toast.LENGTH_LONG).show();
        }
        downloadFinished();
    }

    @Override
    public void onRefresh() {
        loadInstallCards();
        loadRateUsCard();
        loadSupportCard();

        if (isRateUsCardSet) {
            showRateUsCard(prefs.getInt("rate_later_count", 0));
        }
        if (isSupportCardSet) {
            showSupportCard(prefs.getInt("support_later_count", 0));
        }


        //        downloadCard.onTagUpdated(PackageGuesser.getCurrentlyInstalled(getContext()));
        if (downloader != null) {
            downloader.new TagUpdater().execute();
        }
    }

    public static int spToPx(Context context, float sp) {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
        return px;
    }

    public void showAd() {
        if (downloadAd.isLoaded()) {
            downloadAd.show();
        } else {
            requestAd();
            downloadAd.setAdListener(ifAdNotLoadedListener);
        }
    }

    private AdListener ifAdLoadedListener = new AdListener() {
        @Override
        public void onAdClosed() {
            super.onAdClosed();
            requestAd();
        }
    };

    private AdListener ifAdNotLoadedListener = new AdListener() {
        @Override
        public void onAdClosed() {
            super.onAdClosed();
            requestAd();
        }

        @Override
        public void onAdLoaded() {
            super.onAdLoaded();
            showAd();
            downloadAd.setAdListener(ifAdLoadedListener);
        }
    };

    public InstallCard getInstallCard(String path) {
        return fileCards.get(path);
    }

    @Nullable
    public Downloader getDownloader() {
        if (downloader == null || downloader.getStatus() != AsyncTask.Status.PENDING) {
            if (getContext() == null)
                return null;
            downloader = new Downloader(this);
        }
        return downloader;
    }
}
