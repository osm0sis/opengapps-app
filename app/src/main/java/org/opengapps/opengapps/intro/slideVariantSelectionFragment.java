package org.opengapps.opengapps.intro;

import android.content.Context;
import android.content.SharedPreferences;

import org.opengapps.opengapps.R;
import org.opengapps.opengapps.SelectionValidator;


public class slideVariantSelectionFragment extends GappsSelectionFragment {
    public slideVariantSelectionFragment() {
        super(R.string.label_variant, R.string.slide_variant_description, "selection_variant", R.array.opengapps_variant);
    }

    @Override
    protected boolean isValid(String selection) {
        SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        String arch = prefs.getString("selection_arch", "");
        String and = prefs.getString("selection_android", "");
        return SelectionValidator.isValid(arch, and, selection);
    }
}
