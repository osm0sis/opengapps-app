package org.opengapps.opengapps.download;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.IOException;

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

    public Downloader(DownloadFragment downloadFragment) {
        this.downloadFragment = downloadFragment;
        SharedPreferences prefs = downloadFragment.getContext().getSharedPreferences(downloadFragment.getString(R.string.pref_name), MODE_PRIVATE);
        this.architecture = prefs.getString("selection_arch", "arm");
        this.android = prefs.getString("selection_android", null);
        this.variant = prefs.getString("selection_variant", null);
        feedFile = new File(downloadFragment.getContext().getFilesDir(), "gapps_feed.xml");
        urlString = downloadFragment.getString(R.string.feed_url).replace("%arch", architecture);
        setLastFile();
    }

    private void setLastFile() {
        SharedPreferences prefs = downloadFragment.getContext().getSharedPreferences(downloadFragment.getString(R.string.pref_name), MODE_PRIVATE);
        String path = prefs.getString("download_dir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        String title = "OpenGApps-" + architecture + "-" + android + "-" + variant;
        File f = new File(path, title + ".zip");
        if (f.exists())
            lastFile = f;
    }


    @Override
    protected Long doInBackground(Void... voids) {
        refreshFeed();
        tag = parseFeed();
        Uri uri = generateUri();
        return doDownload(uri);
    }

    @Override
    protected void onPostExecute(Long id) {
        Toast.makeText(downloadFragment.getContext(), downloadFragment.getString(R.string.download_started), Toast.LENGTH_SHORT).show();
        DownloadProgressView progress = (DownloadProgressView) downloadFragment.getView().findViewById(R.id.progressView);
        progress.show(id, downloadFragment);
        downloadFragment.downloadStarted(id, tag);
        SharedPreferences prefs = downloadFragment.getContext().getSharedPreferences(downloadFragment.getString(R.string.pref_name), MODE_PRIVATE);
        prefs.edit().putBoolean("checkMissing", true).apply();
    }

    public String getTag() {
        return tag;
    }

    public void deleteLastFile() {
        if (lastFile != null)
            //noinspection ResultOfMethodCallIgnored
            lastFile.delete();
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
            downloadFragment.OnTagUpdated();
        }
    }

    private void logSelections() {
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(downloadFragment.getContext());
        SharedPreferences preferences = downloadFragment.getContext().getSharedPreferences(downloadFragment.getString(R.string.pref_name), MODE_PRIVATE);
        for (String entry : new String[]{"selection_arch", "selection_android", "selection_variant"}) {
            Bundle bundle = new Bundle(2);
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, entry);
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, preferences.getString(entry, "null"));
            analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        }
    }

    private Uri generateUri() {
        String url = downloadFragment.getString(R.string.download_url);
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
            xpp.setInput(new FileReader(new File(downloadFragment.getContext().getFilesDir(), "gapps_feed.xml")));
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
        SharedPreferences prefs = downloadFragment.getContext().getSharedPreferences(downloadFragment.getString(R.string.pref_name), MODE_PRIVATE);
        if (lastFile != null) {
            //noinspection ResultOfMethodCallIgnored
            lastFile.delete();
        }
        downloadMd5(uri.toString());
        DownloadManager.Request request = new DownloadManager.Request(uri);
        String title = "OpenGApps-" + architecture + "-" + android + "-" + variant;
        request.setTitle(title);
        if (prefs.getBoolean("download_wifi_only", true))
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        String path = prefs.getString("download_dir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        File f = new File(new File(path), title + ".zip");
        lastFile = f;
        //noinspection ResultOfMethodCallIgnored
        f.delete();
        request.setDestinationUri(Uri.fromFile(f));
        DownloadManager downloadManager = (DownloadManager) downloadFragment.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        return downloadManager.enqueue(request);
    }

    private void downloadMd5(String uri) {
        uri += ".md5";
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(uri)
                    .build();
            Response response = client.newCall(request).execute();
            File feedFile = new File(downloadFragment.getContext().getFilesDir(), "gapps.md5");
            FileWriter fileWriter = new FileWriter(feedFile, false);
            fileWriter.write(response.body().string());
            fileWriter.close();
        } catch (Exception ignored) {

        }
    }

    public boolean fileExists() {
        if (lastFile != null) {
            if (lastFile.exists())
                return true;
        }
        return false;
    }

    private static String convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (byte md5Byte : md5Bytes) {
            returnVal += Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toUpperCase();
    }

    public static String getDownloadedFile(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.pref_name), MODE_PRIVATE);
        String architecture = prefs.getString("selection_arch", null);
        String android = prefs.getString("selection_android", null);
        String variant = prefs.getString("selection_variant", null);
        String path = prefs.getString("download_dir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        return path + "/" + "OpenGApps-" + architecture + "-" + android + "-" + variant + ".zip";
    }
}
