package org.opengapps.opengapps.intro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.github.fcannizzaro.materialstepper.AbstractStep;

import org.opengapps.opengapps.R;


public abstract class GappsSelectionFragment extends AbstractStep implements SharedPreferences.OnSharedPreferenceChangeListener, RadioGroup.OnCheckedChangeListener {
    private final int title;
    private final int description;
    private final String key;
    private final int stringArray;
    private RadioGroup group;
    private SparseArray<RadioButton> buttons;
    private SharedPreferences prefs;

    public GappsSelectionFragment(int title, int description, String key, int stringArray) {
        this.title = title;
        this.description = description;
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

    @Override
    public String name() {
        return getArguments().getString("title", "NOT FOUND");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.appintro_set_variant, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle b = getArguments();
        TextView header = (TextView) getView().findViewById(R.id.headline_intro_gapps);
        header.setText(getString(title));
        TextView descriptionView = (TextView) getView().findViewById(R.id.description_intro_gapps);
        descriptionView.setText(getString(description));
        if (b != null && b.containsKey("position")) {
            header.setVisibility(View.GONE);
            descriptionView.setVisibility(View.GONE);
        } else {
            ConstraintLayout layout = (ConstraintLayout) getView().findViewById(R.id.constraint_selection);
            layout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDark));
        }
        loadRadioBoxes();
        prefs = getActivity().getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    protected abstract boolean isValid(String selection);

    private void loadRadioBoxes() {
        SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        String selection = prefs.getString(key, null);
        String[] items = getResources().getStringArray(stringArray);
        if (getView() != null)
            group = (RadioGroup) getView().findViewById(R.id.arch_radio_group);
        buttons = new SparseArray<>(items.length);
        for (String item : items) {
            RadioButton radioButton = new RadioButton(getActivity());
            radioButton.setText(item);
//            radioButton.setTextColor(Color.parseColor("#ffffff"));
            if (selection.toLowerCase().equals(item.toLowerCase()))
                radioButton.setChecked(true);
            if (!isValid(item))
                radioButton.setEnabled(false);
            int id = View.generateViewId();
            radioButton.setId(id);
            buttons.put(id, radioButton);
            group.addView(radioButton);
        }
        group.setOnCheckedChangeListener(this);
    }

    public void saveSelection() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, buttons.get(group.getCheckedRadioButtonId()).getText().toString());
        editor.apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (prefs.getBoolean("firstStart", true))
            if (!s.equals(key)) {
                group.removeAllViews();
                loadRadioBoxes();
            }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        saveSelection();
    }


}
