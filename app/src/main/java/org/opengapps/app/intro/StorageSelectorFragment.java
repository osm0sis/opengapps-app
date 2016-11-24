package org.opengapps.app.intro;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.codekidlabs.storagechooser.ExternalStoragePathFinder;

import org.opengapps.app.R;
import org.opengapps.app.prefs.Preferences;

public class StorageSelectorFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.appintro_storage_selector, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initButton();
    }

    private void initButton() {
        Button chooseStorageButton = (Button) getView().findViewById(R.id.choose_storage_button);

        chooseStorageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ExternalStoragePathFinder.Builder builder = new ExternalStoragePathFinder.Builder()
                        .withActivity(getActivity())
                        .withFragmentManager(getActivity().getSupportFragmentManager())
                        .actionSave(getContext().getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE))
                        .build();

                builder.show();
            }
        });
    }
}
