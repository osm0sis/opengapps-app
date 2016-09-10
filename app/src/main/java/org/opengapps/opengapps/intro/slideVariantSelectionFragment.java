package org.opengapps.opengapps.intro;

import org.opengapps.opengapps.R;
import org.opengapps.opengapps.SelectionValidator;


public class slideVariantSelectionFragment extends GappsSelectionFragment {

    public slideVariantSelectionFragment() {
        super(R.string.label_variant, R.string.slide_variant_description, "selection_variant", R.array.opengapps_variant);
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
