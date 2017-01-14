package org.opengapps.app.utils;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import org.opengapps.app.R;

public class DialogUtil {

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
