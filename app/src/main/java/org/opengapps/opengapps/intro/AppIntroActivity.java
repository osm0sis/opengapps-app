package org.opengapps.opengapps.intro;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.widget.Button;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntro2Fragment;
import com.github.paolorotolo.appintro.AppIntroViewPager;

import org.opengapps.opengapps.R;
import org.opengapps.opengapps.prefs.Preferences;

import eu.chainfire.libsuperuser.Shell;


public class AppIntroActivity extends AppIntro2 {
    private Button permButton;
    private boolean termsAccepted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setInitialSettings();
        skipButtonEnabled = false;
        int primaryDarkColor = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark);
        addSlide(AppIntro2Fragment.newInstance(getString(R.string.app_name), getString(R.string.appintro_introslide_title), R.drawable.ic_opengapps_large, primaryDarkColor));
        addSlide(new slideTermsOfUse());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                addSlide(new SlidePermissionFragment());
            }
        }

        if (Shell.SU.available())
            addSlide(new RequestRootFragment());

        addSlide(new slideArchSelectorFragment());
        addSlide(new slideAndroidSelectorFragment());
        addSlide(new slideVariantSelectionFragment());
    }

    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        if (newFragment != null) {
            if (newFragment.getClass().equals(SlidePermissionFragment.class) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permButton = (Button) findViewById(R.id.permission_button);
                setupPermissionButton();

                setNextPageSwipeLock(true);
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    setNextPageSwipeLock(false);
            }
            if (newFragment.getClass().equals(slideTermsOfUse.class)) {
                if (!termsAccepted)
                    setNextPageSwipeLock(true);
            }
            if (newFragment instanceof GappsSelectionFragment)
                ((GappsSelectionFragment) newFragment).onStepVisible();
            if (oldFragment instanceof GappsSelectionFragment)
                ((GappsSelectionFragment) oldFragment).saveSelections();
        }
    }

    public void onTermsAccepted(Button termButton) {
        termButton.setText(R.string.label_accepted);
        setNextPageSwipeLock(false);
        termButton.setEnabled(false);
        termsAccepted = true;
        AppIntroViewPager pager = getPager();
        pager.setCurrentItem(pager.getCurrentItem() + 1, true);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                setNextPageSwipeLock(false);
                permButton.setText(getApplication().getText(R.string.permission_granted));
                permButton.setEnabled(false);
            }
        }
    }

    private void setupPermissionButton() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permButton.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onClick(View view) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                        setNextPageSwipeLock(false);
                    else
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                }
            });
        }
    }

    private void setInitialSettings() {
        GappsSelectionFragment.selectionArch = PackageGuesser.getArch(getBaseContext());
        GappsSelectionFragment.selectionAnd = PackageGuesser.getAndroidVersion(getBaseContext());
        GappsSelectionFragment.selectionVariant = PackageGuesser.getVariant(getBaseContext());
    }


    @Override
    public void onDonePressed(Fragment currentFragment) {
        SharedPreferences sharedPref = getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("firstStart", false);
        editor.putString("selection_android", GappsSelectionFragment.selectionAnd);
        editor.putString("selection_variant", GappsSelectionFragment.selectionVariant);
        editor.putString("selection_arch", GappsSelectionFragment.selectionArch);
        editor.apply();
        setResult(2);
        finish();
    }

    @Override
    public void onBackPressed() {
        //NoOp
    }
}
