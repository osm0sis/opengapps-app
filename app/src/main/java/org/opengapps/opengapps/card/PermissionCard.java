package org.opengapps.opengapps.card;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import org.opengapps.opengapps.R;

public class PermissionCard extends CardView {

    public PermissionCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.permission_card, this, true);
        initPermissionButton();
    }


    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public void init(){
        int permissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            setVisibility(View.GONE);
        } else {
            setVisibility(View.VISIBLE);
        }
    }

    private void initPermissionButton() {
        Button permissionButton = (Button) findViewById(R.id.grant_permission_button);
        permissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Activity activity = getActivity();
                if (activity != null) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                }
            }
        });
    }
}
