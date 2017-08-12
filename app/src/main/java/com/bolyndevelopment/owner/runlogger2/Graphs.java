package com.bolyndevelopment.owner.runlogger2;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Graphs extends AppCompatActivity {
    public static final String TAG = "Graphs";
    public static final int DURATION = 1;
    public static final int DISTANCE = 2;
    public static final int CALORIES_BURNED = 3;
    GraphView graph;
    Spinner spinner;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graphs);
        handler = new Handler();
        initSpinner();

        graph = (GraphView) findViewById(R.id.graph);

    }

    private void initSpinner() {
        final Spinner spinner = (Spinner) findViewById(R.id.spinner_data);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.graph_array_list, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String child = (String)parent.getItemAtPosition(position);
                switch (position) {
                    case 1:
                        getDuration();
                        break;
                    case 2:
                        getDistance();
                        break;
                    case 3:
                        getCaloriesBurned();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void getDuration(/*timeFrame variable */) {
        Cursor c = DatabaseAccess.getInstance().rawQuery("select date, time from Data order by date asc", null);
        c.moveToFirst();
        ArrayList<DataPoint> dpArray = new ArrayList<>();
        while (!c.isAfterLast()) {
            Log.d(TAG, "0: " + c.getString(0) + ", 1: " + c.getLong(1));
            Date date = convertStringToDate(c.getString(0), "MM/dd/yyyy");
            int minutes = Utils.convertMillisToIntMinutes(c.getLong(1));
            DataPoint dp = new DataPoint(date, minutes);
            dpArray.add(dp);
            c.moveToNext();
        }
        c.close();
        DataPoint[] finalArray = convertToArray(dpArray);
        /*
        DataPoint[] finalArray = new DataPoint[dpArray.size()];
        for (int x = 0; x < dpArray.size(); x++) {
            finalArray[x] = dpArray.get(x);
        }
        */
        renderGraph(finalArray, DURATION, null, null);
        calcAverageSeries(dpArray);
    }

    private void getDistance(/*timeFrame variable? */) {
        Cursor c = DatabaseAccess.getInstance().rawQuery("select date, miles from Data order by date asc", null);
        c.moveToFirst();
        ArrayList<DataPoint> dpArray = new ArrayList<>();
        while (!c.isAfterLast()) {
            Log.d(TAG, "0: " + c.getString(0) + ", 1: " + c.getLong(1));
            Date date = convertStringToDate(c.getString(0), "MM/dd/yyyy");
            DataPoint dp = new DataPoint(date, c.getFloat(1));
            dpArray.add(dp);
            c.moveToNext();
        }
        c.close();
        DataPoint[] finalArray = new DataPoint[dpArray.size()];
        for (int x = 0; x < dpArray.size(); x++) {
            finalArray[x] = dpArray.get(x);
        }
        renderGraph(finalArray, DISTANCE, null, null);
        calcAverageSeries(dpArray);
    }

    private void getCaloriesBurned(/*timeFrame variable? */) {
        Cursor c = DatabaseAccess.getInstance().rawQuery("select date, calories from Data order by date asc", null);
        c.moveToFirst();
        ArrayList<DataPoint> dpArray = new ArrayList<>();
        while (!c.isAfterLast()) {
            Log.d(TAG, "0: " + c.getString(0) + ", 1: " + c.getLong(1));
            Date date = convertStringToDate(c.getString(0), "MM/dd/yyyy");
            DataPoint dp = new DataPoint(date, c.getFloat(1));
            dpArray.add(dp);
            c.moveToNext();
        }
        c.close();
        DataPoint[] finalArray = new DataPoint[dpArray.size()];
        for (int x = 0; x < dpArray.size(); x++) {
            finalArray[x] = dpArray.get(x);
        }
        renderGraph(finalArray, CALORIES_BURNED, null, null);
        calcAverageSeries(dpArray);
    }

    private void calcAverageSeries(final ArrayList<DataPoint> dataPoints) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<DataPoint> averages = new ArrayList<>();
                int remainder = dataPoints.size() % 3;
                for (int x = 0; x < dataPoints.size() - remainder; x += 3) {
                    double ave = (dataPoints.get(x).getY() + dataPoints.get(x + 1).getY() + dataPoints.get(x + 2).getY()) / 3;
                    averages.add(new DataPoint(dataPoints.get(x + 1).getX(), ave));
                }
                DataPoint[] finalArray = convertToArray(averages);
                final LineGraphSeries<DataPoint> series = new LineGraphSeries<>(finalArray);
                series.setTitle("Average");
                series.setColor(Color.RED);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        graph.addSeries(series);
                    }
                });
            }
        }).start();

    }

    private DataPoint[] convertToArray(ArrayList<DataPoint> inList) {
        final DataPoint[] finalArray = new DataPoint[inList.size()];
        for (int x = 0; x < inList.size(); x++) {
            finalArray[x] = inList.get(x);
        }
        return finalArray;
    }

    public void renderGraph(DataPoint[] dataPoints, int type, @Nullable Date beginDate, @Nullable Date endDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        Date d1 = calendar.getTime();
        calendar.add(Calendar.MONTH, -2);
        Date d2 = calendar.getTime();

        graph.removeAllSeries();
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(dataPoints);

        // set date label formatter
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        graph.getViewport().setScalable(true);
        graph.getViewport().setMinX(d2.getTime());
        graph.getViewport().setMaxX(d1.getTime());
        graph.getViewport().setXAxisBoundsManual(true);

        String title = null;
        switch (type) {
            case DURATION:
                title = "Duration";
                break;
            case DISTANCE:
                title = "Distance";
                break;
            case CALORIES_BURNED:
                title = "Calories Burned";
                break;
        }
        graph.addSeries(series);

        series.setTitle(title);
        //series2.setTitle("bar");
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setBackgroundColor(Color.WHITE);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
    }

    public Date convertStringToDate(String inDate) throws ParseException{
        DateFormat formatter;
        Date outDate;
        String formattedDate = inDate.replace("/", "-");
        formatter = new SimpleDateFormat("yyyy-MM-dd");
        outDate = formatter.parse(formattedDate);
        Log.d(TAG, "Date " + outDate);
        return outDate;
    }

    @Nullable
    static Date convertStringToDate(String inDate, String format) {
        DateFormat formatter;
        Date outDate = null;
        //String formattedDate = inDate.replace("/", "-");
        formatter = new SimpleDateFormat(format, Locale.US);
        try {
            outDate = formatter.parse(inDate);
        } catch (ParseException pe) {

        }
        return outDate;
    }

    static String convertDateToString(Date inDate, String format) {
        return new SimpleDateFormat(format, Locale.US).format(inDate);
    }

    public void onClick(View view) {
    }
}
