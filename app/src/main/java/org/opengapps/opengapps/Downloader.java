package org.opengapps.opengapps;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

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

public class Downloader extends AsyncTask<Void, Void, Void> {
    private final MainActivity mainActivity;
    private String architecture, android, variant, tag;

    public Downloader( MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        SharedPreferences prefs = mainActivity.getSharedPreferences(mainActivity.getResources().getString(R.string.pref_name), Context.MODE_PRIVATE);
        this.architecture = prefs.getString("selection_arch", null);
        this.android = prefs.getString("selection_android", null);
        this.variant = prefs.getString("selection_variant", null);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        refreshFeed();
        tag = parseFeed();
        Uri uri = generateUri();
        doDownload(uri);
        return null;
    }

    public String getTag() {
        return tag;
    }

    public void deleteLastFile() {

    }

    public class TagUpdater extends AsyncTask<Void, Void, String> {
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
        String url = mainActivity.getResources().getString(R.string.download_url);
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
                    .url(mainActivity.getResources().getString(R.string.feed_url).replace("%arch", architecture))
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

    private void doDownload(Uri uri) {
        DownloadManager.Request request = new DownloadManager.Request(uri);
        String title = "OpenGApps-" + architecture + "-" + android + "-" + variant;
        request.setTitle(title+"-zip");
        new File(title+".zip").delete();
        request.setDestinationUri(Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), title)));
        DownloadManager downloadManager = (DownloadManager) mainActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        long id = downloadManager.enqueue(request);
        registerReceiver(id, tag);
    }

    private void onDownloadComplete() {
        SharedPreferences prefs = mainActivity.getSharedPreferences(mainActivity.getResources().getString(R.string.pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_downloaded_tag", tag);
        editor.apply();
        mainActivity.onDownloadComplete(tag);
    }

    private void registerReceiver(final long id, final String currentTag) {
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == id) {
                    onDownloadComplete();
                    context.unregisterReceiver(this);
                }
            }
        };
        mainActivity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
}
