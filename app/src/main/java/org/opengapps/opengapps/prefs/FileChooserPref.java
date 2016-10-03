package org.opengapps.opengapps.prefs;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Environment;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import org.opengapps.opengapps.R;
import org.opengapps.opengapps.download.Downloader;

public class FileChooserPref extends Preference implements DirectoryChooserFragment.OnFragmentInteractionListener {
    private DirectoryChooserFragment dialog;

    public FileChooserPref(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        setSummary(getPersistedString(Downloader.defaultDownloadDir));
    }

    @Override
    protected void onClick() {
        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .initialDirectory(Environment.getExternalStorageDirectory().getAbsolutePath())
                .newDirectoryName(getContext().getString(R.string.new_folder))
                .allowNewDirectoryNameModification(true)
                .allowReadOnlyDirectory(false)
                .build();
        if (dialog == null)
            dialog = DirectoryChooserFragment.newInstance(config);
        dialog.setDirectoryChooserListener(this);
        FragmentManager fragmentManager = ((Activity) getContext()).getFragmentManager();
        dialog.show(fragmentManager, null);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    }

    @Override
    public void onSelectDirectory(@NonNull String path) {
        persistString(path);
        setSummary(path);
        dialog.dismiss();
        dialog = null;
    }

    @Override
    public void onCancelChooser() {
        dialog.dismiss();
        dialog = null;
    }
}
