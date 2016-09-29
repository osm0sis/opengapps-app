package org.opengapps.opengapps.intro;

import android.content.Context;
import android.os.Build;

import org.opengapps.opengapps.R;

import java.io.File;
import java.util.Scanner;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
class PackageGuesser {
    private static String Arch, Android, Variant;

    private PackageGuesser() {
    }

    public static String getArch(Context context) {
        if (Arch != null)
            return Arch;
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
                Arch = architectures[0];
                return architectures[0];//arm
            } else {
                Arch = architectures[1];
                return architectures[1]; //arm64
            }
        } else if (arch.contains("86")) {
            if (!x64) {
                Arch = architectures[2];
                return architectures[2]; //x86
            } else {
                Arch = architectures[3];
                return architectures[3]; //x86x64
            }
        } else {
            Arch = architectures[0];
            return architectures[0]; //Default to arm
        }
    }

    public static String getAndroidVersion(Context context) {
        if (Android != null)
            return Android;
        String[] androidVersion = context.getResources().getStringArray(R.array.android_versions);
        switch (Build.VERSION.SDK_INT) {
            case 19:
                Android = androidVersion[4];
                return androidVersion[4];//KitKat-Device
            case 21:
                Android = androidVersion[3];
                return androidVersion[3];//Lollipop-5.0-Device
            case 22:
                Android = androidVersion[2];
                return androidVersion[2];//Lollipop-5.1-Device
            case 23:
                Android = androidVersion[1];
                return androidVersion[1];//Marshmallow-Device
            case 24:
                Android = androidVersion[0];
                return androidVersion[0];//Nougat-Device
            default:
                Android = androidVersion[0];
                return androidVersion[0]; //Default to latest
        }
    }

    public static String getVariant(Context context) {
        File propFile = new File("/system/etc/g.prop");
        try {
            Scanner scanner = new Scanner(propFile);
            Pattern pattern = Pattern.compile("ro\\.addon\\.open_type=.*");
            String line = "";
            String currentLine;
            while (scanner.hasNext()) {
                currentLine = scanner.nextLine();
                if (pattern.matcher(currentLine).matches()) {
                    line = currentLine;
                    break;
                }
            }
            return line.trim().substring(line.indexOf('=') + 1, line.trim().length());
        } catch (Exception e) {
            return "";
        }
    }
}
