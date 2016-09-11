package org.opengapps.opengapps;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class InstallCard extends CardView implements PopupMenu.OnMenuItemClickListener {
    private File gappsFile;
    private File md5File;
    private File versionLogFile;
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

        ImageButton menuButton = (ImageButton) findViewById(R.id.menuButton);
        menuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });


        Button installButton = (Button) findViewById(R.id.install_button);
        if (!ZipInstaller.canReboot(getContext())) {
            installButton.setTextColor(Color.parseColor("#757575"));
            installButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getContext(), getResources().getString(R.string.autoinstall_root_disclaimer), Toast.LENGTH_SHORT).show();
                }
            });
        } else
            installButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    new ZipInstaller(getContext()).installZip(gappsFile);
                }
            });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void removeFiles() {
        gappsFile.delete();
        md5File.delete();
        versionLogFile.delete();
    }

    public void setFile(File file) {
        gappsFile = file;
        md5File = new File(gappsFile.getAbsolutePath() + ".md5");
        String versionLog = gappsFile.getAbsolutePath().substring(0, gappsFile.getAbsolutePath().length() - 4) + ".versionlog.txt";
        versionLogFile = new File(versionLog);
        TextView fileName = (TextView) findViewById(R.id.newest_version);
        fileName.setText(file.getName());
    }

    private void showPopup(View view) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        MenuInflater inflater = popup.getMenuInflater();
        popup.setOnMenuItemClickListener(this);
        inflater.inflate(R.menu.install_card_menu, popup.getMenu());
        popup.getMenu().findItem(R.id.menu_show_md5).setVisible(false);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent sendIntent = new Intent();
        Uri uri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", gappsFile);
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setType("application/zip");
        getContext().startActivity(sendIntent);
        return false;
    }
}
