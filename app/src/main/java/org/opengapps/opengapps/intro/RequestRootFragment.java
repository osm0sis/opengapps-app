package org.opengapps.opengapps.intro;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.opengapps.opengapps.R;

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
                Shell.SU.run("ls");
            }
        });
    }
}
