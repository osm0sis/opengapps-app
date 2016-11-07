package org.opengapps.app.intro;

import android.content.Context;
import android.os.Build;

import org.opengapps.app.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class PackageGuesser {
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
            if (!x64) {
                return architectures[2]; //x86
            } else {
                return architectures[3]; //x86x64
            }
        } else {
            return architectures[0]; //Default to arm
        }
    }

    public static String getAndroidVersion(Context context) {
        String[] androidVersion = context.getResources().getStringArray(R.array.android_versions);
        switch (Build.VERSION.SDK_INT) {
            case 19:
                return androidVersion[5];//KitKat-Device
            case 21:
                return androidVersion[4];//Lollipop-5.0-Device
            case 22:
                return androidVersion[3];//Lollipop-5.1-Device
            case 23:
                return androidVersion[2];//Marshmallow-Device
            case 24:
                return androidVersion[1];//Nougat-Device
            case 25:
                return androidVersion[0];//Nougat-MR1-Device (hello pixel)
            default:
                return androidVersion[1]; //Default to nougat
        }
    }

    public static String getVariant() {
        File propFile = new File("/system/etc/g.prop");
        try {
            FileReader reader = new FileReader(propFile);
            BufferedReader bufferedReader = new BufferedReader(reader);
            Pattern pattern = Pattern.compile("ro\\.addon\\.open_type=.*");
            String line = "";
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null) {
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

    public static String getCurrentlyInstalled() {
        File propFile = new File("/system/etc/g.prop");
        try {
            FileReader reader = new FileReader(propFile);
            BufferedReader bufferedReader = new BufferedReader(reader);
            Pattern pattern = Pattern.compile("ro\\.addon\\.open_version=.*");
            String line = "";
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null) {
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
