package org.opengapps.app.intro;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.opengapps.app.R;
import org.opengapps.app.prefs.Preferences;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

@SuppressWarnings("ConstantConditions")
public class RequestRootFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.appintro_request_root, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initButton();
    }

    private void initButton() {
        Button requestButton = (Button) getView().findViewById(R.id.permission_button);
        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> run = Shell.SU.run("whoami");
                if (run != null && run.size() >= 1 && run.get(0).equals("root")) {
                    ((Button) view).setText(R.string.permission_granted);
                    view.setEnabled(false);
                    getContext().getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE).edit().putBoolean("root_mode", true).apply();
                }
            }
        });
    }
}
