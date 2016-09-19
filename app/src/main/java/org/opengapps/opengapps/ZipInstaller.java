package org.opengapps.opengapps;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.PowerManager;
import android.widget.Toast;

import org.opengapps.opengapps.download.Downloader;
import org.opengapps.opengapps.prefs.Preferences;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import eu.chainfire.libsuperuser.Shell;

import static android.content.Context.POWER_SERVICE;

public class ZipInstaller {
    private final Context context;
    private final SharedPreferences prefs;

    public ZipInstaller(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
    }

    public void installZip(File file) {
        if (Shell.SU.available()) {
            try {
                File f = new File(context.getFilesDir(), "openrecoveryscript");
                FileWriter fileWriter = new FileWriter(f, false);
                if (prefs.getBoolean("wipe_cache", false))
                    fileWriter.append("\nwipe cache");
                fileWriter.append("\ninstall ").append(file.getAbsolutePath());
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

    void installZip() {
        installZip(new File(Downloader.getDownloadedFile(context)));
    }

    public static boolean canReboot(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
            int result;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                result = info.flags & PermissionInfo.PROTECTION_FLAG_PRIVILEGED;
            } else {
                //noinspection deprecation
                result = info.flags & PermissionInfo.PROTECTION_FLAG_SYSTEM;
            }
            return result != 0 || Shell.SU.available();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
