package com.bolyndevelopment.owner.runlogger2;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
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
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements LogActivityDialogFragment.LogActivityListener{
    public static final String TAG = "MainActivity";
    static final int CODE_TIMER = 100;

    List<ListItem> recordsList = new ArrayList<>();
    Handler handler = new Handler();
    RecyclerView recyclerView;
    MyAdapter mAdapter;
    ArrayList<String> lapDataFromTimer;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_TIMER && resultCode == Activity.RESULT_OK) {
            final String totalTime = data.getStringExtra("totalTime");
            lapDataFromTimer = data.getStringArrayListExtra("list");
            Log.d("TEST", "lapdata size: " + lapDataFromTimer.size());
            showDialog(totalTime);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        initRecyclerView();
        addRandomData();
        queryForRecords();

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(null);
            }
        });
        findViewById(R.id.fab_time_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getBaseContext(), TimerActivity.class), CODE_TIMER);
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
        recyclerView = (RecyclerView) findViewById(R.id.main_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        mAdapter = new MyAdapter();
        recyclerView.setAdapter(mAdapter);
    }

    private class ListItem {
        int calories;
        float distance;
        String date, time, cType;
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
        } else {
            //alert to their being a problem
        }
    }

    public void graphIt(/*int position*/) {
        startActivity(new Intent(this, HelloGraph.class));
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

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.BaseViewHolder> {

        @Override
        public MyAdapter.BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new BaseViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(MyAdapter.BaseViewHolder holder, int position) {
            ListItem item = recordsList.get(position);
            //holder.order.setText(String.valueOf(item.order));
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
            ImageView icon;

            public BaseViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                //order = (TextView) itemView.findViewById(R.id.column_number);
                date = (TextView) itemView.findViewById(R.id.list_date_input);
                time = (TextView) itemView.findViewById(R.id.list_time_input);
                distance = (TextView) itemView.findViewById(R.id.list_miles_input);
                calories = (TextView) itemView.findViewById(R.id.list_calories_input);
                icon = (ImageView) itemView.findViewById(R.id.column_icon);
            }

            @Override
            public void onClick(View v) {
                graphIt(/*getAdapterPosition()*/);
            }
        }
    }
}
