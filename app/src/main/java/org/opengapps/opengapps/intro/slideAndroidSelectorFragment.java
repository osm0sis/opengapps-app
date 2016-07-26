package org.opengapps.opengapps.intro;

import android.content.Context;
import android.content.SharedPreferences;

import org.opengapps.opengapps.R;
import org.opengapps.opengapps.SelectionValidator;

public class slideAndroidSelectorFragment extends GappsSelectionFragment {


    public slideAndroidSelectorFragment() {
        super(R.string.label_android, R.string.slide_android_description, "selection_android", R.array.android_versions);
    }

    @Override
    protected boolean isValid(String selection) {
        SharedPreferences prefs = getActivity().getSharedPreferences(getResources().getString(R.string.pref_name), Context.MODE_PRIVATE);
        String arch = prefs.getString("selection_arch", "");
        return SelectionValidator.isValidArchAnd(arch, selection);
    }
}
