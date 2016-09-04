package org.opengapps.opengapps;

import android.os.Bundle;

import com.github.fcannizzaro.materialstepper.AbstractStep;
import com.github.fcannizzaro.materialstepper.style.TabStepper;

import org.opengapps.opengapps.intro.slideAndroidSelectorFragment;
import org.opengapps.opengapps.intro.slideArchSelectorFragment;
import org.opengapps.opengapps.intro.slideVariantSelectionFragment;

public class Stepper extends TabStepper {
    private int i = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setErrorTimeout(1500);
        setTitle(getString(R.string.pref_header_gapps_selection));
        setAlternativeTab(false);
        setLinear(true);
        addStep(createFragment(new slideArchSelectorFragment()));
        addStep(createFragment(new slideAndroidSelectorFragment()));
        addStep(createFragment(new slideVariantSelectionFragment()));
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onComplete() {
        finish();
    }


    @Override
    public void onBackPressed() {
        //noOp - you can't escape the selection
    }


    private AbstractStep createFragment(AbstractStep fragment) {
        Bundle b = new Bundle();
        b.putInt("position", i++);
        if(fragment instanceof slideArchSelectorFragment)
            b.putString("title", getString(R.string.label_architecture));
        else if(fragment instanceof slideAndroidSelectorFragment)
            b.putString("title", getString(R.string.label_android));
        else if(fragment instanceof slideVariantSelectionFragment)
            b.putString("title", getString(R.string.label_variant));
        fragment.setArguments(b);
        return fragment;
    }
}
