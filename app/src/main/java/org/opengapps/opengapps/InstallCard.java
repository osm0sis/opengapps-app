package org.opengapps.opengapps;

import android.content.Context;
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

    public InstallCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.install_card, this, true);
        initButtons();
    }

    public void setDeleteListener(DownloadFragment listener){
        deleteListener = listener;
    }

    private void initButtons() {
        Button deleteButton = (Button) findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gappsFile != null)
                {
                    gappsFile.delete();
                    deleteListener.onDeleteFile();
                }
            }
        });

        Button installButton = (Button) findViewById(R.id.install_button);
        installButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new ZipInstaller(getContext()).installZip(gappsFile);
            }
        });
    }

    public void setFile(File file) {
        gappsFile = file;
        TextView fileName = (TextView) findViewById(R.id.newest_version);
        fileName.setText(file.getName());
    }
}
