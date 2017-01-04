package org.opengapps.app.card;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import org.opengapps.app.R;

public class SupportCard extends CardView {

    Button supportButton;
    Button laterButton;

    public SupportCard(Context context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.support_card, this, true);
        initButtons();
    }

    public SupportCard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initButtons() {
        supportButton = (Button) findViewById(R.id.support_button);
        laterButton = (Button) findViewById(R.id.later_button);
    }

    public void setSupportButton(View.OnClickListener clickListener) {
        supportButton.setOnClickListener(clickListener);
    }

    public void setLaterListener(View.OnClickListener clickListener) {
        laterButton.setOnClickListener(clickListener);
    }

    public void setLaterLabel(String label) {
        laterButton.setText(label);
    }
}
