package org.opengapps.opengapps.intro;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import com.google.firebase.analytics.FirebaseAnalytics;

import org.opengapps.opengapps.R;

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
                final Button termButton = (Button) findViewById(R.id.accept_button);
                termButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        termButton.setText(R.string.label_accepted);
                        setNextPageSwipeLock(false);
                        view.setEnabled(false);
                        termsAccepted = true;
                        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getApplicationContext());
                        Bundle bundle = new Bundle(1);
                        bundle.putBoolean("terms_accepted", true);
                        analytics.logEvent("terms_of_service", bundle);
                    }
                });
                if (!termsAccepted)
                    setNextPageSwipeLock(true);
            }
        }
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
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        String arch;
        boolean x64;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            arch = Build.SUPPORTED_32_BIT_ABIS[0];
            x64 = Build.SUPPORTED_64_BIT_ABIS.length > 0;
        } else {
            arch = Build.CPU_ABI;
            x64 = false;
        }
        if (!sharedPref.contains("selection_arch")) {
            String[] architectures = getResources().getStringArray(R.array.architectures);
            if (arch.contains("arm")) {
                if (!x64) {
                    editor.putString("selection_arch", architectures[0]); //arm
                } else {
                    editor.putString("selection_arch", architectures[1]); //arm64
                }
            } else if (arch.contains("86")) {
                if (!x64)
                    editor.putString("selection_arch", architectures[2]); //x86
                else
                    editor.putString("selection_arch", architectures[3]); //x86x64
            } else
                editor.putString("selection_arch", architectures[0]); //Default to arm
        }

        if (!sharedPref.contains("selection_android")) {
            String[] androidVersion = getResources().getStringArray(R.array.android_versions);
            switch (Build.VERSION.SDK_INT) {
                case 19:
                    editor.putString("selection_android", androidVersion[0]);//KitKat-Device
                    break;
                case 21:
                    editor.putString("selection_android", androidVersion[1]);//Lollipop-5.0-Device
                    break;
                case 22:
                    editor.putString("selection_android", androidVersion[2]);//Lollipop-5.1-Device
                    break;
                case 23:
                    editor.putString("selection_android", androidVersion[3]);//Marshmallow-Device
                    break;
                case 24:
                    editor.putString("selection_android", androidVersion[4]);//Nougat-Device
                    break;
                default:
                    editor.putString("selection_android", androidVersion[androidVersion.length - 1]); //Default to latest
            }
        }

        if (!sharedPref.contains("selection_variant"))
            editor.putString("selection_variant", "stock");

        editor.apply();
    }


    @Override
    public void onDonePressed(Fragment currentFragment) {
        GappsSelectionFragment fragment = (GappsSelectionFragment) currentFragment;
        fragment.saveSelection();
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("firstStart", false);
        editor.apply();
        finish();
    }

    @Override
    public void onBackPressed() {
        //NoOp
    }
}
