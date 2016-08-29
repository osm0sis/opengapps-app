package org.opengapps.opengapps.download;

import android.os.AsyncTask;

import org.opengapps.opengapps.DownloadFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Scanner;

public class FileValidator extends AsyncTask<String , Void, Boolean>{
    private final DownloadFragment downloadFragment;

    public FileValidator(DownloadFragment downloadFragment){
        this.downloadFragment = downloadFragment;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            downloadFragment.getContext().getFilesDir();
            File f = new File(downloadFragment.getContext().getFilesDir(), "gapps.md5");
            Scanner scanner = new Scanner(f);
            String expectedHash = scanner.nextLine();
            expectedHash = expectedHash.substring(0, expectedHash.indexOf(" "));
            String actualHash = fileToMD5(params[0]);
            return expectedHash.equalsIgnoreCase(actualHash);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        downloadFragment.hashSuccess(aBoolean);
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
                if (numRead > 0)
                    digest.update(buffer, 0, numRead);
            }
            byte [] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) { }
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