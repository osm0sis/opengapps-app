package org.opengapps.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import static org.opengapps.app.intro.AppIntroActivity.BUILD_FLAVOR;
import static org.opengapps.app.intro.AppIntroActivity.FLAVOR_GPLAY;

public class SupportActivity extends AppCompatActivity {

    private LinearLayout supportButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        initButtons();
    }


    private void initButtons() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView t = (TextView) view.findViewById(R.id.support_url_text);
                String url = t.getText().toString();
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i);
            }
        };

        findViewById(R.id.homepage_button).setOnClickListener(listener);
        findViewById(R.id.translate_button).setOnClickListener(listener);
        findViewById(R.id.bugreport_button).setOnClickListener(listener);
        findViewById(R.id.wiki_button).setOnClickListener(listener);
        findViewById(R.id.support_chat_button).setOnClickListener(listener);
        findViewById(R.id.support_forum_button).setOnClickListener(listener);
        findViewById(R.id.github_button).setOnClickListener(listener);
        supportButton = (LinearLayout) findViewById(R.id.support_opengapps_button);
        supportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = getBaseContext().getString(R.string.url_support_opengapps);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        if(BUILD_FLAVOR.contentEquals(FLAVOR_GPLAY)) {
            supportButton.setVisibility(View.GONE);
        }
    }
}
