package org.opengapps.app.card;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
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

import com.tooltip.Tooltip;

import org.opengapps.app.BuildConfig;
import org.opengapps.app.DownloadFragment;
import org.opengapps.app.R;
import org.opengapps.app.ZipInstaller;
import org.opengapps.app.download.FileValidator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

public class InstallCard extends CardView implements PopupMenu.OnMenuItemClickListener {
    public static boolean invalidate = false;
    private File gappsFile;
    private File md5File;
    private File versionLogFile;
    private boolean checked = false;
    private boolean md5Exists;
    private boolean versionLogExists;
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
        initDeleteButton();
        initMenuButton();
        initInstallButton();
        initMd5Button();
    }

    private void initMd5Button() {
        View success = findViewById(R.id.md5_success);
        success.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new Tooltip.Builder(v)
                        .setText(R.string.label_checksum_valid)
                        .setCancelable(true)
                        .setTextColor(Color.parseColor("#ffffff"))
                        .setDismissOnClick(true)
                        .show();
            }
        });

        View failure = findViewById(R.id.md5_failure);
        failure.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new Tooltip.Builder(v)
                        .setText(R.string.label_checksum_invalid)
                        .setCancelable(true)
                        .setTextColor(Color.parseColor("#ffffff"))
                        .setDismissOnClick(true)
                        .show();
            }
        });
    }

    private void initInstallButton() {
        Button installButton = (Button) findViewById(R.id.install_button);
        if (!ZipInstaller.canReboot(getContext())) {
            installButton.setTextColor(Color.parseColor("#757575"));
            installButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getContext(), getResources().getString(R.string.autoinstall_root_disclaimer), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            installButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    new ZipInstaller(getContext()).installZip(gappsFile);
                }
            });
        }
    }

    private void initMenuButton() {
        ImageButton menuButton = (ImageButton) findViewById(R.id.menu_button);
        menuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });
    }

    private void initDeleteButton() {
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
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void removeFiles() {
        gappsFile.delete();
        md5File.delete();
        versionLogFile.delete();
    }

    public void setFile(File file) {
        gappsFile = file;
        md5File = new File(gappsFile.getAbsolutePath() + DownloadFragment.md5FileExtension);
        String versionLog = gappsFile.getAbsolutePath().substring(0, gappsFile.getAbsolutePath().length() - ".zip".length()) + DownloadFragment.versionlogFileExtension;
        versionLogFile = new File(versionLog);
        md5Exists = md5File.exists();
        versionLogExists = versionLogFile.exists();
        TextView fileName = (TextView) findViewById(R.id.newest_version);
        fileName.setText(file.getName());
        checkMD5();
    }

    private void showPopup(View view) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        MenuInflater inflater = popup.getMenuInflater();
        popup.setOnMenuItemClickListener(this);
        inflater.inflate(R.menu.install_card_menu, popup.getMenu());
        if (!md5Exists) {
            popup.getMenu().findItem(R.id.menu_show_md5).setVisible(false);
        }
        if (!versionLogExists) {
            popup.getMenu().findItem(R.id.menu_show_versionlog).setVisible(false);
        }
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_share_file) {
            shareGappsFile();
            return true;
        } else if (itemId == R.id.menu_show_md5) {
            showMD5();
        } else if (itemId == R.id.menu_show_versionlog) {
            showVersionlog();
        }
        return false;
    }

    private void showVersionlog() {
        String content;
        StringBuilder lines = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(versionLogFile));
            String line;
            while ((line = br.readLine()) != null) {
                lines.append(line);
                lines.append('\n');
            }
            br.close();
            content = lines.toString();
        } catch (java.io.IOException e) {
            content = getResources().getString(R.string.file_not_found);
        }
        //Passing null to the inflater is allowed when using AlertDialogs
        @SuppressLint("InflateParams") View view = LayoutInflater.from(getContext()).inflate(R.layout.versionlog_dialog, null);
        TextView text = (TextView) view.findViewById(R.id.versionlog_text);
        text.setText(content);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.label_versionlog)
                .setView(view)
                .setPositiveButton(R.string.label_close, null)
                .show();
    }

    private void showMD5() {
        final String md5sum = FileValidator.getMD5(md5File).toUpperCase(Locale.getDefault());
        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.label_md5_checksum)
                .setMessage(md5sum)
                .setNeutralButton(R.string.label_copy_checksum, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("MD5-Sum", md5sum);
                        clipboard.setPrimaryClip(clip);
                    }
                })
                .setPositiveButton(R.string.label_close, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(Color.parseColor("#000000"));
            }
        });
        dialog.show();
    }

    private void shareGappsFile() {
        Intent sendIntent = new Intent();
        Uri uri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", gappsFile);
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setType("application/zip");
        getContext().startActivity(sendIntent);
    }

    public void checkMD5() {
        if (checked) {
            return;
        }
        checked = true;
        if (md5File.exists()) {
            findViewById(R.id.md5_progress).setVisibility(VISIBLE);
            new FileValidator(this).execute(gappsFile.getAbsolutePath(), md5File.getAbsolutePath());
        }
    }

    public void hashSuccess(Boolean matches) {
        findViewById(R.id.md5_progress).setVisibility(INVISIBLE);
        if (matches) {
            findViewById(R.id.md5_success).setVisibility(VISIBLE);
        } else {
            findViewById(R.id.md5_failure).setVisibility(VISIBLE);
        }
        if (deleteListener != null) {
            deleteListener.hashSuccess(matches);
        }
    }

    public File getGappsFile() {
        return gappsFile;
    }

    public Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public boolean exists() {
        return gappsFile.exists();
    }

}
