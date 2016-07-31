package org.opengapps.opengapps;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import eu.chainfire.libsuperuser.Shell;

class ZipInstaller {
    private final Context context;
    private final SharedPreferences prefs;

    ZipInstaller(Context context){
        this.context = context;
        prefs = context.getSharedPreferences(context.getString(R.string.pref_name), Context.MODE_PRIVATE);
    }

    void installZip(){
        if(Shell.SU.available()){
            try {
                File f = new File(context.getFilesDir(), "openrecoveryscript");
                FileWriter fileWriter = new FileWriter(f, false);
                fileWriter.append("install ").append(Downloader.getDownloadedFile(context));
                fileWriter.close();
                String command = "cp "+ f.getAbsolutePath() + " /cache/recovery/openrecoveryscript";
                Shell.SU.run(command);
                Shell.SU.run("reboot recovery");
            } catch (IOException ignored) {
            }
        }
    }
}
