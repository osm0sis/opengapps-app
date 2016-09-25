package org.opengapps.opengapps.intro;

import android.content.Context;

import org.opengapps.opengapps.R;
import org.opengapps.opengapps.SelectionValidator;

public class slideAndroidSelectorFragment extends GappsSelectionFragment {

    public slideAndroidSelectorFragment() {
        super(R.string.label_android, R.string.slide_android_description, "selection_android", R.array.android_versions);
    }

    @Override
    protected boolean isValid(String selection) {
        String arch = GappsSelectionFragment.selectionArch;
        return SelectionValidator.isValidArchAnd(arch, selection);
    }

    @Override
    public String getSelection() {
        return GappsSelectionFragment.selectionAnd;
    }

    @Override
    public void setSelection(String selection) {
        selection = selection.replace(" <i>(detected)</i>", "");
        GappsSelectionFragment.selectionAnd = selection;
    }

    @Override
    public String getGuessedSelection(Context context) {
        return PackageGuesser.getAndroidVersion(context);
    }
}
