package org.opengapps.opengapps.intro;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.github.fcannizzaro.materialstepper.AbstractStep;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.opengapps.opengapps.R;


@SuppressWarnings("ConstantConditions")
public class slideTermsOfUse extends AbstractStep {
    @Override
    public String name() {
        return getArguments().getString("title", "NOT FOUND");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.appintro_terms_of_use, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Button declineButton = (Button) getView().findViewById(R.id.decline_button);
        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getContext());
                Bundle params = new Bundle(1);
                params.putString(FirebaseAnalytics.Param.VALUE, "decline");
                analytics.logEvent("terms_of_service", params);
                getActivity().setResult(1);
                getActivity().finish();
            }
        });

        Button acceptButton = (Button) getView().findViewById(R.id.accept_button);
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getContext());
                Bundle params = new Bundle(1);
                params.putString(FirebaseAnalytics.Param.VALUE, "accept");
                analytics.logEvent("terms_of_service", params);
                ((AppIntroActivity)getActivity()).onTermsAccepted((Button) v);
                onNext();
            }
        });
    }
}
