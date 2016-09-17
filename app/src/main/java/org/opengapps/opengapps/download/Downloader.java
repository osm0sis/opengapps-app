package org.opengapps.opengapps.download;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.opengapps.opengapps.DownloadFragment;
import org.opengapps.opengapps.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

@SuppressWarnings("ConstantConditions")
public class Downloader extends AsyncTask<Void, Void, Long> {
    private final DownloadFragment downloadFragment;
    private String architecture, android, variant, tag;
    private static File lastFile;
    private File feedFile;
    private String urlString;
    private String baseUrl;
    private FirebaseAnalytics analytics;
    private SharedPreferences prefs;
    private DownloadManager downloadManager;

    public Downloader(DownloadFragment downloadFragment) {
        this.downloadFragment = downloadFragment;
        analytics = FirebaseAnalytics.getInstance(downloadFragment.getContext());
        prefs = downloadFragment.getContext().getSharedPreferences(downloadFragment.getString(R.string.pref_name), MODE_PRIVATE);
        downloadManager = (DownloadManager) downloadFragment.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        this.architecture = prefs.getString("selection_arch", "arm");
        this.android = prefs.getString("selection_android", null);
        this.variant = prefs.getString("selection_variant", null);
        feedFile = new File(downloadFragment.getContext().getFilesDir(), "gapps_feed.xml");
        urlString = downloadFragment.getString(R.string.feed_url).replace("%arch", architecture);
        baseUrl = downloadFragment.getString(R.string.download_url);
        setLastFile(downloadFragment.getContext(), true);
    }

    public static void setLastFile(Context context, boolean fileExists) {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.pref_name), MODE_PRIVATE);
        String architecture = prefs.getString("selection_arch", "arm").toLowerCase();
        String android = prefs.getString("selection_android", "").toLowerCase();
        String variant = prefs.getString("selection_variant", "").toLowerCase();
        String path = prefs.getString("download_dir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        String title = "open_gapps-" + architecture + "-" + android + "-" + variant + "-" + getLastDownloadedTag(context);
        File f = new File(path, title + ".zip");
        if (fileExists && f.exists())
            lastFile = f;
        if (!fileExists)
            lastFile = null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (downloadFragment != null)
            Toast.makeText(downloadFragment.getContext(), downloadFragment.getString(R.string.download_started), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected Long doInBackground(Void... voids) {
        if (tag == null)
            tag = parseFeed();
        Uri uri = generateUri();
        return doDownload(uri);
    }

    @Override
    protected void onPostExecute(Long id) {
        if (downloadFragment != null) {
            DownloadProgressView progress = (DownloadProgressView) downloadFragment.getView().findViewById(R.id.progress_view);
            progress.show(id, downloadFragment);
            downloadFragment.downloadStarted(id, tag);
        }
        prefs.edit().putBoolean("checkMissing", true).apply();
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public static void deleteLastFile() {
        if (lastFile != null)
            //noinspection ResultOfMethodCallIgnored
            lastFile.delete();
    }

    public static void setLastFile(File lastFile) {
        Downloader.lastFile = lastFile;
    }

    public class TagUpdater extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            refreshFeed();
            return parseFeed();
        }

        @Override
        protected void onPostExecute(String s) {
            logSelections();
            tag = s;
            if (downloadFragment != null && downloadFragment.isVisible())
                downloadFragment.OnTagUpdated();
        }
    }

    private void logSelections() {
        for (String entry : new String[]{"selection_arch", "selection_android", "selection_variant"}) {
            Bundle params = new Bundle(1);
            params.putString(FirebaseAnalytics.Param.VALUE, prefs.getString(entry, "null"));
            analytics.logEvent(entry, params);
        }
    }

    private Uri generateUri() {
        String url = baseUrl;
        url = url.replace("%arch", architecture);
        url = url.replace("%tag", tag);
        url = url.replace("%variant", variant);
        url = url.replace("%android", android);
        return Uri.parse(url);
    }

    private String parseFeed() {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new FileReader(feedFile));
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("link") && xpp.getAttributeValue(0).equals("alternate")) {
                    String href = xpp.getAttributeValue(null, "href");
                    return href.substring(href.lastIndexOf('/') + 1);
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void refreshFeed() {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(urlString)
                    .build();

            Response response = client.newCall(request).execute();


            FileWriter fileWriter = new FileWriter(feedFile, false);
            fileWriter.write(response.body().string());
            fileWriter.close();
        } catch (IOException ignored) {
        }
    }

    private long doDownload(Uri uri) {
        if (lastFile != null) {
            //noinspection ResultOfMethodCallIgnored
            lastFile.delete();
        }
        DownloadManager.Request request = new DownloadManager.Request(uri);
        String title = "open_gapps" + "-" + architecture.toLowerCase() + "-" + android.toLowerCase() + "-" + variant.toLowerCase() + "-" + tag.toLowerCase();
        request.setTitle(title);
        if (prefs.getBoolean("download_wifi_only", true))
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        File path = new File(prefs.getString("download_dir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()));
        File gappsPackage = new File(path, title + ".zip");
        if (prefs.getBoolean("download_md5", true))
            downloadMD5(uri.toString(), new File(path, title + ".zip" + ".md5"));
        if (prefs.getBoolean("download_versionlog", false))
            downloadVersionLog(uri.toString(), new File(path, title + ".versionlog.txt"));
        lastFile = gappsPackage;
        request.setDestinationUri(Uri.fromFile(gappsPackage));
        return downloadManager.enqueue(request);
    }

    private void downloadVersionLog(String uri, File file) {
        uri = uri.substring(0, uri.length() - 4) + ".versionlog.txt";
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
        uri += ".md5";
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


    public boolean fileExists() {
        return !getLastDownloadedTag(downloadFragment.getContext()).equals("");
    }

    public static String getDownloadedFile(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.pref_name), MODE_PRIVATE);
        String architecture = prefs.getString("selection_arch", null).toLowerCase();
        String android = prefs.getString("selection_android", null).toLowerCase();
        String variant = prefs.getString("selection_variant", null).toLowerCase();
        String tag = prefs.getString("last_downloaded_tag", null).toLowerCase();
        String path = prefs.getString("download_dir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        return path + "/" + "open_gapps" + "-" + architecture + "-" + android + "-" + variant + "-" + tag + ".zip";
    }

    public static String getLastDownloadedTag(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.pref_name), MODE_PRIVATE);
            final String architecture = prefs.getString("selection_arch", null).toLowerCase();
            final String selection_android = prefs.getString("selection_android", null).toLowerCase();
            final String variant = prefs.getString("selection_variant", null).toLowerCase();
            final String filterString = "open_gapps" + "-" + architecture + "-" + selection_android + "-" + variant + "-";
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(filterString) && name.endsWith(".zip");
                }
            };


            File downloadDir = new File(prefs.getString("download_dir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
            File[] files = downloadDir.listFiles(filter);
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
}
