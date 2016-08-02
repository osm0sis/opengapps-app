package org.opengapps.opengapps.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Environment;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

public class FileChooserPref extends Preference implements DialogSelectionListener {
    private FilePickerDialog dialog;

    public FileChooserPref(Context context, AttributeSet attrs) {
        super(context, attrs);
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.extensions = null;
        properties.root = Environment.getExternalStorageDirectory();
        dialog = new FilePickerDialog(getContext(), properties);
        dialog.setDialogSelectionListener(this);
    }

    @Override
    protected void onClick() {
        dialog.show();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    public void onSelectedFilePaths(String[] files) {
        if (files.length == 0) {
            Toast.makeText(getContext(), "You have to tick the checkbox for the directory", Toast.LENGTH_SHORT).show();
        } else {
            persistString(files[0]);
        }
    }
}
