package com.bolyndevelopment.owner.runlogger2;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity implements LogActivityDialogFragment.LogActivityListener{
    public static final String TAG = "MainActivity";

    List<ListItem> recordsList = new ArrayList<>();
    Handler handler = new Handler();
    RecyclerView recyclerView;
    MyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initRecyclerView();
        //addRandomData();
        queryForRecords();


        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });
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
                list.add(getTimeMillis(String.valueOf(min) + ":" + String.valueOf(sec)));
                list.add(String.valueOf(miles));
                list.add(String.valueOf(calories));
                list.add("Bike");
                DatabaseAccess.getInstance().addRecord(list);
            }
        }
    }

    private String getTimeMillis(String time) {

        String[] array = TextUtils.split(time, ":");
        int[] timeArray = new int[array.length];
        for (int x = 0; x<array.length;x++) {
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

    private void queryForRecords() {
        recordsList.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Cursor cursor = DatabaseAccess.getInstance().getRecords();
                cursor.moveToFirst();
                ListItem item;
                while (!cursor.isAfterLast()) {
                    item = new ListItem();
                    item.order = cursor.getInt(0);
                    item.calories = cursor.getInt(4);
                    item.distance = cursor.getFloat(3);
                    item.date = cursor.getString(1);
                    item.time = Utils.convertMillisToHms(cursor.getLong(2));
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
        recyclerView = (RecyclerView) findViewById(R.id.main_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        mAdapter = new MyAdapter();
        recyclerView.setAdapter(mAdapter);
    }

    private class ListItem {
        int order, calories;
        float distance;
        String date, time;
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                long id = DatabaseAccess.getInstance().addRecord(list);
                if (id > -1) {
                    ListItem item = new ListItem();
                    item.order = (int) id;
                    item.date = String.valueOf(list.get(0));
                    item.time = String.valueOf(list.get(1));
                    item.distance = Float.parseFloat((String)list.get(2));
                    item.calories = Integer.parseInt((String)list.get(3));
                    recordsList.add(item);
                    final int index = recordsList.indexOf(item);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyItemInserted(index);
                        }
                    });
                }
            }
        }).start();
    }

    public void graphIt(int position) {
        //startActivity(new Intent(this, Graphs.class));
        startActivity(new Intent(this, HelloGraph.class));
    }

    public void showDialog() {
        final LogActivityDialogFragment frag = new LogActivityDialogFragment();
        frag.show(getFragmentManager(), "dialog");
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.BaseViewHolder> {

        @Override
        public MyAdapter.BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new BaseViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(MyAdapter.BaseViewHolder holder, int position) {
            ListItem item = recordsList.get(position);
            holder.order.setText(String.valueOf(item.order));
            holder.date.setText(item.date);
            holder.time.setText(item.time);
            holder.distance.setText(String.valueOf(item.distance));
            holder.calories.setText(String.valueOf(item.calories));
        }

        @Override
        public int getItemCount() {
            return recordsList.size();
        }

        public class BaseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView order, date, time, distance, calories;

            public BaseViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                order = (TextView) itemView.findViewById(R.id.column_number);
                date = (TextView) itemView.findViewById(R.id.list_date_input);
                time = (TextView) itemView.findViewById(R.id.list_time_input);
                distance = (TextView) itemView.findViewById(R.id.list_miles_input);
                calories = (TextView) itemView.findViewById(R.id.list_calories_input);
            }

            @Override
            public void onClick(View v) {
                graphIt(getAdapterPosition());
            }
        }
    }
}
