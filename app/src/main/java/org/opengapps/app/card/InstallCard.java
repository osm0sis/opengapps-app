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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tooltip.Tooltip;

import org.opengapps.app.BuildConfig;
import org.opengapps.app.DownloadFragment;
import org.opengapps.app.R;
import org.opengapps.app.ZipInstaller;
import org.opengapps.app.download.FileValidator;
import org.opengapps.app.prefs.Preferences;

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

    /**
     * Listener that is called on delete of the file is set here. (Usually the DownloadFragment containing the view
     *
     * @param listener DownloadFragment that gets notified when a gappsPackage is deleted
     */
    public void setDeleteListener(DownloadFragment listener) {
        deleteListener = listener;
    }

    /**
     * ContainerMethod for initializing all needed buttons and defining the onClick-Behaviour
     */
    private void initButtons() {
        initDeleteButton();
        initMenuButton();
        initInstallButton();
        initMd5Button();
    }

    /**
     * Initializes the checkmark and the cross to show the corresponding tooltip
     */
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

    /**
     * Initializes the InstallButton and sets onClick-Behaviour.
     * If root is granted and rootmode is active, the button turns red and onClick makes it install the ZIP
     * Otherwise, button is gray and clicking only shows a toast.
     */
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
                    boolean showInstallWarning = getContext().getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE).getBoolean("show_install_warning", true);
                    if (showInstallWarning)
                        new AlertDialog.Builder(getContext())
                                .setTitle(R.string.pref_header_install)
                                .setView(new showAgainDiag(getContext()))
                                .setPositiveButton(R.string.label_install, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        new ZipInstaller(getContext()).installZip(gappsFile);
                                    }
                                })
                                .setNegativeButton(R.string.cancel_label, null)
                                .show();
                    else
                        new ZipInstaller(getContext()).installZip(gappsFile);
                }
            });
        }
    }

    /**
     * Sets the 3dot-menu-button to show the menu on click
     */
    private void initMenuButton() {
        ImageButton menuButton = (ImageButton) findViewById(R.id.menu_button);
        menuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });
    }

    /**
     * Initializes the deletebutton and sets onClick to Delete all the files associated with the package as well as notify the deleteListener
     */
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

    /**
     * Removes Package, Versionlogfile as well as MD5-File if they exist.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void removeFiles() {
        gappsFile.delete();
        md5File.delete();
        versionLogFile.delete();
    }

    /**
     * Associates a File with the installCard. checks if md5/versionlog exists, sets filename and tries to check MD5 if possible
     *
     * @param file GApps-Package as File. ZIP-File, not md5/versionlog
     */
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

    /**
     * Shows the options of the 3dot-menu. Hides "show MD5" and "show Versionlog" if necessary
     *
     * @param view Root-View that has to contain the popup
     */
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


    private class showAgainDiag extends LinearLayout {
        public showAgainDiag(Context context) {
            super(context);
            setOrientation(VERTICAL);
            final LayoutParams params = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            final int margin = DownloadFragment.dpToPx(getContext(), 16);
            params.setMargins(margin, margin, margin, 0);
            TextView textView = new TextView(getContext());
            textView.setTextSize(DownloadFragment.spToPx(getContext(), 6));
            textView.setText(R.string.explanation_install_warning);
            addView(textView, params);
            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(R.string.dont_show_again);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    getContext().getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE).edit().putBoolean("show_install_warning", !b).apply();
                }
            });
            addView(checkBox, params);
        }


        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();

        }
    }

}
