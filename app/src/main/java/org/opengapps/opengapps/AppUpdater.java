package org.opengapps.opengapps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AppUpdater extends AsyncTask<Context, Void, Boolean> {
    private OkHttpClient client;
    private Context context;

    @Override
    protected void onPreExecute() {
        client = new OkHttpClient();
    }

    @Override
    protected Boolean doInBackground(Context... contexts) {
        context = contexts[0];
        Request request = new Request.Builder()
                .url(context.getString(R.string.url_app_version))
                .build();
        try {
            Response response = client.newCall(request).execute();
            int[] result = parseVersionFile(response.body().string());
            if (result[1] != -1) {
                long currentTime = System.currentTimeMillis() / 1000L;
                SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.pref_name), Context.MODE_PRIVATE);
                preferences.edit().putLong("checkAgain", currentTime + result[1]).apply();
            }
            return result[0] > BuildConfig.VERSION_CODE;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean updateAvailable) {
        if (updateAvailable)
            new AlertDialog.Builder(context)
                    .setTitle("App-Update available")
                    .setMessage("YOLO")
                    .setPositiveButton("MANA", null)
                    .show();
    }

    public static boolean checkAllowed(SharedPreferences preferences) {
        return preferences.getLong("checkAgain", 0) < (System.currentTimeMillis() / 1000L);
    }

    private static int[] parseVersionFile(String fileContent) {
        int[] result = new int[]{-1, -1};
        String[] splittedString = fileContent.split("[\\r\\n]+");

        for (String line : splittedString) {
            if (line.startsWith("version="))
                result[0] = Integer.parseInt(line.substring(line.indexOf('=') + 1));
            else if (line.startsWith("checkAgain="))
                result[1] = Integer.parseInt(line.substring(line.indexOf('=') + 1));
        }
        return result;
    }
}
