package org.opengapps.app.download;

import android.app.FragmentManager;
import android.os.AsyncTask;

import org.opengapps.app.DownloadFragment;
import org.opengapps.app.card.InstallCard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.security.MessageDigest;

public class FileValidator extends AsyncTask<String, Void, Boolean> {
    private String filePath;
    private FragmentManager fragmentManager;

    public FileValidator(InstallCard installCard) {
        fragmentManager = installCard.getActivity().getFragmentManager();
        filePath = installCard.getGappsFile().getAbsolutePath();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        File f = new File(params[1]);
        String expectedHash = getMD5(f);
        String actualHash = fileToMD5(params[0]);
        return expectedHash.equalsIgnoreCase(actualHash);
    }

    public static String getMD5(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String expectedHash = reader.readLine();
            if(expectedHash==null)
                throw new Exception();
            expectedHash = expectedHash.substring(0, expectedHash.indexOf(" "));
            return expectedHash;
        } catch (Exception e) {
            return "FILE NOT FOUND";
        }
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        DownloadFragment fragment = (DownloadFragment) fragmentManager.findFragmentByTag(DownloadFragment.TAG);
        if (fragment != null && fragment.isAdded()) {
            InstallCard card = fragment.getInstallCard(filePath);
            if (card != null && card.isAttachedToWindow()) {
                card.hashSuccess(aBoolean);
            }
        }
    }

    private static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead);
                }
            }
            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (byte md5Byte : md5Bytes) {
            returnVal += Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toUpperCase();
    }
}
