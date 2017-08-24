package com.bolyndevelopment.owner.runlogger2;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityMainBinding;
import com.bolyndevelopment.owner.runlogger2.databinding.DialogFragLayoutBinding;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements LogActivityDialogFragment.LogActivityListener{
    public static final String TAG = "MainActivity";
    static final int CODE_TIMER = 100;

    List<ListItem> recordsList = new ArrayList<>();
    Handler handler = new Handler();
    MyAdapter mAdapter;
    ArrayList<String> lapDataFromTimer;
    ActivityMainBinding binder;
    boolean isAddDialogOpen = false;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_TIMER && resultCode == Activity.RESULT_OK) {
            final String totalTime = data.getStringExtra("totalTime");
            lapDataFromTimer = data.getStringArrayListExtra("list");
            Log.d("TEST", "lapdata size: " + lapDataFromTimer.size());
            //showDialog(totalTime);
            initAddDialog(totalTime);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        binder = DataBindingUtil.setContentView(this, R.layout.activity_main);

        setSupportActionBar(binder.toolbar);

        initRecyclerView();
        //addRandomData();
        queryForRecords();
        if (savedInstanceState != null) {
            isAddDialogOpen = savedInstanceState.getBoolean("isAddDialogOpen");
            if (isAddDialogOpen) {
                initAddDialog(null);
            }
        }

        binder.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //showDialog(null);
                Log.d(TAG, "dialogopen: " + isAddDialogOpen);
                if (!isAddDialogOpen) {
                    initAddDialog(null);
                    isAddDialogOpen = true;
                }
            }
        });
        binder.fabTimeRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getBaseContext(), TimerActivity.class), CODE_TIMER);
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isAddDialogOpen", isAddDialogOpen);
        super.onSaveInstanceState(outState);
    }

    private void initAddDialog(@Nullable String time) {
        AddDialog ad = new AddDialog();
        if (time != null) {
            ad.time = time;
        }
        ad.date = Utils.convertDateToString(new Date(), "MM/dd/yyyy");
        Cursor c = DatabaseAccess.getInstance().rawQuery("select date, cardio_type from Data limit 1", null);
        c.moveToFirst();
        String type = null;
        if (c.getCount() > 0) {
            type = c.getString(1);
        }
        if (type != null) {
            final List<String> list = Arrays.asList(getResources().getStringArray(R.array.cardio_types));
            ad.spinnerPosition = list.indexOf(type);
        }
        recordsList.add(0, ad);
        binder.mainRecyclerview.getAdapter().notifyItemInserted(0);
        binder.mainRecyclerview.scrollToPosition(0);
        isAddDialogOpen = true;
    }

    private void addRandomData() {
        for (int y=1; y<8; y++) {
            for (int x = 1; x < 31; x += 3) {
                String date = String.format(Locale.US, "%02d/%02d/%04d", y, x, 2017);
                Random random = new Random();
                int min = random.nextInt((60 - 1) + 1) + 1;
                int sec = random.nextInt((60 - 1) + 1) + 1;
                int miles = random.nextInt((15 - 1) + 1) + 1;
                int calories = random.nextInt((1000 - 100) + 1) + 100;
                ArrayList<String> list = new ArrayList<>();
                list.add(date);
                list.add(Utils.getTimeStringMillis(String.valueOf(min) + ":" + String.valueOf(sec)));
                list.add(String.valueOf(miles));
                list.add(String.valueOf(calories));
                list.add("Bike");
                long l = DatabaseAccess.getInstance().addRecords(list, null);
                Log.d(TAG, "Row: " + l);
            }
        }
    }

    private void queryForRecords() {
        recordsList.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Cursor cursor = DatabaseAccess.getInstance().getAllRecords();
                cursor.moveToFirst();
                ListItem item;
                while (!cursor.isAfterLast()) {
                    item = new ListItem();
                    //item.order = cursor.getInt(0);
                    item.cType = cursor.getString(4);
                    item.calories = cursor.getInt(3);
                    item.distance = cursor.getFloat(2);
                    item.date = cursor.getString(0);
                    item.time = Utils.convertMillisToHms(cursor.getLong(1));
                    recordsList.add(item);
                    cursor.moveToNext();
                }
                cursor.close();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();

    }

    private void initRecyclerView() {
        binder.mainRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binder.mainRecyclerview.setHasFixedSize(true);
        mAdapter = new MyAdapter();
        binder.mainRecyclerview.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.graph_it) {

        } else {
            startActivity(new Intent(MainActivity.this, AndroidDatabaseManager.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogPositiveClick(Bundle bundle) {
        final ArrayList<String> list = bundle.getStringArrayList("data");
        if (list != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long id = DatabaseAccess.getInstance().addRecords(list, lapDataFromTimer);
                    if (id > -1) {
                        ListItem item = new ListItem();
                        //item.order = (int) id;
                        item.date = list.get(0);
                        item.time = Utils.convertMillisToHms(Long.parseLong(list.get(1)));
                        item.distance = Float.parseFloat(list.get(2));
                        item.calories = list.get(3).equals("") ? 0 : Integer.parseInt(list.get(3));
                        item.cType = list.get(4);
                        recordsList.add(0, item);
                        //final int index = recordsList.indexOf(item);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyItemInserted(0);
                            }
                        });
                    }
                }
            }).start();
        } else {
            //alert to their being a problem
        }
        //new DatabaseBackup(this).dumpBackupFile();
    }

    private void saveEnteredData(final ArrayList<String> list) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long id = DatabaseAccess.getInstance().addRecords(list, lapDataFromTimer);
                    if (id > -1) {
                        ListItem item = new ListItem();
                        //item.order = (int) id;
                        item.date = list.get(0);
                        item.time = Utils.convertMillisToHms(Long.parseLong(list.get(1)));
                        item.distance = Float.parseFloat(list.get(2));
                        item.calories = list.get(3).equals("") ? 0 : Integer.parseInt(list.get(3));
                        item.cType = list.get(4);
                        recordsList.add(0, item);
                        recordsList.remove(1);
                        //final int index = recordsList.indexOf(item);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyItemChanged(0);
                                binder.mainRecyclerview.scrollToPosition(0);
                            }
                        });
                    }
                }
            }).start();
        //new DatabaseBackup(this).dumpBackupFile();
    }

    public void graphIt(String date) {
        Intent i = new Intent(this, HelloGraph.class);
        i.putExtra("date", date);
        startActivity(i);
    }

    public void showDialog(@Nullable String time) {
        Cursor c = DatabaseAccess.getInstance().rawQuery("select date, cardio_type from Data limit 1", null);
        c.moveToFirst();
        Bundle b = new Bundle();
        if (c.getCount() > 0) {
            b.putString("type", c.getString(1));
        }
        if (time != null) {
            b.putString("totalTime", time);
        }
        //if (lapData != null) {
            //b.putStringArrayList("lapData", lapData);
        //}
        c.close();
        final LogActivityDialogFragment frag = new LogActivityDialogFragment();
        frag.setArguments(b);
        frag.show(getFragmentManager(), "dialog");
    }

    public void sort(View view) {
        Snackbar.make(binder.getRoot(), "Press: " + ((TextView)view).getText().toString(), Snackbar.LENGTH_SHORT).show();
        switch (view.getId()) {

            case R.id.main_date_tv:
            case R.id.main_time_tv:
            case R.id.main_dist_tv:
            case R.id.main_cals_tv:
            case R.id.main_icon_tv:

        }
    }

    private class ListItem {
        int calories;
        float distance;
        String date = null, time = null, cType;
    }

    private class AddDialog extends ListItem {
        int spinnerPosition = 0;


    }

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
            return viewType == LIST_ITEM ? new BaseViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_layout, parent, false)) :
                    new AddViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.button_dialog_frag_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (recordsList.get(position) instanceof AddDialog) {
                AddViewHolder avh = (AddViewHolder) holder;
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
                BaseViewHolder bHolder = (BaseViewHolder) holder;
                bHolder.date.setText(item.date);
                bHolder.time.setText(item.time);
                bHolder.distance.setText(String.valueOf(item.distance));
                bHolder.calories.setText(String.valueOf(item.calories));
            }
        }

        @Override
        public int getItemCount() {
            return recordsList.size();
        }

        class BaseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView date, time, distance, calories;
            ImageView icon;

            BaseViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                date = (TextView) itemView.findViewById(R.id.list_date_input);
                time = (TextView) itemView.findViewById(R.id.list_time_input);
                distance = (TextView) itemView.findViewById(R.id.list_miles_input);
                calories = (TextView) itemView.findViewById(R.id.list_calories_input);
                icon = (ImageView) itemView.findViewById(R.id.column_icon);
            }

            @Override
            public void onClick(View v) {
                graphIt(date.getText().toString());
            }
        }

        class AddViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, Serializable{
            Spinner cardioSpinner;
            TextView dateInput;
            EditText timeInput, distInput, calsInput;
            TextInputLayout timeLayout;

            public AddViewHolder(View itemView) {
                super(itemView);
                cardioSpinner = (Spinner) itemView.findViewById(R.id.cardio_type_spinner);
                dateInput = (TextView) itemView.findViewById(R.id.date_input);
                timeInput = (EditText) itemView.findViewById(R.id.time_input);
                distInput = (EditText) itemView.findViewById(R.id.miles_input);
                calsInput = (EditText) itemView.findViewById(R.id.calories_input);
                timeLayout = (TextInputLayout) itemView.findViewById(R.id.time_layout);
                itemView.findViewById(R.id.date_picker_button).setOnClickListener(this);
                itemView.findViewById(R.id.cancel_button).setOnClickListener(this);
                itemView.findViewById(R.id.confirm_button).setOnClickListener(this);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getBaseContext(),
                        R.array.cardio_types, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                cardioSpinner.setAdapter(adapter);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.date_picker_button:
                        DialogFragment newFragment = new DatePickerFragment();
                        Bundle b = new Bundle();
                        b.putSerializable("date", this);
                        newFragment.setArguments(b);
                        newFragment.show(getFragmentManager(), "datePicker");
                        break;
                    case R.id.cancel_button:
                        //Toast.makeText(getBaseContext(), "Cancel button", Toast.LENGTH_SHORT).show();
                        recordsList.remove(0);
                        binder.mainRecyclerview.getAdapter().notifyItemRemoved(0);
                        isAddDialogOpen = false;
                        break;
                    case R.id.confirm_button:
                        //Toast.makeText(getBaseContext(), "Confirm button", Toast.LENGTH_SHORT).show();
                        int validation = validateFields();
                        if (validation > -1) {
                            highlightField(validation);
                        } else if (validateTimeFormattedProper()){
                            saveEnteredData(addInfoToArray());
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
                if (timeInput.getText().toString().equals("")) return TIME_EDITTEXT;
                if (distInput.getText().toString().equals("")) return DIST_EDITTEXT;
                return -1;
            }

            private void highlightField(int field) {
                Log.d("TEST", "highlightField");
                final Drawable background = getResources().getDrawable(R.drawable.error_rectangle);
                switch (field) {
                    case CARDIO_SPINNER:
                        Log.d("TEST", "CardioSpinner");
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

            private ArrayList<String> addInfoToArray() {
                ArrayList<String> runData = new ArrayList<>();
                runData.add(dateInput.getText().toString());
                runData.add(getTimeMillis());
                runData.add(distInput.getText().toString());
                runData.add(calsInput.getText().toString());
                String cardio = (String)cardioSpinner.getSelectedItem();
                runData.add(cardio); //how we'll add in the cardio type
                return runData;
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
            String formattedDate = String.format(Locale.US, "%02d/%02d/%04d", month, day, year);
            Log.d("LADF", "formatted date: " + formattedDate);
            //String datePicked = month + "/" + day + "/" + year;
            // Do something with the date chosen by the user
            avh.dateInput.setText(formattedDate);
            //((LogActivityDialogFragment)getFragmentManager().findFragmentByTag("dialog")).setDateInput(formattedDate);
        }
    }
}
