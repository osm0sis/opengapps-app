package org.opengapps.opengapps;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;


import org.opengapps.opengapps.DownloadProgress.DownloadProgressView;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class Downloader extends AsyncTask<Void, Void, Long> {
    private final MainActivity mainActivity;
    private String architecture, android, variant, tag;
    private static File lastFile;

    Downloader(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        SharedPreferences prefs = mainActivity.getSharedPreferences(mainActivity.getString(R.string.pref_name), Context.MODE_PRIVATE);
        this.architecture = prefs.getString("selection_arch", null);
        this.android = prefs.getString("selection_android", null);
        this.variant = prefs.getString("selection_variant", null);
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
        DownloadProgressView progress = (DownloadProgressView) mainActivity.findViewById(R.id.progressView);
        progress.show(id, mainActivity);
        mainActivity.downloadStarted(id, tag);
    }

    String getTag() {
        return tag;
    }

    void deleteLastFile() {
        if (lastFile != null)
            //noinspection ResultOfMethodCallIgnored
            lastFile.delete();
    }

    class TagUpdater extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            refreshFeed();
            return parseFeed();
        }

        @Override
        protected void onPostExecute(String s) {
            tag = s;
            mainActivity.OnTagUpdated();
        }
    }

    private Uri generateUri() {
        String url = mainActivity.getString(R.string.download_url);
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
            xpp.setInput(new FileReader(new File(mainActivity.getFilesDir(), "gapps_feed.xml")));
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
                    .url(mainActivity.getString(R.string.feed_url).replace("%arch", architecture))
                    .build();

            Response response = client.newCall(request).execute();

            File feedFile = new File(mainActivity.getFilesDir(), "gapps_feed.xml");
            FileWriter fileWriter = new FileWriter(feedFile, false);
            fileWriter.write(response.body().string());
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long doDownload(Uri uri) {
        SharedPreferences prefs = mainActivity.getSharedPreferences(mainActivity.getString(R.string.pref_name), Context.MODE_PRIVATE);
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
        DownloadManager downloadManager = (DownloadManager) mainActivity.getSystemService(Context.DOWNLOAD_SERVICE);
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
            File feedFile = new File(mainActivity.getFilesDir(), "gapps.md5");
            FileWriter fileWriter = new FileWriter(feedFile, false);
            fileWriter.write(response.body().string());
            fileWriter.close();
        } catch (Exception ignored) {

        }
    }

    private static String convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (byte md5Byte : md5Bytes) {
            returnVal += Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toUpperCase();
    }

    static String getDownloadedFile(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.pref_name), Context.MODE_PRIVATE);
        String architecture = prefs.getString("selection_arch", null);
        String android = prefs.getString("selection_android", null);
        String variant = prefs.getString("selection_variant", null);
        String path = prefs.getString("download_dir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        return path + "/" + "OpenGApps-" + architecture + "-" + android + "-" + variant + ".zip";
    }
}
