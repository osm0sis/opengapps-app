package org.opengapps.opengapps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class AdBlockDetector {
    private AdBlockDetector() {
        //NoOp
    }

    public static boolean hasAdBlockEnabled(Context context) {
        parseXml(context);
        return checkHostsFile() || checkForPackages(context);
    }

    private static String[] parseXml(Context context) {
        ArrayList<String> content = new ArrayList<>();
        try {
            final XmlPullParser parser = Xml.newPullParser();
            boolean rightTag = false;
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(context.getResources().openRawResource(R.raw.evil_apps), null);
            parser.nextTag();

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("app")) {
                    rightTag = true;
                } else if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals("app")) {
                    {
                        rightTag = false;
                    }
                } else if (parser.getEventType() == XmlPullParser.TEXT && rightTag) {
                    content.add(parser.getText());
                }
            }
        } catch (Exception e) {
            return null;
        }
        return content.toArray(new String[0]);
    }

    private static boolean checkForPackages(Context context) {
        String[] adBlockers = parseXml(context);
        for (String blocker : adBlockers != null ? adBlockers : new String[0]) {
            if (checkForPackage(context, blocker)) {
                return true;
            }
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
