package org.opengapps.app.download;

import android.Manifest;
import android.app.DownloadManager;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import org.opengapps.app.DownloadFragment;
import org.opengapps.app.R;
import org.opengapps.app.prefs.Preferences;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static android.content.Context.MODE_PRIVATE;

@SuppressWarnings("ConstantConditions")
public class Downloader extends AsyncTask<Void, Void, Long> {
    private final static String downloadUrl = "https://github.com/opengapps/%arch/releases/download/%tag/open_gapps-%arch-%android-%variant-%tag.zip";
    private final static String feedUrl = "https://github.com/opengapps/%arch/releases.atom";
    public static String defaultDownloadDir;
    public static final String OPENGAPPS_PREDEFINED_PATH = "/Download/OpenGApps";

    private final DownloadFragment downloadFragment;
    private String architecture, android, variant, tag;
    private FragmentManager manager;
    private boolean errorOccured = false;
    private File feedFile;
    private String urlString;
    private String baseUrl;
    private SharedPreferences prefs;
    private DownloadManager downloadManager;
    private DownloadProgressView progress;

    public Downloader(DownloadFragment downloadFragment) {
        this.downloadFragment = downloadFragment;
        manager = downloadFragment.getFragmentManager();
        prefs = downloadFragment.getContext().getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        downloadManager = (DownloadManager) downloadFragment.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        this.architecture = prefs.getString("selection_arch", "arm");
        this.android = prefs.getString("selection_android", null);
        this.variant = prefs.getString("selection_variant", null);
        feedFile = new File(downloadFragment.getContext().getFilesDir(), "gapps_feed.xml");
        urlString = feedUrl.replace("%arch", architecture);
        baseUrl = downloadUrl;
        defaultDownloadDir = DownloadFragment.getDownloadDir(downloadFragment.getContext());
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (downloadFragment != null) {
            if (!errorOccured) {
                progress = (DownloadProgressView) downloadFragment.getView().findViewById(R.id.progress_view);
                progress.begin();
            } else {
                Toast.makeText(downloadFragment.getContext(), downloadFragment.getString(R.string.download_started), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected Long doInBackground(Void... voids) {
        if (errorOccured) {
            return -1L;
        }
        if (tag == null) {
            tag = parseFeed();
        }
        Uri uri = generateUri();
        try {
            return doDownload(uri);
        } catch (SecurityException e) {
            return (long) -2;
        }
    }

    @Override
    protected void onPostExecute(Long id) {
        if (id == -1) {
            checkAndHandleError();
            return;
        } else if (id == -2) {
            try {
                downloadFragment.downloadFailed(0);
                Toast.makeText(downloadFragment.getContext(), "You cant save to this directory", Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
            }
        }
        DownloadFragment fragment = (DownloadFragment) manager.findFragmentByTag(DownloadFragment.TAG);

        if (fragment != null) {
            DownloadProgressView progress = (DownloadProgressView) fragment.getView().findViewById(R.id.progress_view);
            progress.show(id, fragment);
            fragment.downloadStarted(id, tag);
        }
        prefs.edit().putBoolean("checkMissing", true).apply();
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public static void deleteOldFiles(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            return;
        }
        File downloadDir = new File(prefs.getString("download_dir", defaultDownloadDir));
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                boolean nameFits = name.startsWith("open_gapps-") && name.endsWith(".zip");
                boolean isCurrentSelection = name.contains(prefs.getString("selection_android", "unset")) && name.contains(prefs.getString("selection_arch", "unset").toLowerCase()) && name.contains(prefs.getString("selection_variant", "unset"));
                return nameFits && isCurrentSelection;
            }
        };

        File[] files = downloadDir.listFiles(filter);
        if (files == null || files.length == 0) {
            return;
        }
        Arrays.sort(files);
        int keptFiles = prefs.getInt("kept_packages", 1);
        for (int i = 0; i < files.length - keptFiles - 1; i++) {
            //noinspection ResultOfMethodCallIgnored
            deletePackage(files[i]);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void deletePackage(File gappsFile) {
        File md5File = new File(gappsFile.getAbsolutePath() + DownloadFragment.md5FileExtension);
        String versionLog = gappsFile.getAbsolutePath().substring(0, gappsFile.getAbsolutePath().length() - ".zip".length()) + DownloadFragment.versionlogFileExtension;
        File versionlogFile = new File(versionLog);
        md5File.delete();
        versionlogFile.delete();
        gappsFile.delete();
    }

    private Uri generateUri() {
        String url = baseUrl;
        url = url.replace("%arch", architecture);
        url = url.replace("%tag", tag);
        url = url.replace("%variant", variant);
        url = url.replace("%android", android);
        return Uri.parse(url);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean runningDownload(Context context, String expectedName) {
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor c = manager.query(query.setFilterByStatus(7));
        if (c.moveToFirst()) {
            String title = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
            return expectedName.contains(title);
        }
        return false;
    }

    private String parseFeed() {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new FileReader(feedFile));

            List<String> entries = new ArrayList<>(5);
            try {
                boolean firstEntryFound = false;
                parser.next();
                parser.require(XmlPullParser.START_TAG, null, "feed");
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String name = parser.getName();
                    if (name.equals("entry")) {
                        firstEntryFound = true;
                    }
                    if (firstEntryFound && name.equals("id")) {
                        parser.next();
                        String text = parser.getText().substring(parser.getText().lastIndexOf('/') + 1);
                        if (text.matches("[0-9]+")) {
                            entries.add(parser.getText().substring(parser.getText().lastIndexOf('/') + 1));
                        }
                    }
                }
            } catch (XmlPullParserException | IOException e) {
                return "";
            }
            String[] entryArray = entries.toArray(new String[entries.size()]);
            Arrays.sort(entryArray);
            return entryArray[entryArray.length - 1];
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void checkAndHandleError() {
        try {
            downloadFragment.downloadFailed(0);
            Toast.makeText(downloadFragment.getContext(), R.string.label_download_error, Toast.LENGTH_SHORT).show();
        } catch (NullPointerException ignored) {
        }
    }

    private void refreshFeed() {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(urlString)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful() || response.isRedirect()) {
                errorOccured = true;
            }


            FileWriter fileWriter = new FileWriter(feedFile, false);
            ResponseBody body = response.body();
            fileWriter.write(body.string());
            body.close();
            fileWriter.close();
        } catch (IOException ignored) {
            errorOccured = true;
        }
    }

    private long doDownload(Uri uri) throws SecurityException {
        DownloadManager.Request request = new DownloadManager.Request(uri);
        String title = "open_gapps" + "-" + architecture.toLowerCase() + "-" + android.toLowerCase() + "-" + variant.toLowerCase() + "-" + tag.toLowerCase();
        request.setTitle(title);
        if (prefs.getBoolean("download_wifi_only", true)) {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        }
        File path = new File(prefs.getString("download_dir", defaultDownloadDir));
        File gappsPackage = new File(path, title + ".zip");
        if (prefs.getBoolean("download_md5", true)) {
            downloadMD5(uri.toString(), new File(gappsPackage.getAbsolutePath() + DownloadFragment.md5FileExtension));
        }
        if (prefs.getBoolean("download_versionlog", false)) {
            downloadVersionLog(uri.toString(), new File(path, title + DownloadFragment.versionlogFileExtension));
        }
        gappsPackage = new File(gappsPackage + ".tmp");
        request.setDestinationUri(Uri.fromFile(gappsPackage));
        return downloadManager.enqueue(request);
    }

    private void downloadVersionLog(String uri, File file) {
        uri = uri.substring(0, uri.length() - ".zip".length()) + DownloadFragment.versionlogFileExtension;
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(uri)
                    .build();
            Response response = client.newCall(request).execute();
            FileWriter fileWriter = new FileWriter(file, false);
            fileWriter.write(response.body().string());
            fileWriter.close();
        } catch (Exception ignored) {
        }
    }

    private void downloadMD5(String uri, File file) {
        uri += DownloadFragment.md5FileExtension;
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(uri)
                    .build();
            Response response = client.newCall(request).execute();
            InputStream inputStream = response.body().byteStream();
            FileOutputStream fos = new FileOutputStream(file);
            int inByte;
            while ((inByte = inputStream.read()) != -1) {
                fos.write(inByte);
            }
            inputStream.close();
            fos.close();
        } catch (Exception ignored) {
        }
    }


    public static String getDownloadedFile(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
        String architecture = prefs.getString("selection_arch", null).toLowerCase();
        String android = prefs.getString("selection_android", null).toLowerCase();
        String variant = prefs.getString("selection_variant", null).toLowerCase();
        String tag = prefs.getString("last_downloaded_tag", null).toLowerCase();
        String path = prefs.getString("download_dir", defaultDownloadDir);
        return path + "/" + "open_gapps" + "-" + architecture + "-" + android + "-" + variant + "-" + tag + ".zip";
    }

    public static String getLastDownloadedTag(@NonNull Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            SharedPreferences prefs = context.getSharedPreferences(Preferences.prefName, MODE_PRIVATE);
            final String architecture = prefs.getString("selection_arch", "").toLowerCase();
            final String selection_android = prefs.getString("selection_android", "").toLowerCase();
            final String variant = prefs.getString("selection_variant", "").toLowerCase();
            final String filterString = "open_gapps" + "-" + architecture + "-" + selection_android + "-" + variant + "-";
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(filterString) && name.endsWith(".zip");
                }
            };


            File downloadDir = new File(prefs.getString("download_dir", defaultDownloadDir));
            File[] files = downloadDir.listFiles(filter);
            if (files == null || files.length == 0) {
                return "";
            }
            Arrays.sort(files);
            if (files.length >= 1) {
                String tag = files[files.length - 1].getName();
                tag = tag.substring(filterString.length(), tag.length() - 4);
                return tag;
            } else {
                return "";
            }
        }
        return "";
    }

    public DownloadProgressView getProgressView() {
        return progress;
    }

    public class
    TagUpdater extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            refreshFeed();
            String feed = parseFeed();
            if (errorOccured) {
                return "";
            }
            return feed;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            errorOccured = false;
        }

        @Override
        protected void onPostExecute(String s) {
            if (errorOccured) {
                checkAndHandleError();
            }
            tag = s;
            if (downloadFragment != null && downloadFragment.isVisible()) {
                downloadFragment.onTagUpdated();
            }
        }
    }
}
