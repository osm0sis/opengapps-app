package org.opengapps.opengapps;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import eu.chainfire.libsuperuser.Shell;

import static android.content.Context.POWER_SERVICE;

class ZipInstaller {
    private final Context context;
    private final SharedPreferences prefs;

    ZipInstaller(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(context.getString(R.string.pref_name), Context.MODE_PRIVATE);
    }

    void installZip() {
        if (Shell.SU.available()) {
            try {
                File f = new File(context.getFilesDir(), "openrecoveryscript");
                FileWriter fileWriter = new FileWriter(f, false);
                if (prefs.getBoolean("wipe_cache", false))
                    fileWriter.append("\nwipe cache");
                if (prefs.getBoolean("wipe_data", false))
                    fileWriter.append("\nwipe data");
                fileWriter.append("\ninstall ").append(Downloader.getDownloadedFile(context));
                fileWriter.close();
                String command = "cp " + f.getAbsolutePath() + " /cache/recovery/openrecoveryscript";
                Shell.SU.run(command);
                Shell.SU.run("reboot recovery");

            } catch (IOException ignored) {
            }
        } else {
            try {
                PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
                pm.reboot("recovery");
            } catch (Exception e) {
                Toast.makeText(context, context.getString(R.string.autoinstall_root_disclaimer), Toast.LENGTH_LONG).show();
            }
        }
    }
}
