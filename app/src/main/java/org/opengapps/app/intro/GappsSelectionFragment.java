package org.opengapps.app.intro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.Html;
import android.text.Spanned;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.fcannizzaro.materialstepper.AbstractStep;

import org.opengapps.app.DownloadFragment;
import org.opengapps.app.R;
import org.opengapps.app.prefs.Preferences;


@SuppressWarnings("ConstantConditions")
public abstract class GappsSelectionFragment extends AbstractStep implements RadioGroup.OnCheckedChangeListener {
    public static String selectionArch = "";
    public static String selectionAnd = "";
    public static String selectionVariant = "";
    private final int title;
    private final int description;
    private final int smallDesc;
    private final int link;
    private static boolean hintShown = false;
    private ColorStateList style;
    private final String key;
    private final int stringArray;
    private RadioGroup group;
    private SparseArray<RadioButton> buttons;

    public GappsSelectionFragment(int title, int description, int smallDesc, int link, String key, int stringArray) {
        this.title = title;
        this.description = description;
        this.smallDesc = smallDesc;
        this.link = link;
        this.key = key;
        this.stringArray = stringArray;
    }

    @Override
    public void onStepVisible() {
        if (isAdded()) {
            group.removeAllViews();
            loadRadioBoxes();
        }
    }

    public abstract String getGuessedSelection(Context context);

    @Override
    public String name() {
        return getArguments().getString("title", "NOT FOUND");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        style = new ColorStateList(
                new int[][]{

                        new int[]{-android.R.attr.state_checked}, //unchecked
                        new int[]{android.R.attr.state_checked} //checked
                },
                new int[]{
                        Color.parseColor("#E8E8E8") //unchecked
                        , ContextCompat.getColor(getContext(), R.color.colorAccent)//checked
                }
        );
        return inflater.inflate(R.layout.appintro_set_variant, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initButtons();
        Bundle b = getArguments();
        TextView header = (TextView) getView().findViewById(R.id.headline_intro_gapps);
        header.setText(getString(title));
        TextView descriptionView = (TextView) getView().findViewById(R.id.description_intro_gapps);
        @SuppressWarnings("deprecation") Spanned spanned = Html.fromHtml(getString(description));
        descriptionView.setText(spanned);
        if (b != null && b.containsKey("position")) {
            header.setVisibility(View.GONE);
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) ((View) descriptionView.getParent()).getLayoutParams();
            layoutParams.setMargins(0, 0, 0, DownloadFragment.dpToPx(getActivity(), 50));
            layoutParams.setMarginStart(DownloadFragment.dpToPx(getActivity(), 24));
            layoutParams.setMarginEnd(DownloadFragment.dpToPx(getActivity(), 24));
        }
        ConstraintLayout layout = (ConstraintLayout) getView().findViewById(R.id.constraint_selection);
        layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDark));

        loadRadioBoxes();
    }

    private void initButtons() {
        getView().findViewById(R.id.more_info_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(link))));
            }
        });
    }

    protected abstract boolean isValid(String selection);

    private void loadRadioBoxes() {
        SharedPreferences prefs = getActivity().getSharedPreferences(Preferences.prefName, Context.MODE_PRIVATE);
        String defaultSelection;
        if (getSelection().equals("")) {
            defaultSelection = prefs.getString(key, "");
        } else {
            defaultSelection = getSelection();
        }
        String[] items = getResources().getStringArray(stringArray);
        if (getView() != null) {
            group = (RadioGroup) getView().findViewById(R.id.arch_radio_group);
        }
        buttons = new SparseArray<>(items.length);
        for (String item : items) {
            AppCompatRadioButton radioButton = new AppCompatRadioButton(getActivity());
            radioButton.setText(item);
            radioButton.setSupportButtonTintList(style);
            radioButton.setTextColor(Color.parseColor("#E0E0E0"));
            if (getGuessedSelection(getContext()).toLowerCase().equals(item.toLowerCase())) {
                //noinspection deprecation
                Spanned spanned = Html.fromHtml(radioButton.getText() + " <i color='red'>(" + getString(R.string.detected) + ")</i>");
                radioButton.setText(spanned);
            }
            if (defaultSelection.toLowerCase().equals(item.toLowerCase())) {
                radioButton.setChecked(true);
                setSelection(defaultSelection);
            }
            if (!isValid(item)) {
                radioButton.setEnabled(false);
            }
            int id = View.generateViewId();
            radioButton.setId(id);
            buttons.put(id, radioButton);
            group.addView(radioButton);
        }
        checkIfValid();
        group.setOnCheckedChangeListener(this);
    }

    private void checkIfValid() {
        RadioButton selectedButton = buttons.get(group.getCheckedRadioButtonId());
        if (selectedButton != null && selectedButton.isChecked() && !selectedButton.isEnabled()) {
            for (int i = 0; i < buttons.size(); i++) {
                int key = buttons.keyAt(i);
                // get the object by the key.
                RadioButton button = buttons.get(key);
                if (button.isEnabled()) {
                    button.setChecked(true);
                }
            }
        }
    }

    public abstract String getSelection();

    public abstract void setSelection(String selection);

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        Button button = buttons.get(group.getCheckedRadioButtonId());
        String text = buttons.get(group.getCheckedRadioButtonId()).getText().toString();
        if (text.contains(" ")) {
            text = text.substring(0, text.indexOf(" "));
        }
        if (text.equals(getGuessedSelection(getContext())) && button.isPressed()) {
            Toast.makeText(getContext(), getString(smallDesc), Toast.LENGTH_SHORT).show();
        }
        setSelection(text);
    }

    public void saveSelections() {
        onCheckedChanged(null, 0);
    }
}
