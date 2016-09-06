package org.opengapps.opengapps;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.opengapps.opengapps.download.DownloadProgressView;
import org.opengapps.opengapps.download.Downloader;
import org.opengapps.opengapps.download.FileValidator;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.MODE_PRIVATE;


@SuppressWarnings("ConstantConditions")
public class DownloadFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, DownloadProgressView.DownloadStatusListener, SwipeRefreshLayout.OnRefreshListener {
    private Downloader downloader;
    private SharedPreferences prefs;
    private InterstitialAd downloadAd;
    private SwipeRefreshLayout refreshLayout;
    private boolean downloaderLoaded = false;
    private static boolean isRestored = false;
    private static String lastTag = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getContext().getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isRestored", true);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        initPermissionCard();
        if (downloader == null) {
            initDownloader(isRestored);
        }
        if (!downloader.fileExists() && prefs.getLong("running_download_id", 0) == 0) {
            onDeleteFile();
        }


        isRestored = true;
    }


    public void onDeleteFile() {
        prefs.edit().remove("last_downloaded_tag").apply();
        setNewVersionAvailable(true);
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
        setHasOptionsMenu(true);

        return inflater.inflate(R.layout.content_main, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.dl_refresher);
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        InstallCard installCard = (InstallCard) getView().findViewById(R.id.install_card);
        installCard.setDeleteListener(this);

        refreshLayout.setOnRefreshListener(this);
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getContext());
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
    }

    private void requestAd() {
        AdRequest request;
        if (BuildConfig.DEBUG)
            request = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice("F98ACBE481522BAE0A91AC208FDF938F")
                    .addTestDevice("CAAA7C86D5955208EF75484D93E09948")
                    .addTestDevice(Settings.Secure.getString(getContext().getContentResolver(), "android_id"))
                    .build();
        else
            request = new AdRequest.Builder()
                    .build();
        downloadAd.loadAd(request);
    }

    private void restoreDownloadProgress() {
        Long id = prefs.getLong("running_download_id", 0);
        if (id != 0) {
            DownloadProgressView progress = (DownloadProgressView) getView().findViewById(R.id.progressView);
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
        CardView permssionCard = (CardView) getView().findViewById(R.id.permission_card);
        int permissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED)
            permssionCard.setVisibility(View.GONE);
        else {
            permssionCard.setVisibility(View.VISIBLE);
            initPermissionButton();
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

    private void initPermissionButton() {
        Button permissionButton = (Button) getView().findViewById(R.id.grant_permission_button);
        permissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!prefs.getBoolean("firstStart", true))
            initPermissionCard();
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
        InstallCard installCard = (InstallCard) getView().findViewById(R.id.install_card);

        card.setVisibility(View.VISIBLE);
        if (updateAvailable) {
            header.setText(getString(R.string.update_available));
            header.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            downloadButton.setText(getString(R.string.label_update));
            downloadButton.setEnabled(true);
            downloadButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            installCard.setVisibility(View.VISIBLE);
        } else {
            header.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            downloadButton.setEnabled(false);
            downloadButton.setTextColor(Color.parseColor("#757575"));
            installCard.setFile(new File(Downloader.getDownloadedFile(getContext())));
            installCard.setVisibility(View.VISIBLE);
        }
        if (prefs.getString("last_downloaded_tag", "unset").equals("unset") && prefs.getString("running_download_tag", "unset").equals("unset")) {
            header.setText(getString(R.string.label_download));
            header.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            downloadButton.setText(getString(R.string.label_download));
            downloadButton.setEnabled(true);
            downloadButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
            installCard.setVisibility(View.INVISIBLE);
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
                clearAll();
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
    private void clearAll() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("last_downloaded_tag");
        editor.apply();
        downloader.deleteLastFile();
        downloader = new Downloader(this);
        downloader.new TagUpdater();
    }

    public void OnTagUpdated() {
        lastTag = downloader.getTag();
        refreshLayout.setRefreshing(false);
        TextView version = (TextView) getView().findViewById(R.id.newest_version);
        version.setText(convertDate(lastTag));
        if (downloader.fileExists()) {
            if (prefs.getString("last_downloaded_tag", "").equals(lastTag))
                setNewVersionAvailable(false);
            else
                setNewVersionAvailable(true);
        }
    }

    private String convertDate(String tag) {
        if (tag == null)
            return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date date = null;
        try {
            date = sdf.parse(tag);
        } catch (ParseException e) {
            e.printStackTrace();
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
        Log.e("DL", "I AM SUCCESSFUL. ONCE");
        if (prefs.getBoolean("checkMissing", false)) {
            prefs.edit().remove("checkMissing").apply();
            new FileValidator(this).execute(filePath);
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
        downloadCancelled();
    }

    @Override
    public void onRefresh() {
        downloader.new TagUpdater().execute();
    }
}
