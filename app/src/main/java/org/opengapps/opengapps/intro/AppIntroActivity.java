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
import android.view.View;
import android.widget.Button;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntro2Fragment;

import org.opengapps.opengapps.R;

public class AppIntroActivity extends AppIntro2 {
    private Button permButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setInitialSettings();
        skipButtonEnabled = false;
        addSlide(AppIntro2Fragment.newInstance("Open-GApps", "Welcome to the official App for OpenGApps", R.drawable.ic_opengapps_large, Color.parseColor("#00796B")));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                addSlide(AppIntro2Fragment.newInstance("First things first", "To download an OpenGApps-Package, you have to grant us Permission to your External Storage", R.drawable.ic_opengapps_large, Color.parseColor("#00796B")));
                addSlide(new SlidePermissionFragment());
            }
        }
        addSlide(AppIntro2Fragment.newInstance("Get Root Permission", "As most of our fellow users are rooted anyway, we integrated an auto-install-mode in the app. This is completely voluntary", R.drawable.ic_supersu, Color.parseColor("#00796B")));
        addSlide(new RequestRootFragment());

        addSlide(AppIntro2Fragment.newInstance("Your Device", "In the following steps, we'll ask you a few questions about your device. We'll do our best to guess them, but if it fails, you can help us",
                R.drawable.ic_architecture, Color.parseColor("#00796B")));
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
        SharedPreferences sharedPref = getSharedPreferences(getResources().getString(R.string.pref_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (!sharedPref.contains("selection_arch")) {
            String arch = System.getProperty("os.arch");
            if (arch.contains("arm")) {
                if (arch.contains("64")) {
                    editor.putString("selection_arch", getResources().getStringArray(R.array.architectures)[1]);
                } else
                    editor.putString("selection_arch", getResources().getStringArray(R.array.architectures)[0]);
            } else if (arch.contains("86")) {
                if (arch.contains("64"))
                    editor.putString("selection_arch", getResources().getStringArray(R.array.architectures)[2]);
                else
                    editor.putString("selection_arch", getResources().getStringArray(R.array.architectures)[3]);
            }
        }

        if (!sharedPref.contains("selection_android")) {
            switch (Build.VERSION.SDK_INT) {
                case 19:
                    editor.putString("selection_android", getResources().getStringArray(R.array.android_versions)[0]);//KitKat-Device
                    break;
                case 21:
                    editor.putString("selection_android", getResources().getStringArray(R.array.android_versions)[1]);//Lollipop-5.0-Device
                    break;
                case 22:
                    editor.putString("selection_android", getResources().getStringArray(R.array.android_versions)[2]);//Lollipop-5.1-Device
                    break;
                case 23:
                    editor.putString("selection_android", getResources().getStringArray(R.array.android_versions)[3]);//Marshmallow-Device
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
        SharedPreferences sharedPref = getSharedPreferences(getResources().getString(R.string.pref_name), Context.MODE_PRIVATE);
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
