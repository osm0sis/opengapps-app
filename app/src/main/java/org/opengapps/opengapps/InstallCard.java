package org.opengapps.opengapps;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

public class InstallCard extends CardView {
    private File gappsFile;
    private DownloadFragment deleteListener;

    public InstallCard(Context context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.install_card, this, true);
        initButtons();
    }

    public InstallCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.install_card, this, true);
        initButtons();
    }

    public void setDeleteListener(DownloadFragment listener) {
        deleteListener = listener;
    }

    private void initButtons() {
        Button deleteButton = (Button) findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gappsFile != null) {
                    removeFiles();
                    deleteListener.onDeleteFile(gappsFile);
                }
            }
        });

        Button installButton = (Button) findViewById(R.id.install_button);
        if (!ZipInstaller.canReboot(getContext())) {
            installButton.setEnabled(false);
            installButton.setTextColor(Color.parseColor("#757575"));
        }
        installButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new ZipInstaller(getContext()).installZip(gappsFile);
            }
        });
    }

    private void removeFiles() {
        String substring = gappsFile.getAbsolutePath().substring(0, gappsFile.getAbsolutePath().length() - 4) + ".versionlog.txt";
        gappsFile.delete();
        new File(gappsFile.getAbsolutePath() + ".md5").delete();
        new File(substring).delete();
    }

    public void setFile(File file) {
        gappsFile = file;
        TextView fileName = (TextView) findViewById(R.id.newest_version);
        fileName.setText(file.getName());
    }
}
