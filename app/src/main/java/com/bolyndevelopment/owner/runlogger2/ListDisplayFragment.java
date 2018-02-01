package com.bolyndevelopment.owner.runlogger2;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ListDisplayFragment extends Fragment {

    List<ListItem> recordsList = new ArrayList<>();
    List<ListItem> oldList = new ArrayList<>();
    ArrayList<String> lapDataFromTimer;
    MyAdapter myAdapter;
    RecyclerView mainRecyclerView;
    boolean isAddDialogOpen = false;

    static final int ASCENDING = -1;
    static final int DESCENDING = 1;

    static final int ALPHA_25 = 63;
    static final int CODE_TIMER = 100;
    static final int MIN_DELAY_MILLIS = 200;

    Handler handler = new Handler();

    private ListFragListener mListener;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MainActivityAlt", "onActivityResult - LDF");
        if (requestCode == CODE_TIMER && resultCode == Activity.RESULT_OK) {
            final String totalTime = data.getStringExtra("totalTime");
            lapDataFromTimer = data.getStringArrayListExtra("list");
            initAddDialog(totalTime);
        }
    }

    public interface ListFragListener {
        void graphIt(String param1, String param2);
        long saveEnteredData(HashMap<String, String> map, ArrayList<String> lapData);
        void setInitDialogOpen(boolean isOpen);
    }

    public ListDisplayFragment() {
    }

    public void notifyOfDataChange() {
        myAdapter.notifyDataSetChanged();
    }

    public static ListDisplayFragment newInstance(String param1, String param2) {
        return new ListDisplayFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //addRandomData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_display, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainRecyclerView = (RecyclerView) view.findViewById(R.id.main_recyclerview);
        mainRecyclerView.setHasFixedSize(true);
        myAdapter = new MyAdapter();
        mainRecyclerView.setAdapter(myAdapter);
        ((MainActivityAlt)getActivity()).queryForRecords();
        //queryForRecords();
        /*
        used to add decoration onto recyclerview
        Drawable right = getResources().getDrawable(R.drawable.right_divider);
        Drawable left = getResources().getDrawable(R.drawable.left_divider);
        binder.mainRecyclerview.addItemDecoration(new DividerDecoration(left, right));
        */
    }

    public void sortList(String... args) {
        List<Comparator<ListItem>> comparatorList = new ArrayList<>();
        oldList.clear();
        oldList.addAll(recordsList);
        for (String str : args) {
            switch (str) {
                case "Select...":
                    continue;
                case "Exercise: A-Z":
                    comparatorList.add(ListSorter.getAlphabeticComparator(ASCENDING));
                    //ListSorter.sortAlphabetic(recordsList, ASCENDING);
                    break;
                case "Exercise: Z-A":
                    comparatorList.add(ListSorter.getAlphabeticComparator(DESCENDING));
                    //ListSorter.sortAlphabetic(recordsList, DESCENDING);
                    break;
                case "Date: Ascending":
                    comparatorList.add(ListSorter.getDateComparator(ASCENDING));
                    //ListSorter.sortByDate(recordsList, ASCENDING);
                    break;
                case "Date: Descending":
                    comparatorList.add(ListSorter.getDateComparator(DESCENDING));
                    //ListSorter.sortByDate(recordsList, DESCENDING);
                    break;
                case "Distance: Ascending":
                    comparatorList.add(ListSorter.getDistanceComparator(ASCENDING));
                    //ListSorter.sortByDistance(recordsList, ASCENDING);
                    break;
                case "Distance: Descending":
                    comparatorList.add(ListSorter.getDistanceComparator(DESCENDING));
                    //ListSorter.sortByDistance(recordsList, DESCENDING);
                    break;
                case "Time: Ascending":
                    comparatorList.add(ListSorter.getTimeComparator(ASCENDING));
                    //ListSorter.sortByTime(recordsList, ASCENDING);
                    break;
                case "Time: Descending":
                    comparatorList.add(ListSorter.getTimeComparator(DESCENDING));
                    //ListSorter.sortByTime(recordsList, DESCENDING);
                    break;
                case "Calories: Ascending":
                    comparatorList.add(ListSorter.getCalorieComparator(ASCENDING));
                    //ListSorter.sortByCalories(recordsList, ASCENDING);
                    break;
                case "Calories: Descending":
                    comparatorList.add(ListSorter.getCalorieComparator(DESCENDING));
                    //ListSorter.sortByCalories(recordsList, DESCENDING);
                    break;
            }
            Collections.sort(recordsList, new ChainedComparator(comparatorList));
            //notifyOfDataChange();
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ListDiffCallback(oldList, recordsList));
            result.dispatchUpdatesTo(myAdapter);
        }
    }

    public void onRecordsQueried(final Cursor cursor) {
        oldList.clear();
        oldList.addAll(recordsList);
        recordsList.clear();
        if (cursor.getCount() == 0) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Uh oh, no results for that selection...", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            cursor.moveToFirst();
            ListItem item;
            while (!cursor.isAfterLast()) {
                item = new ListItem();
                item.cType = cursor.getString(4);
                item.calories = cursor.getInt(3);
                item.distance = cursor.getFloat(2);
                item.date = cursor.getString(0);
                item.time = Utils.convertMillisToHms(cursor.getLong(1));
                recordsList.add(item);
                cursor.moveToNext();
            }
            cursor.close();
            //myAdapter.notifyDataSetChanged();
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ListDiffCallback(oldList, recordsList));
            result.dispatchUpdatesTo(myAdapter);
        }
    }

    public void initAddDialog(@Nullable String time) {
        AddDialog ad = new AddDialog();
        if (time != null) {
            ad.time = time;
            Cursor c = DataModel.getInstance().rawQuery("select date, cardio_type from Data limit 1", null);
            c.moveToFirst();
            String type = null;
            if (c.getCount() > 0) {
                type = c.getString(1);
                c.close();
            }
            if (type != null) {
                final List<String> list = Arrays.asList(getResources().getStringArray(R.array.cardio_types));
                ad.spinnerPosition = list.indexOf(type);
            }
        }
        ad.date = Utils.convertDateToString(new Date(), "MM/dd/yyyy");
        recordsList.add(0, ad);
        mainRecyclerView.getAdapter().notifyItemInserted(0);
        mainRecyclerView.scrollToPosition(0);
    }

    public void onTimerFabClicked(boolean isAddDialogOpen) {
        if (isAddDialogOpen) {
            recordsList.remove(0);
            mainRecyclerView.getAdapter().notifyItemRemoved(0);
        }
        final Intent i = new Intent(getActivity(), TimerActivity.class);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivityForResult(i, CODE_TIMER);
            }
        }, MIN_DELAY_MILLIS);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ListFragListener) {
            mListener = (ListFragListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ListFragListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void onSaveEnteredData(long id, HashMap<String, String> map) {
        if (id > -1) {
            ListItem item = new ListItem();
            item.date = map.get(MainActivityAlt.DATE);
            item.time = Utils.convertMillisToHms(Long.parseLong(map.get(MainActivityAlt.TIME)));
            item.distance = map.get(MainActivityAlt.DISTANCE).equals("") ? 0 : Float.parseFloat(map.get(MainActivityAlt.DISTANCE));
            item.calories = map.get(MainActivityAlt.CALORIES).equals("") ? 0 : Integer.parseInt(map.get(MainActivityAlt.CALORIES));
            item.cType = map.get(MainActivityAlt.CARDIO_TYPE);
            recordsList.add(0, item);
            recordsList.remove(1);
            myAdapter.notifyItemChanged(0);
            mainRecyclerView.scrollToPosition(0);
            mListener.setInitDialogOpen(false);
        }
    }

    /*
    public static class ListItem {
        int calories;
        float distance;
        String date = null, time = null, cType;

        @Override
        public String toString() {
            return "cType: " + cType + ", calories: " + calories + ", distance: " +
                    distance + ", date: " + date + ", time: " + time;
        }
    }

    private class AddDialog extends ListItem {
        int spinnerPosition = 0;
    }*/

    private class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final int LIST_ITEM = 1;
        final int ADD_DIALOG = 2;

        final int CARDIO_SPINNER = 1;
        final int TIME_EDITTEXT = 2;
        final int DIST_EDITTEXT = 3;

        @Override
        public int getItemViewType(int position) {
            return recordsList.get(position) instanceof AddDialog ? ADD_DIALOG : LIST_ITEM;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return viewType == LIST_ITEM ? new MyAdapter.BaseViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_layout_v3, parent, false)) :
                    new MyAdapter.AddViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.button_dialog_frag_layout, parent, false));
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (recordsList.get(position) instanceof AddDialog) {
            MyAdapter.AddViewHolder avh = (MyAdapter.AddViewHolder) holder;
            final AddDialog ad = (AddDialog) recordsList.get(position);
            if (ad.spinnerPosition != 0) {
                avh.cardioSpinner.setSelection(ad.spinnerPosition);
            }
            if (ad.date != null) {
                avh.dateInput.setText(ad.date);
            }
            if (ad.time != null) {
                avh.timeInput.setText(ad.time);
            }
        } else {
            final ListItem item = recordsList.get(position);
            MyAdapter.BaseViewHolder bHolder = (MyAdapter.BaseViewHolder) holder;
            String date = Utils.convertDateToString(Utils.convertStringToDate(item.date, DataModel.DATE_FORMAT), "MMM d");
            bHolder.date.setText(date);
            String cal = String.valueOf(item.calories) + " cals";
            bHolder.calories.setText(cal);
            bHolder.name.setText(item.cType);
            String distTime;
            if (bHolder.name.getText().equals(getResources().getString(R.string.jump_rope))) {
                distTime = item.time;
            } else if (bHolder.name.getText().equals(getResources().getString(R.string.swimming))){
                distTime = item.distance + " laps in " + item.time;
            } else {
                distTime = item.distance + " " + ((MainActivityAlt)getActivity()).distUnit + " in " + item.time;
            }
            bHolder.distance.setText(distTime);
            bHolder.icon.setImageResource(Utils.getCardioIcon(item.cType));
            int color = Utils.ColorUtils.getCardioColor(item.cType);

            Drawable circle = getResources().getDrawable(R.drawable.circle);
            Drawable semiCircleBanner = getResources().getDrawable(R.drawable.semi_circle_banner);

            semiCircleBanner.mutate();
            semiCircleBanner.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            bHolder.fl.setBackground(semiCircleBanner);

            circle.mutate();
            circle.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            bHolder.icon.setBackground(circle);
        }
    }

        @Override
        public int getItemCount() {
        return recordsList.size();
    }

        class BaseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView date, time, distance, calories, name;
        ImageView icon;
        FrameLayout fl;

        BaseViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            date = (TextView) itemView.findViewById(R.id.list_date_input);
            distance = (TextView) itemView.findViewById(R.id.list_miles_input);
            calories = (TextView) itemView.findViewById(R.id.list_calories_input);
            name = (TextView) itemView.findViewById(R.id.list_name_input);
            icon = (ImageView) itemView.findViewById(R.id.column_icon);
            fl = (FrameLayout) itemView.findViewById(R.id.frame_bg);
        }

        @Override
        public void onClick(View v) {
            mListener.graphIt(recordsList.get(getAdapterPosition()).date, recordsList.get(getAdapterPosition()).cType);
        }
    }

        class AddViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, Serializable {
        ViewGroup mainLayout;
        Spinner cardioSpinner;
        TextView dateInput;
        EditText timeInput, distInput, calsInput;
        TextInputLayout timeLayout;

        AddViewHolder(final View itemView) {
            super(itemView);
            mainLayout = (ViewGroup) itemView.findViewById(R.id.btn_dialog_frag_rel_layout);
            cardioSpinner = (Spinner) itemView.findViewById(R.id.cardio_type_spinner);
            dateInput = (TextView) itemView.findViewById(R.id.date_input);
            timeInput = (EditText) itemView.findViewById(R.id.time_input);
            distInput = (EditText) itemView.findViewById(R.id.miles_input);
            calsInput = (EditText) itemView.findViewById(R.id.calories_input);
            timeLayout = (TextInputLayout) itemView.findViewById(R.id.time_layout);
            itemView.findViewById(R.id.date_picker_button).setOnClickListener(this);
            itemView.findViewById(R.id.cancel_button).setOnClickListener(this);
            itemView.findViewById(R.id.confirm_button).setOnClickListener(this);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.cardio_types, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            cardioSpinner.setAdapter(adapter);
            cardioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    distInput.setEnabled(position != 6);
                    if (position == 11) {
                        ((TextView) itemView.findViewById(R.id.miles)).setText(getResources().getString(R.string.lap_label));
                    } else {
                        ((TextView) itemView.findViewById(R.id.miles)).setText(getResources().getString(R.string.distance_label));
                    }
                    GradientDrawable sd = (GradientDrawable)getResources().getDrawable(R.drawable.rounded_corner_background);
                    if (position != 0) {
                        sd.mutate();
                        int color = Utils.ColorUtils.getCardioColor(((TextView) view).getText().toString());
                        int chgColor = Utils.ColorUtils.changeAlpha(color, ALPHA_25);
                        sd.setColor(chgColor);
                        mainLayout.setBackground(sd);
                    } else {
                        sd.mutate();
                        sd.setColor(Color.parseColor("#e6e6e6"));
                        mainLayout.setBackground(sd);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.date_picker_button:
                    DialogFragment newFragment = new DatePickerFragment();
                    Bundle b = new Bundle();
                    b.putSerializable("date", this);
                    newFragment.setArguments(b);
                    newFragment.show(getActivity().getFragmentManager(), "datePicker");
                    break;
                case R.id.cancel_button:
                    recordsList.remove(0);
                    mainRecyclerView.getAdapter().notifyItemRemoved(0);
                    mListener.setInitDialogOpen(false);
                    break;
                case R.id.confirm_button:
                    int validation = validateFields();
                    if (validation > -1) {
                        highlightField(validation);
                    } else if (validateTimeFormattedProper()){
                        final HashMap<String, String> map = addInfoToMap();
                        long id = mListener.saveEnteredData(map, lapDataFromTimer);
                        onSaveEnteredData(id, map);
                    }
                    break;
            }
        }

        private boolean validateTimeFormattedProper() {
            final String time = timeInput.getText().toString();
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
                        timeLayout.setError("Seconds >= 60");
                        return false;
                    } else {
                        timeLayout.setErrorEnabled(false);
                        return true;
                    }
                case 3:
                    if (timeArray[2] >= 60) {
                        timeLayout.setError("Seconds >= 60");
                        return false;
                    } else if (timeArray[1] >= 60){
                        timeLayout.setError("Minutes >= 60");
                        return false;
                    } else {
                        timeLayout.setErrorEnabled(false);
                        return true;
                    }
                default:
                    timeLayout.setError("Something ain't right...");
                    return false;
            }
        }

        private int validateFields() {
            if (cardioSpinner.getSelectedItemPosition() == 0) return CARDIO_SPINNER;
            if (timeInput.getText().toString().isEmpty()) return TIME_EDITTEXT;
            if (distInput.isEnabled() && distInput.getText().toString().isEmpty()) return DIST_EDITTEXT;
            return -1;
        }

        @SuppressWarnings("deprecation")
        private void highlightField(int field) {
            final Drawable background = getResources().getDrawable(R.drawable.error_rectangle);
            switch (field) {
                case CARDIO_SPINNER:
                    cardioSpinner.setBackground(background);
                    break;
                case TIME_EDITTEXT:
                    timeInput.setBackground(background);
                    break;
                case DIST_EDITTEXT:
                    distInput.setBackground(background);
                    break;
            }
        }

        private HashMap<String, String> addInfoToMap() {
            HashMap<String, String> cardioData = new HashMap<>();
            cardioData.put(MainActivityAlt.DATE, dateInput.getText().toString());
            cardioData.put(MainActivityAlt.TIME, Utils.getTimeMillis(timeInput.getText().toString()));
            cardioData.put(MainActivityAlt.DISTANCE, distInput.getText().toString());
            cardioData.put(MainActivityAlt.CALORIES, calsInput.getText().toString());
            String cardio = (String) cardioSpinner.getSelectedItem();
            cardioData.put(MainActivityAlt.CARDIO_TYPE, cardio);
            return cardioData;
        }

        private String getTimeMillis() {
            final String time = timeInput.getText().toString();
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
    }
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        MyAdapter.AddViewHolder avh;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            avh = (MyAdapter.AddViewHolder) getArguments().getSerializable("date");
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
            String formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
            //String datePicked = month + "/" + day + "/" + year;
            // Do something with the date chosen by the user
            avh.dateInput.setText(formattedDate);
        }
    }

    static class ListSorter {

        //verified works
        static void sortAlphabetic(List<ListItem> recordsList, final int direction) {
            Collections.sort(recordsList, new Comparator<ListItem>() {
                @Override
                public int compare(ListItem o1, ListItem o2) {
                    if (direction == ASCENDING) {
                        return o1.cType.compareTo(o2.cType);
                    } else {
                        return o2.cType.compareTo(o1.cType);
                    }
                }
            });
        }

        static Comparator<ListItem> getAlphabeticComparator(int direction) {
            if (direction == ASCENDING) {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o1.cType.compareTo(o2.cType);
                    }
                };
            } else {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o2.cType.compareTo(o1.cType);
                    }
                };
            }
        }

        //needs some work to make dates sort properly
        static Comparator<ListItem> getDateComparator(int direction) {
            if (direction == ASCENDING) {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o1.date.compareTo(o2.date);
                    }
                };
            } else {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o2.date.compareTo(o1.date);
                    }
                };
            }
        }

        static Comparator<ListItem> getDistanceComparator(int direction) {
            if (direction == ASCENDING) {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return Math.round(o1.distance - o2.distance);
                    }
                };
            } else {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return Math.round(o2.distance - o1.distance);
                    }
                };
            }
        }

        static Comparator<ListItem> getTimeComparator(int direction) {
            if (direction == ASCENDING) {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        long t1, t2;
                        t1 = Long.valueOf(Utils.getTimeMillis(o1.time));
                        t2 = Long.valueOf(Utils.getTimeMillis(o2.time));
                        return (int) (t1 - t2);
                    }
                };
            } else {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        long t1, t2;
                        t1 = Long.valueOf(Utils.getTimeMillis(o1.time));
                        t2 = Long.valueOf(Utils.getTimeMillis(o2.time));
                        return (int) (t2 - t1);
                    }
                };
            }
        }

        static Comparator<ListItem> getCalorieComparator(int direction) {
            if (direction == ASCENDING) {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o1.calories - o2.calories;
                    }
                };
            } else {
                return new Comparator<ListItem>() {
                    @Override
                    public int compare(ListItem o1, ListItem o2) {
                        return o2.calories - o1.calories;
                    }
                };
            }
        }

        //verified works
        static void sortByDate(List<ListItem> recordsList, final int direction) {
            Collections.sort(recordsList, new Comparator<ListItem>() {
                @Override
                public int compare(ListItem o1, ListItem o2) {
                    if (direction == ASCENDING) {
                        return o1.date.compareTo(o2.date);
                    } else {
                        return o2.date.compareTo(o1.date);
                    }
                }
            });
        }
        //verified works
        static void sortByDistance(List<ListItem> recordsList, final int direction) {
            Collections.sort(recordsList, new Comparator<ListItem>(){
                @Override
                public int compare(ListItem o1, ListItem o2) {
                    if (direction == ASCENDING) {
                        return Math.round(o1.distance - o2.distance);
                    } else {
                        return Math.round(o2.distance - o1.distance);
                    }
                }
            });
        }
        //verified works
        static void sortByTime(List<ListItem> recordsList, final int direction) {
            Collections.sort(recordsList, new Comparator<ListItem>() {
                @Override
                public int compare(ListItem o1, ListItem o2) {
                    String t1, t2;
                    t1 = Utils.getTimeMillis(o1.time);
                    t2 = Utils.getTimeMillis(o2.time);
                    if (direction == ASCENDING) {
                        return t1.compareTo(t2);
                    } else {
                        return t2.compareTo(t1);
                    }
                }
            });
        }

        //verified works
        static void sortByCalories(List<ListItem> recordsList, final int direction) {
            Collections.sort(recordsList, new Comparator<ListItem>() {
                @Override
                public int compare(ListItem o1, ListItem o2) {
                    if (direction == ASCENDING) {
                        return o1.calories - o2.calories;
                    } else {
                        return o2.calories - o1.calories;
                    }
                }
            });
        }
    }

    //idea from http://www.codejava.net/java-core/collections/sorting-a-list-by-multiple-attributes-example
    public class ChainedComparator implements Comparator<ListItem>{
        private List<Comparator<ListItem>> listComparators;

        public ChainedComparator(List<Comparator<ListItem>> comparators) {
            this.listComparators = comparators;
        }

        @Override
        public int compare(ListItem o1, ListItem o2) {
            for (Comparator<ListItem> comparator : listComparators) {
                int result = comparator.compare(o1, o2);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        }
    }
}