package org.opengapps.opengapps.intro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.opengapps.opengapps.R;
import org.opengapps.opengapps.prefs.Preferences;

@SuppressWarnings("WeakerAccess")
class PackageGuesser {
    private PackageGuesser() {
    }

    public static String getArch(Context context) {
        String arch;
        boolean x64;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            arch = Build.SUPPORTED_32_BIT_ABIS[0];
            x64 = Build.SUPPORTED_64_BIT_ABIS.length > 0;
        } else {
            //noinspection deprecation
            arch = Build.CPU_ABI;
            x64 = false;
        }
        String[] architectures = context.getResources().getStringArray(R.array.architectures);
        if (arch.contains("arm")) {
            if (!x64) {
                return architectures[0];//arm
            } else {
                return architectures[1]; //arm64
            }
        } else if (arch.contains("86")) {
            if (!x64)
                return architectures[2]; //x86
            else
                return architectures[3]; //x86x64
        } else
            return architectures[0]; //Default to arm
    }

    public static String getAndroidVersion(Context context) {
        String[] androidVersion = context.getResources().getStringArray(R.array.android_versions);
        switch (Build.VERSION.SDK_INT) {
            case 19:
                return androidVersion[4];//KitKat-Device
            case 21:
                return androidVersion[3];//Lollipop-5.0-Device
            case 22:
                return androidVersion[2];//Lollipop-5.1-Device
            case 23:
                return androidVersion[1];//Marshmallow-Device
            case 24:
                return androidVersion[0];//Nougat-Device
            default:
                return androidVersion[0]; //Default to latest
        }
    }

    public static String getVariant(Context context) {
        return "stock";
    }

    private SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
    }
}
