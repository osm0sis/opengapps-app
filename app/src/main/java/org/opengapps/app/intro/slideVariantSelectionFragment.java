package org.opengapps.app.intro;

import android.content.Context;

import org.opengapps.app.R;
import org.opengapps.app.SelectionValidator;


public class slideVariantSelectionFragment extends GappsSelectionFragment {

    public slideVariantSelectionFragment() {
        super(R.string.appintro_label_variant, R.string.slide_variant_description, R.string.slide_variant_hint, R.string.variant_moreinfo, "selection_variant", R.array.opengapps_variant);
    }

    @Override
    public String getGuessedSelection(Context context) {
        return PackageGuesser.getVariant();
    }

    @Override
    protected boolean isValid(String selection) {
        String arch = selectionArch;
        String and = selectionAnd;
        return SelectionValidator.isValid(arch, and, selection);
    }

    @Override
    public String getSelection() {
        return GappsSelectionFragment.selectionVariant;
    }

    @Override
    public void setSelection(String selection) {
        GappsSelectionFragment.selectionVariant = selection;
    }
}
