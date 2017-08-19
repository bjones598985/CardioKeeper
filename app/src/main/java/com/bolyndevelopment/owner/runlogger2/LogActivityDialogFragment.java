package com.bolyndevelopment.owner.runlogger2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;

import com.bolyndevelopment.owner.runlogger2.databinding.DialogFragLayoutBinding;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Owner on 11/18/2015.
 */
public class LogActivityDialogFragment extends DialogFragment {
    LogActivityListener mListener;

    DialogFragLayoutBinding binding;

    final int CARDIO_SPINNER = 1;
    final int TIME_EDITTEXT = 2;
    final int DIST_EDITTEXT = 3;

    interface LogActivityListener {
        public void onDialogPositiveClick(Bundle bundle);
    }

    private void setDateInput(String datePicked) {
        binding.dateInput.setText(datePicked);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle inBundle = getArguments();
        final String totalTime = inBundle.getString("totalTime");
        //lapData = inBundle.getStringArrayList("lapData");

        mListener = (LogActivityListener) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        binding = DataBindingUtil.inflate(getActivity().getLayoutInflater(), R.layout.dialog_frag_layout, null, false);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.cardio_types, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        binding.cardioTypeSpinner.setAdapter(adapter);
        //set the cardio type to the last recorded one
        final String type = getArguments().getString("type");
        if (type != null) {
            final List<String> list = Arrays.asList(getResources().getStringArray(R.array.cardio_types));
            binding.cardioTypeSpinner.setSelection(list.indexOf(type));
        }
        //set the date to today, can change if want
        String date = Utils.convertDateToString(new Date(), "MM/dd/yyyy");
        binding.dateInput.setText(date);
        if (totalTime != null) {
            binding.timeInput.setText(totalTime);
        }
        binding.datePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runDatePicker();
            }
        });
        binding.cardioTypeSpinner.requestFocus();
        builder.setView(binding.getRoot())
                .setTitle("Add Your Cardio")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (validateTimeFormattedProper()) {
                            int validation = validateFields();
                            if (validation == -1) {
                                mListener.onDialogPositiveClick(addInfoToArray());
                            } else {
                                highlightField(validation);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getDialog().cancel();
                    }
                });
        return builder.create();
    }

    private int validateFields() {
        if (binding.cardioTypeSpinner.getSelectedItemPosition() == 0) return CARDIO_SPINNER;
        if (binding.timeInput.getText().equals("")) return TIME_EDITTEXT;
        if (binding.milesInput.getText().equals("")) return DIST_EDITTEXT;
        return -1;
    }

    private void highlightField(int field) {
        Log.d("TEST", "highlightField");
        switch (field) {
            case CARDIO_SPINNER:
                Log.d("TEST", "CardioSpinner");
                Drawable background = getResources().getDrawable(R.drawable.rounded_corner_background);
                background.mutate();
                background.setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
                binding.cardioTypeSpinner.setBackground(background);
                break;
            case TIME_EDITTEXT:

                break;
            case DIST_EDITTEXT:

                break;
        }
    }

    private Bundle addInfoToArray() {
        ArrayList<String> runData = new ArrayList<>();
        runData.add(binding.dateInput.getText().toString());
        runData.add(getTimeMillis());
        runData.add(binding.milesInput.getText().toString());
        runData.add(binding.caloriesInput.getText().toString());
        String cardio = (String) binding.cardioTypeSpinner.getSelectedItem();
        runData.add(cardio); //how we'll add in the cardio type
        Bundle runInfo = new Bundle();
        runInfo.putStringArrayList("data", runData);
        //runInfo.putStringArrayList("lapData", lapData);
        return runInfo;
    }

    private String getTimeMillis() {
        final String time = binding.timeInput.getText().toString();
        String[] array = TextUtils.split(time, ":");
        int[] timeArray = new int[array.length];
        for (int x = 0; x<array.length; x++) {
            if (array[x].equals("")) {
                timeArray[x] = 0;
            } else {
                timeArray[x] = Integer.valueOf(array[x]);
            }
        }
        long millis = 0;
        switch (timeArray.length) {
            case 2:
                millis =  Utils.convertToMillis(0, timeArray[0], timeArray[1]);
                break;
            case 3:
                millis =  Utils.convertToMillis(timeArray[0], timeArray[1], timeArray[2]);
                break;
        }
        return String.valueOf(millis);
    }

    private void runDatePicker() {
        //DialogFragment newFragment = new DatePickerFragment();
        //newFragment.show(getFragmentManager(), "datePicker");
    }

    private boolean validateTimeFormattedProper() {
        final String time = binding.timeInput.getText().toString();
        String[] array = TextUtils.split(time, ":");
        int[] timeArray = new int[array.length];
        for (int x = 0; x<array.length;x++) {
            if (array[x].equals("")) {
                timeArray[x] = 0;
            } else {
                timeArray[x] = Integer.valueOf(array[x]);
            }
        }
        switch (timeArray.length) {
            case 2:
                if (timeArray[1] >= 60) {
                    binding.timeLayout.setError("Seconds >= 60");
                    return false;
                } else {
                    binding.timeLayout.setErrorEnabled(false);
                    return true;
                }
            case 3:
                if (timeArray[2] >= 60) {
                    binding.timeLayout.setError("Seconds >= 60");
                    return false;
                } else if (timeArray[1] >= 60){
                    binding.timeLayout.setError("Minutes >= 60");
                    return false;
                } else {
                    binding.timeLayout.setErrorEnabled(false);
                    return true;
                }
            default:
                binding.timeLayout.setError("Something ain't right...");
                return false;
        }
    }

    /*
    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            month++;
            String formattedDate = String.format(Locale.US, "%02d/%02d/%04d", month, day, year);
            Log.d("LADF", "formatted date: " + formattedDate);
            //String datePicked = month + "/" + day + "/" + year;
            // Do something with the date chosen by the user
            ((LogActivityDialogFragment)getFragmentManager().findFragmentByTag("dialog")).setDateInput(formattedDate);
        }
    }
    */
}
