package org.opengapps.app.intro;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.codekidlabs.storagechooser.utils.DiskUtil;
import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntro2Fragment;
import com.github.paolorotolo.appintro.AppIntroViewPager;

import org.opengapps.app.BuildConfig;
import org.opengapps.app.NavigationActivity;
import org.opengapps.app.R;
import org.opengapps.app.ZipInstaller;
import org.opengapps.app.download.Downloader;
import org.opengapps.app.prefs.Preferences;


public class AppIntroActivity extends AppIntro {
    private Button permButton;
    private boolean termsAccepted = false;

    // flavor [GLOBALLY ACCESSIBLE SWITCH]
    public static final String BUILD_FLAVOR = BuildConfig.BUILD_TYPE;
    //flavor types
    public static final String FLAVOR_GPLAY = "gplay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        showSkipButton(false);
        setSeparatorColor(ContextCompat.getColor(getBaseContext(), R.color.app_intro_seperator_color));
        setInitialSettings();
        int primaryDarkColor = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark);
        addSlide(AppIntro2Fragment.newInstance(getString(R.string.app_name), getString(R.string.appintro_introslide_title), R.drawable.ic_opengapps_large, primaryDarkColor));
        if(!BUILD_FLAVOR.contentEquals(FLAVOR_GPLAY)) {
            addSlide(new slideTermsOfUse());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                addSlide(new SlidePermissionFragment());
            }
        }

        if (ZipInstaller.hasRoot()) {
            addSlide(new RequestRootFragment());
        }

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
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    setNextPageSwipeLock(false);
                }
            }
            if(!BUILD_FLAVOR.contentEquals(FLAVOR_GPLAY)) {
                if (newFragment.getClass().equals(slideTermsOfUse.class)) {
                    if (!termsAccepted) {
                        setNextPageSwipeLock(true);
                    }
                }
            }
            if (newFragment instanceof GappsSelectionFragment) {
                ((GappsSelectionFragment) newFragment).onStepVisible();
            }
            if (oldFragment instanceof GappsSelectionFragment) {
                ((GappsSelectionFragment) oldFragment).saveSelections();
            }
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
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        setNextPageSwipeLock(false);
                    } else {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    }
                }
            });
        }
    }

    private void setInitialSettings() {
        GappsSelectionFragment.selectionArch = PackageGuesser.getArch(getBaseContext());
        GappsSelectionFragment.selectionAnd = PackageGuesser.getAndroidVersion(getBaseContext());
        String guessedVariant = PackageGuesser.getVariant();
        GappsSelectionFragment.selectionVariant = !TextUtils.isEmpty(guessedVariant) ? guessedVariant : "stock";
    }


    @Override
    public void onDonePressed(Fragment currentFragment) {
        SharedPreferences sharedPref = getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("selection_android", GappsSelectionFragment.selectionAnd)
        .putString("selection_variant", GappsSelectionFragment.selectionVariant)
        .putString("selection_arch", GappsSelectionFragment.selectionArch)
        .putBoolean("firstStart", false)
        //  Add primary download path to pref
        .putString(DiskUtil.SC_PREFERENCE_KEY, Environment.getExternalStorageDirectory().getAbsolutePath() + Downloader.OPENGAPPS_PREDEFINED_PATH)
        // add rate count during 1st run
        .putInt("rate_count", 0)
        .putBoolean("rate_done", false).apply();

        Intent i = new Intent(getBaseContext(), NavigationActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        //NoOp
    }
}
