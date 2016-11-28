package org.opengapps.app.card;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import org.opengapps.app.R;

public class RateUsCard extends CardView {

    Button rateButton;
    Button laterButton;

    public RateUsCard(Context context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.rate_us_card, this, true);
        initButtons();
    }

    public RateUsCard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initButtons() {
        rateButton = (Button) findViewById(R.id.rate_button);
        laterButton = (Button) findViewById(R.id.later_button);
    }

    public void setRateListener(View.OnClickListener clickListener) {
        rateButton.setOnClickListener(clickListener);
    }

    public void setLaterListener(View.OnClickListener clickListener) {
        laterButton.setOnClickListener(clickListener);
    }
}
