package org.opengapps.app.intro;

import android.content.Context;

import org.opengapps.app.R;
import org.opengapps.app.SelectionValidator;

public class slideAndroidSelectorFragment extends GappsSelectionFragment {

    public slideAndroidSelectorFragment() {
        super(R.string.appintro_label_android, R.string.slide_android_description, R.string.slide_android_hint, R.string.android_moreinfo, "selection_android", R.array.android_versions);
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
        selection = selection.replace(" <i>(" + getString(R.string.detected) + ")</i>", "");
        GappsSelectionFragment.selectionAnd = selection;
    }

    @Override
    public String getGuessedSelection(Context context) {
        return PackageGuesser.getAndroidVersion(context);
    }
}
