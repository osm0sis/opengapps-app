package org.opengapps.app.utils;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import org.opengapps.app.NavigationActivity;
import org.opengapps.app.R;
import org.opengapps.app.prefs.Preferences;

public class DialogUtil {

    public static Dialog showRatingDialog(final Context context) {
        SharedPreferences preferences = context.getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();

        return new AlertDialog.Builder(context)
                .setTitle(R.string.title_rate_us)
                .setMessage(R.string.message_rate_us)
                .setPositiveButton(R.string.label_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        NavigationActivity.openURL(context, context.getString(R.string.url_ogapps_market));
                        editor.putBoolean("rate_done", true);
                        editor.apply();
                    }
                })
                .setNegativeButton(R.string.label_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        editor.putInt("rate_count", 5);
                        editor.putBoolean("rate_done",false);
                        editor.apply();
                    }
                })
                .setNeutralButton(R.string.label_never, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editor.putBoolean("rate_done", true);
                        editor.apply();
                    }
                })
                .create();
    }

    public static Dialog showAlertWithMessage(Context context, String title,String message) {
        return new android.support.v7.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.label_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).create();
    }
}
