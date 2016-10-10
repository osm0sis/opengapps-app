package org.opengapps.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import org.opengapps.app.prefs.Preferences;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AppUpdater extends AsyncTask<Context, Void, AppUpdater.UpdateStatus> {
    private final static String versionInfoUrl = "http://opengapps.org/app/version.txt";

    private OkHttpClient client;
    private Context context;

    @Override
    protected void onPreExecute() {
        client = new OkHttpClient();
    }

    @Override
    protected UpdateStatus doInBackground(Context... contexts) {
        context = contexts[0];
        Request request = new Request.Builder()
                .url(versionInfoUrl)
                .build();
        try {
            Response response = client.newCall(request).execute();
            int[] result = parseVersionFile(response.body().string());
            if (result[1] != -1) {
                long currentTime = System.currentTimeMillis() / 1000L;
                SharedPreferences preferences = context.getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
                preferences.edit().putLong("checkAgain", currentTime + result[1]).apply();
            }
            if (result[2] > BuildConfig.VERSION_CODE) {
                return UpdateStatus.forced;
            } else if (result[0] > BuildConfig.VERSION_CODE) {
                return UpdateStatus.optional;
            } else {
                return UpdateStatus.none;
            }
        } catch (IOException e) {
            return UpdateStatus.none;
        }
    }

    @Override
    protected void onPostExecute(UpdateStatus updateAvailable) {
        if (updateAvailable == UpdateStatus.optional) {
            if (!isAppInstalled("com.android.vending")) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.title_optional_app_update)
                        .setMessage(R.string.explanation_optional_app_update)
                        .setPositiveButton(R.string.label_update, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openUpdateSite();
                            }
                        })
                        .setNegativeButton(R.string.cancel_label, null)
                        .show();
            }
        } else if (updateAvailable == UpdateStatus.forced) {
            NavigationActivity.forcedUpdate = true;
            Toast.makeText(context, R.string.explanation_forced_update, Toast.LENGTH_LONG).show();
            openUpdateSite();
        }
    }

    private void openUpdateSite() {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)));
        } catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.url_download_update))));
        }
    }


    private boolean isAppInstalled(@SuppressWarnings("SameParameterValue") String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean checkAllowed(SharedPreferences preferences) {
        return preferences.getLong("checkAgain", 0) < (System.currentTimeMillis() / 1000L);
    }

    private static int[] parseVersionFile(String fileContent) {
        int[] result = new int[]{-1, -1, -1};
        String[] splittedString = fileContent.split("[\\r\\n]+");

        for (String line : splittedString) {
            if (line.startsWith("version=")) {
                result[0] = Integer.parseInt(line.substring(line.indexOf('=') + 1));
            } else if (line.startsWith("checkAgain=")) {
                result[1] = Integer.parseInt(line.substring(line.indexOf('=') + 1));
            } else if (line.startsWith("minVersion=")) {
                result[2] = Integer.parseInt(line.substring(line.indexOf('=') + 1));
            }
        }
        return result;
    }


    public enum UpdateStatus {
        none, optional, forced
    }
}
