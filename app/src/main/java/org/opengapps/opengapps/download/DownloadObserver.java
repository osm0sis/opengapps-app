package org.opengapps.opengapps.download;

import android.os.FileObserver;

import org.opengapps.opengapps.DownloadFragment;

public class DownloadObserver extends FileObserver{
    private DownloadFragment downloadFragment;

    public DownloadObserver(DownloadFragment downloadFragment, String path) {
        super(path);
        this.downloadFragment = downloadFragment;
    }

    @Override
    public void onEvent(int i, String s) {
//        new FileValidator(downloadFragment).execute(s);
//        stopWatching();
    }
}
