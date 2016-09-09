package org.opengapps.opengapps;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SupportActivity extends AppCompatActivity {

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

        LinearLayout wiki = (LinearLayout) findViewById(R.id.wiki_button);
        LinearLayout supportChat = (LinearLayout) findViewById(R.id.chat_button);
        LinearLayout reportBug = (LinearLayout) findViewById(R.id.bugreport_button);
        LinearLayout supportForum = (LinearLayout) findViewById(R.id.support_forum_button);

        supportForum.setOnClickListener(listener);
        wiki.setOnClickListener(listener);
        supportChat.setOnClickListener(listener);
        reportBug.setOnClickListener(listener);
    }
}
