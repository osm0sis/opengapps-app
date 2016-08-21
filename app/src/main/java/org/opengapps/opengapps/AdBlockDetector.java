package org.opengapps.opengapps;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class AdBlockDetector {
    private AdBlockDetector() {
        //NoOp
    }

    public static boolean hasAdBlockEnabled(Context context) {
        return checkHostsFile() || checkForPackages(context);
    }

    private static boolean checkForPackages(Context context) {
        String[] adBlockers = context.getResources().getStringArray(R.array.adBlockers);
        for (String blocker : adBlockers) {
            if (checkForPackage(context, blocker))
                return true;
        }
        return false;
    }

    private static boolean checkForPackage(Context context, String androidPackage) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(androidPackage, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static boolean checkHostsFile() {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/etc/hosts")));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.contains("admob")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
        return false;
    }
}
