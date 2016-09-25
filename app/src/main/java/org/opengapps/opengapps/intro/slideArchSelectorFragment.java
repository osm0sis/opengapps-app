package org.opengapps.opengapps.intro;

import android.annotation.SuppressLint;
import android.content.Context;

import org.opengapps.opengapps.R;

@SuppressLint("ValidFragment")
public class slideArchSelectorFragment extends GappsSelectionFragment {


    public slideArchSelectorFragment() {
        super(R.string.label_architecture, R.string.slide_arch_description, "selection_arch", R.array.architectures);
    }

    @Override
    protected boolean isValid(String selection) {
        return true;
    }

    @Override
    public String getSelection() {
        return GappsSelectionFragment.selectionArch;
    }

    @Override
    public void setSelection(String selection) {
        GappsSelectionFragment.selectionArch = selection;
    }

    @Override
    public String getGuessedSelection(Context context) {
        return PackageGuesser.getArch(getContext());
    }
}