package org.opengapps.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.github.fcannizzaro.materialstepper.AbstractStep;
import com.github.fcannizzaro.materialstepper.style.DotStepper;

import org.opengapps.app.intro.GappsSelectionFragment;
import org.opengapps.app.intro.slideAndroidSelectorFragment;
import org.opengapps.app.intro.slideArchSelectorFragment;
import org.opengapps.app.intro.slideVariantSelectionFragment;
import org.opengapps.app.prefs.Preferences;

public class Stepper extends DotStepper {
    private int i = 1;
    private int currentStep = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTitle(getString(R.string.pref_header_gapps_selection));
        addStep(createFragment(new slideArchSelectorFragment()));
        addStep(createFragment(new slideAndroidSelectorFragment()));
        addStep(createFragment(new slideVariantSelectionFragment()));
        super.onCreate(savedInstanceState);
        setSupportActionBar(getToolbar());
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onNavigateUp() {
        super.onNavigateUp();
        GappsSelectionFragment.selectionArch = "";
        GappsSelectionFragment.selectionVariant = "";
        GappsSelectionFragment.selectionAnd = "";
        return true;
    }

    @Override
    public void onNext() {
        super.onNext();
        currentStep++;
    }

    @Override
    public void onPrevious() {
        super.onPrevious();
        currentStep--;
    }

    @Override
    public void onComplete() {
        SharedPreferences sharedPref = getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("firstStart", false);
        editor.putString("selection_android", GappsSelectionFragment.selectionAnd);
        editor.putString("selection_variant", GappsSelectionFragment.selectionVariant);
        editor.putString("selection_arch", GappsSelectionFragment.selectionArch);
        editor.apply();
        GappsSelectionFragment.selectionArch = "";
        GappsSelectionFragment.selectionVariant = "";
        GappsSelectionFragment.selectionAnd = "";
        finish();
    }

    @Override
    public void onBackPressed() {
        if (currentStep == 1) {
            onNavigateUp();
        } else {
            onPrevious();
        }
    }

    private AbstractStep createFragment(AbstractStep fragment) {
        Bundle b = new Bundle();
        b.putInt("position", i++);
        if (fragment instanceof slideArchSelectorFragment) {
            b.putString("title", getString(R.string.label_architecture));
        } else if (fragment instanceof slideAndroidSelectorFragment) {
            b.putString("title", getString(R.string.label_android));
        } else if (fragment instanceof slideVariantSelectionFragment) {
            b.putString("title", getString(R.string.label_variant));
        }
        fragment.setArguments(b);
        return fragment;
    }
}
