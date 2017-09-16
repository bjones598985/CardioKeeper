package com.bolyndevelopment.owner.runlogger2;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityGraphsBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import es.dmoral.toasty.Toasty;
import lecho.lib.hellocharts.animation.ChartAnimationListener;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ComboLineColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.ComboLineColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;

public class HelloGraph extends AppCompatActivity {
    //Created by Bobby Jones on 8/11/2017
    public static final String TAG = "HelloGraph";

    boolean initialDataLoaded = false;
    int baseAverage;
    int zoomLevel;
    int columnColor;
    int lineColor;
    int axisColor;
    boolean isStacked;

    int[] colors = new int[]{Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff"),
            Color.parseColor("#002b80"), Color.parseColor("#b3ccff"), Color.parseColor("#003cb3"), Color.parseColor("#80aaff"), Color.parseColor("#004de6"), Color.parseColor("#4d88ff")};

    Handler handler = new Handler();
    List<Float> rawData;
    List<AxisValue> axisValues;
    ColumnChartData columnData, stackedColumnData;
    LineChartData lineData;
    ActivityGraphsBinding binding;

    boolean isDataTypeSet = false, isCardioTypeSet = false;
    int dataType, timeFrame = 1;
    String cardioType, yAxisLabel, initialDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_graphs);



        initVars();
        initSpinners();
        initGraph();
        binding.runQueryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentChart();
            }
        });

        initialDate = getIntent().getStringExtra("date");
        Log.d(TAG, "Initial date: " + initialDate);
        String cType = getIntent().getStringExtra("cType");
        if (initialDate != null) {
            overrideInitVars(cType);
            //presentGeneralStatsChart(query, new String[]{date});
            presentChart();
        }
    }

    private void overrideInitVars(String cType) {
        int i = Arrays.asList(getResources().getStringArray(R.array.cardio_types)).indexOf(cType);
        binding.spinnerCardioType.setSelection(i);
        cardioType = cType;
        binding.spinnerData.setSelection(2);
        dataType = 2;
        binding.spinnerTimeFrame.setSelection(0);
        isCardioTypeSet = true;
        isDataTypeSet = true;
    }


    private void initVars() {

        baseAverage = 3;

        zoomLevel = 2;

        columnColor = Color.WHITE;

        lineColor = getResources().getColor(R.color.colorAccent);

        axisColor = Color.WHITE;

        timeFrame = 1;

        axisValues = new ArrayList<>();

        rawData = new ArrayList<>();

        columnData = new ColumnChartData();
        columnData.setStacked(true);

        lineData = new LineChartData();

        //not being used
        stackedColumnData = new ColumnChartData();
    }

    private void initSpinners() {
        final ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(this, R.array.graph_data_type, R.layout.spinner_item);
        dataAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        binding.spinnerData.setAdapter(dataAdapter);
        binding.spinnerData.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dataType = position;
                isDataTypeSet = position != 0;
                isStacked = position == 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing, we don't care
            }
        });

        final ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(this, R.array.time_frame, R.layout.spinner_item);
        timeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        binding.spinnerTimeFrame.setAdapter(timeAdapter);
        binding.spinnerTimeFrame.setSelection(1); //default
        binding.spinnerTimeFrame.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                timeFrame = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing, we don't care
            }
        });

        final ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this, R.array.cardio_types, R.layout.spinner_item);
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        binding.spinnerCardioType.setAdapter(typeAdapter);
        binding.spinnerCardioType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cardioType = ((TextView)view).getText().toString();
                isCardioTypeSet = position != 0;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing, we don't care
            }
        });
    }

    //validate that cardioType and dataType have been chosen before trying to display the chart
    private boolean canPresentChart() {
        return isDataTypeSet && isCardioTypeSet;
    }

    private void initGraph() {
        binding.helloGraph.setValueTouchEnabled(true);
        binding.helloGraph.setOnValueTouchListener(new ComboTouchListener());
        binding.helloGraph.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
    }

    private void presentChart() {
        if (canPresentChart()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String query = getQuery();
                    Log.d(TAG, "Query: " + query);
                    Cursor c = DatabaseAccess.getInstance().rawQuery(query, null);
                    dumpCursorToScreen(c);
                    if (initialDataLoaded) {
                        //updateColumnData(c);
                        updateTrialRun(c);
                    } else {
                        generateColumnData(c);
                        initialDataLoaded = true;
                    }
                    setAxesAndDisplay();
                }
            }).start();
        }
    }

    private String getQuery() {
        String query = null;
        String midQueryPart = buildAndRetrieveMidQueryPart();
        switch (dataType) {
            case 1:
                query = "select Data.date, round(Lap.time * 1.0 / 60000) as Mins, Lap.lap_num from Lap inner join Data on Data._id=Lap.workout_id where cardio_type=" + midQueryPart + " order by Data.date asc";
                yAxisLabel = "Minutes";
                break;
            case 2:
                query = "select date, round(time * 1.0 / 60000) as Mins from Data where cardio_type=" + midQueryPart + " order by date asc";
                yAxisLabel = "Minutes";
                break;
            case 3:
                query = "select date, distance from Data where cardio_type=" + midQueryPart + " order by date asc";
                yAxisLabel = "Distance";
                break;
            case 4:
                query = "select date, calories from Data where cardio_type=" + midQueryPart + " order by date asc";
                yAxisLabel = "Calories";
                break;
            case 5:
                query = "select date, distance / round(time * 1.0 / 3600000) from Data where cardio_type=" + midQueryPart + " order by date asc";
                yAxisLabel = "Speed";
                break;
            case 6:
                query = "select date, calories / round(time * 1.0 / 3600000) from Data where cardio_type=" + midQueryPart + " order by date asc";
                yAxisLabel = "Calories / Hour";
                break;
            case 7:
                query = "select date, calories / distance from Data where cardio_type=" + midQueryPart + " order by date asc";
                yAxisLabel = "Calories / Distance";
                break;
        }
        Log.d(TAG, "Line 270 - Query: " + query);
        return query;
    }

    private String buildAndRetrieveMidQueryPart() {
        String endOfQuery = null;
        String ex = "\'" + cardioType + "\'";
        String today, secondDate;
        if (initialDate != null && !initialDataLoaded) {
            Log.d(TAG, "build -  initaldate not null");
            today = initialDate;
            secondDate = initialDate;
        } else {
            Log.d(TAG, "build -  initaldate null");
            today = Utils.convertDateToString(new Date(), Utils.DB_DATE_FORMAT);
            secondDate = getSecondDate();
        }

        if (dataType == 1) {
            endOfQuery = ex + " and Data.date between \'" + secondDate + "\' and \'" + today + "\'";
        } else {
            endOfQuery = ex + " and date between '" + secondDate + "\' and \'" + today + "\'";
        }
        Log.d(TAG, "end of query: " + endOfQuery);
        return endOfQuery;
    }

    private String getSecondDate() {
        Calendar cal = Calendar.getInstance();
        switch (timeFrame) {
            case 1:
                cal.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case 2:
                cal.add(Calendar.MONTH, -1);
                break;
            case 3:
                cal.add(Calendar.MONTH, -3);
                break;
            case 4:
                cal.add(Calendar.MONTH, -6);
                break;
            case 5:
                cal.set(Calendar.MONTH, 1);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                break;
            default:
        }
        return Utils.convertDateToString(cal.getTime(), Utils.DB_DATE_FORMAT);
    }

    /*
    private void presentGeneralStatsChart(@NonNull final String query, @Nullable final String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor c = DatabaseAccess.getInstance().rawQuery(query, args);
                if (initialDataLoaded) {
                    updateColumnData(c);
                } else {
                    generateColumnData(c);
                    initialDataLoaded = true;
                }
                setAxesAndDisplay();
            }
        }).start();
    }
    */

    private void generateColumnData(Cursor results) {
        results.moveToFirst();
        rawData.clear();
        List<SubcolumnValue> subcolumnValues;
        List<Column> columns = new ArrayList<>();
        axisValues.clear();
        int colCount = 0;
        int count = 0;
        while (!results.isAfterLast()) {
            String date = results.getString(0);
            subcolumnValues = new ArrayList<>();
            setAxisValues(colCount, results.getString(0));
            while (!results.isAfterLast() && date.equals(results.getString(0))) {
                float raw = results.getFloat(1);
                subcolumnValues.add(new SubcolumnValue(raw, colors[count]));
                results.moveToNext();
                count++;
            }
            Column col = new Column(subcolumnValues);
            col.setHasLabels(true);
            col.setHasLabelsOnlyForSelected(true);
            columns.add(col);
            results.moveToNext();
            colCount++;
        }
        results.close();
        columnData.setColumns(columns);
    }

    /*
    private void updateColumnData(Cursor results) {
        int newColCount = getNewColumnCount(results);
        Log.d(TAG, "new col count: " + newColCount);
        final ComboLineColumnChartData oldData = binding.helloGraph.getComboLineColumnChartData();
        final int cursorCount = results.getCount();
        int colCount = 0;
        results.moveToFirst();
        axisValues.clear();
        List<Column> columns = oldData.getColumnChartData().getColumns();
        final int oldColumnCount = columns.size();

        if (newColCount <= oldColumnCount) {
            rawData.clear();
            while (!results.isAfterLast()) {
                int count = 0;
                String date = results.getString(0);
                setAxisValues(colCount, results.getString(0));
                while (!results.isAfterLast() && date.equals(results.getString(0))) {
                    float raw;
                    if (query.equals(QueryStrings.DURATION_QUERY) || query.equals(QueryStrings.LAPS_QUERY_ALT)) {
                        raw = Utils.convertMillisToFloatMinutes(results.getLong(1));
                    } else {
                        raw = results.getFloat(1);
                    }
                    columns.get(colCount).getValues().get(count).setTarget(raw).setColor(colors[count]);
                    if (!isStacked) { //if we're just doing reg columns, delete the stacked values
                        int subsSize = columns.get(colCount).getValues().size();
                        for (int k = 1; k < subsSize; k++) {
                            columns.get(colCount).getValues().get(k).setTarget(0);//.setColor(Color.TRANSPARENT);
                        }
                    }
                    results.moveToNext();
                    count++;
                }
                if (columns.get(colCount).getValues().size() > count) {
                    for (int z = count; z < columns.get(colCount).getValues().size(); z++) {
                        columns.get(colCount).getValues().get(z).setTarget(0);//.setColor(Color.TRANSPARENT);
                    }
                }
                colCount++;
                Log.d(TAG, "ColCount: " + colCount);
            }
            for (int q = newColCount; q < oldColumnCount; q++) {
                if (!isStacked) {
                    int subSize = columns.get(q).getValues().size();
                    for (int l = 0; l < subSize; l++) {
                        columns.get(q).getValues().get(l).setTarget(0);
                    }
                } else {
                    columns.get(q).getValues().get(0).setTarget(0);
                }
            }
        } else {
            rawData.clear();
            //for (int j = 0; j < oldColumnCount; j++) {
            Log.e(TAG, "else colCount: " + colCount);
                while(!results.isAfterLast() && colCount < oldColumnCount) {
                //if (!results.isAfterLast()) {
                    int sublistSize = columns.get(colCount).getValues().size();
                    int count = 0;
                    String date = results.getString(0);
                    setAxisValues(colCount, results.getString(0));
                    while (!results.isAfterLast() && date.equals(results.getString(0))) {
                        float raw;
                        if (query.equals(QueryStrings.DURATION_QUERY) || query.equals(QueryStrings.LAPS_QUERY_ALT)) {
                            raw = Utils.convertMillisToFloatMinutes(results.getLong(1));
                        } else {
                            raw = results.getFloat(1);
                        }
                        if (count >= sublistSize) {
                            columns.get(colCount).getValues().add(new SubcolumnValue(0).setTarget(raw).setColor(colors[count]));
                        } else {
                            columns.get(colCount).getValues().get(count).setTarget(raw).setColor(colors[count]);
                        }
                        if (!isStacked) { //if we're just doing reg columns, delete the stacked values
                            int subsSize = columns.get(colCount).getValues().size();
                            for (int k = 1; k < subsSize; k++) {
                                columns.get(colCount).getValues().get(k).setTarget(0);//.setColor(Color.TRANSPARENT);
                            }
                        }
                        results.moveToNext();
                        count++;
                    }
                    //somehow a vestige of this color is remaining when we shring the values to 0
                    if (columns.get(colCount).getValues().size() > count) {
                        for (int z = count; z < columns.get(colCount).getValues().size(); z++) {
                            Log.d(TAG, "colcount: " + colCount + ", count: " + count + ", z: " + z);
                            columns.get(colCount).getValues().get(z).setTarget(0);//.setColor(Color.TRANSPARENT);
                        }
                    }
                    colCount++;
                    Log.d(TAG, "ColumnCount: " + colCount);
                //}
            }
            while (!results.isAfterLast()) {
                int count = 0;
                String date = results.getString(0);
                setAxisValues(colCount, results.getString(0));
                List<SubcolumnValue> sublist = new ArrayList<>();
                while (!results.isAfterLast() && date.equals(results.getString(0))) {
                    float raw;
                    if (query.equals(QueryStrings.DURATION_QUERY) || query.equals(QueryStrings.LAPS_QUERY_ALT)) {
                        raw = Utils.convertMillisToFloatMinutes(results.getLong(1));
                    } else {
                        raw = results.getFloat(1);
                    }
                    sublist.add(new SubcolumnValue(0).setTarget(raw).setColor(colors[count]));

                    results.moveToNext();
                    count++;
                }
                columns.add(new Column(sublist).setHasLabelsOnlyForSelected(true));
                colCount++;
            }
        }
        //generateAverageLine();
        //updateLineData(oldColumnCount, cursorCount);
        final int colCountFinal = colCount;
        handler.post(new Runnable() {
            @Override
            public void run() {
                binding.helloGraph.setDataAnimationListener(new ChartAnimationListener() {
                    @Override
                    public void onAnimationStarted() {

                    }

                    @Override
                    public void onAnimationFinished() {
                        binding.helloGraph.setDataAnimationListener(null);
                        if (oldColumnCount > colCountFinal) {
                            for (int i = oldColumnCount - 1; i >= colCountFinal; i--) {
                                binding.helloGraph.getComboLineColumnChartData().getColumnChartData().getColumns().remove(i);
                            }
                        }
                        //see if we can't animate the viewport here
                        binding.helloGraph.startDataAnimation(2000);
                    }
                });
                binding.helloGraph.startDataAnimation(1000);
            }
        });
    }
    */

    private void updateTrialRun(Cursor results) {
        boolean isThereNewData = true;
        int newColCount = getNewColumnCount(results);
        Log.d(TAG, "562 - new col count: " + newColCount);
        final ComboLineColumnChartData oldData = binding.helloGraph.getComboLineColumnChartData();
        int colCount = 0;
        results.moveToFirst();

        final List<Column> columns = oldData.getColumnChartData().getColumns();
        int sublistSize = 0;
        if (columns.size() > 0) {
            //sublistSize = columns.get(colCount).getValues().size();
            Log.d(TAG, "Line 572 - sublistsize: " + sublistSize);
        }
        int oldColumnCount = columns.size();
        Log.d(TAG, "570 - oldColCount: " + oldColumnCount);

        rawData.clear();
        if (newColCount == 0) {
            isThereNewData = false;
            Log.d(TAG, "newcolcount = 0");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toasty.info(getBaseContext(), "Uh oh, your search turned up no results", Toast.LENGTH_LONG, true).show();
                }
            });
        }

        if (newColCount > 0) {
            isThereNewData = true;
            Log.d(TAG, "newcolcount > 0");
            if (oldColumnCount == 0) {
                Log.d(TAG, "oldColCount = 0");
                columns.add(new Column());
                oldColumnCount = 1;
            }
            axisValues.clear();

            do {
                int subColCount = 0;
                String date = results.getString(0);
                setAxisValues(colCount, results.getString(0));
                sublistSize = columns.get(colCount).getValues().size();
                while (!results.isAfterLast() && date.equals(results.getString(0))) {
                    float raw = results.getFloat(1);
                    Log.d(TAG, "608 - Date: " + results.getString(0) + ", Raw: " + raw);
                    if (subColCount >= sublistSize) {
                        Log.d(TAG, "610 - subColCount >= sublistsize");
                        columns.get(colCount).getValues().add(new SubcolumnValue().setValue(0).setTarget(raw).setColor(colors[subColCount]));
                    } else {
                        columns.get(colCount).getValues().get(subColCount).setTarget(raw).setColor(colors[subColCount]);
                    }
                    results.moveToNext();
                    subColCount++;
                }
                if (columns.get(colCount).getValues().size() > subColCount) {
                    Log.d(TAG, "Reducing excess subcolumn values to zero");
                    for (int z = subColCount; z < columns.get(colCount).getValues().size(); z++) {
                        columns.get(colCount).getValues().get(z).setTarget(0);//.setColor(Color.TRANSPARENT);
                    }
                }
                colCount++;

            } while (!results.isAfterLast() && (colCount < oldColumnCount));
        }
        if ((newColCount > 0) && (newColCount < oldColumnCount)) {
            Log.d(TAG, "newcolcount > 0 && < oldcolcount");
            for (int k = newColCount; k < oldColumnCount; k++) {
                int subSize = columns.get(k).getValues().size();
                for (int l = 0; l < subSize; l++) {
                    columns.get(k).getValues().get(l).setTarget(0);
                }
            }
        }
        if (newColCount > oldColumnCount) {
            Log.d(TAG, "newcolcount > oldcolumncount");
            while (!results.isAfterLast()) {
                int count = 0;
                String date = results.getString(0);
                setAxisValues(colCount, results.getString(0));
                List<SubcolumnValue> sublist = new ArrayList<>();
                while (!results.isAfterLast() && date.equals(results.getString(0))) {
                    float raw = results.getFloat(1);
                    Log.d(TAG, "626 - Date: " + results.getString(0) + ", Raw: " + raw);
                    sublist.add(new SubcolumnValue(0).setTarget(raw).setColor(colors[count]));
                    results.moveToNext();
                    count++;
                }
                columns.add(new Column(sublist).setHasLabelsOnlyForSelected(true));
                colCount++;
            }
        }
        for (Column col : columns) {
            col.setHasLabelsOnlyForSelected(true);
        }
        final boolean isThereNewDataFinal = isThereNewData;
        final int colCountFinal = colCount;
        Log.d(TAG, "colcountfinal: " + colCountFinal);
        final int oldColumnCountFinal = oldColumnCount;
        handler.post(new Runnable() {
            @Override
            public void run() {
                binding.helloGraph.setDataAnimationListener(new ChartAnimationListener() {
                    @Override
                    public void onAnimationStarted() {

                    }

                    @Override
                    public void onAnimationFinished() {
                        //if (isThereNewDataFinal) binding.noGraphDataTv.setVisibility(View.INVISIBLE);
                        //if (!isThereNewDataFinal) binding.noGraphDataTv.setVisibility(View.VISIBLE);
                        if (colCountFinal > 0 && oldColumnCountFinal > colCountFinal) {
                            for (int i = oldColumnCountFinal - 1; i >= colCountFinal; i--) {
                                binding.helloGraph.getComboLineColumnChartData().getColumnChartData().getColumns().remove(i);
                            }
                        }
                        int count = 0;
                        for (Column col : columns) {
                            for (int i = 0; i < col.getValues().size(); i++) {
                                if (col.getValues().get(i).getValue() == 0) {
                                    Log.d(TAG, "i: " + i);
                                    col.getValues().get(i).setColor(Color.TRANSPARENT);
                                }
                            }
                            Log.d(TAG, "Count: " + count++);
                        }
                        binding.helloGraph.setDataAnimationListener(null);
                        //see if we can't animate the viewport here
                        binding.helloGraph.startDataAnimation(3000);

                    }
                });
                binding.helloGraph.startDataAnimation(3000);
            }
        });
    }

    private int getNewColumnCount(Cursor results) {
        results.moveToFirst();
        int colCount = 0;
        while (!results.isAfterLast()) {
            String date = results.getString(0);
            while (!results.isAfterLast() && date.equals(results.getString(0))) {
                results.moveToNext();
            }
            colCount++;
        }
        return colCount;
    }

    private void setAxisValues(int count, String data) {
        final AxisValue av = new AxisValue(count);
        /*we only want to put a label on every third piece of data
        //if (whichGraph == COMBO_GRAPH) {
            int mod = count % baseAverage;
            if (count > 0 && mod == 0) {
                av.setLabel(data);
            } else {
                av.setLabel("");
            }
        //} else {
            //av.setLabel(data);
        //}
        */
        av.setLabel(data);
        axisValues.add(av);
    }

    private void setAxesAndDisplay() {
        boolean hasTiltedLabels = axisValues.size() > 5;
        final Axis axis = new Axis(axisValues).setTextColor(axisColor).setHasTiltedLabels(hasTiltedLabels).setName(" ");
        final ComboLineColumnChartData data = new ComboLineColumnChartData(columnData, lineData);
        data.setAxisXBottom(axis);
        final Axis axisY = new Axis().setHasLines(true).setTextColor(axisColor).setName(yAxisLabel).setTextSize(16);
        data.setAxisYLeft(axisY);
        handler.post(new Runnable() {
            @Override
            public void run() {
                binding.helloGraph.setComboLineColumnChartData(data);
            }
        });
    }

    private void generateStackedColumnData(Cursor c, int timeFrame, boolean isStacked) {
        c.moveToFirst();
        List<SubcolumnValue> subs;
        List<Column> columns = new ArrayList<>();
        axisValues.clear();
        int colCount = 0;
        int count = 0;
        while (!c.isAfterLast()) {
            String date = c.getString(0);
            setAxisValues(colCount, c.getString(0));
            subs = new ArrayList<>();
            while (!c.isAfterLast() && date.equals(c.getString(0))) {
                SubcolumnValue scv = new SubcolumnValue(c.getLong(2), colors[count]).setLabel("Lap " + c.getInt(1));
                subs.add(new SubcolumnValue(c.getLong(2), colors[count]).setLabel("Lap " + c.getInt(1)));
                c.moveToNext();
                count++;
            }
            Column col = new Column(subs);
            col.setHasLabels(true);
            columns.add(col);
            colCount++;
        }
        c.close();
        stackedColumnData.setColumns(columns);
        stackedColumnData.setStacked(isStacked);
        //binding.helloGraph.getChartRenderer().onChartDataChanged();
    }

    private void updateStackedColumnData(Cursor c, int timeFrame, boolean isStacked) {
        c.moveToFirst();

    }

    private void dumpCursorToScreen(Cursor c) {
        String s = DatabaseUtils.dumpCursorToString(c);
        Log.d(TAG, "Dump: " + s);
    }

    private void generateAverageLine() {
        List<PointValue> pointValues = new ArrayList<>();
        float values = 0;
        int size = columnData.getColumns().size() -1;
        for (Column c : columnData.getColumns()) {
            values += c.getValues().get(0).getValue();
        }
        float ave = values / size;
        pointValues.add(new PointValue(0, ave));
        pointValues.add(new PointValue(size, ave));
        Line line = new Line(pointValues);
        line.setColor(lineColor);
        //line.setCubic(true);
        line.setHasLabels(true);
        line.setHasLines(true);
        line.setHasPoints(true);
        List<Line> lines = new ArrayList<>();
        lines.add(line);
        lineData.setLines(lines);
    }

    private void generateLineData() {
        List<PointValue> pointValues = new ArrayList<>();
        List<String> averages = new ArrayList<>();
        int remainder = rawData.size() % baseAverage;
        for (int x = 0; x < rawData.size() - remainder; x += 3) {
            double ave = (rawData.get(x) + rawData.get(x + 1) + rawData.get(x + 2)) / 3;
            averages.add(String.valueOf(ave));
        }
        int index = 1;
        for (String s : averages) {
            pointValues.add(new PointValue(index, Float.valueOf(s)));
            index += 3;
        }
        Line line = new Line(pointValues);
        line.setColor(lineColor);
        line.setCubic(true);
        line.setHasLabels(true);
        line.setHasLines(true);
        line.setHasPoints(true);
        List<Line> lines = new ArrayList<>();
        lines.add(line);
        lineData.setLines(lines);
    }

    private void updateLineData(final int oldColumnCount, final int newColumnCount) {
        List<String> averages = new ArrayList<>();
        final ComboLineColumnChartData data = binding.helloGraph.getComboLineColumnChartData();
        List<PointValue> pointValues = data.getLineChartData().getLines().get(0).getValues();
        final int oldDataCount = pointValues.size();
        final int remainder = rawData.size() % baseAverage;
        final int newDataCount = (rawData.size() - remainder) / 3;

        if (newDataCount <= oldDataCount) { //we need to delete the old values
            for (int x = 0; x < rawData.size() - remainder; x += 3) {
                double ave = (rawData.get(x) + rawData.get(x + 1) + rawData.get(x + 2)) / 3;
                averages.add(String.valueOf(ave));
            }
            for (int k = 0; k < oldDataCount; k++) {
                if (k >= newDataCount) {
                    float x = pointValues.get(k).getX();
                    pointValues.get(k).setTarget(x, 0);
                } else {
                    float x = pointValues.get(k).getX();
                    pointValues.get(k).setTarget(x, Float.valueOf(averages.get(k)));
                }
            }
        } else { //we need to add in values

            for (int x = 0; x < rawData.size() - remainder; x += 3) {
                double ave = (rawData.get(x) + rawData.get(x + 1) + rawData.get(x + 2)) / 3;
                averages.add(String.valueOf(ave));
            }
            for (int k = 0; k < oldDataCount; k++) {
                float x = pointValues.get(k).getX();
                pointValues.get(k).setTarget(x, Float.valueOf(averages.get(k)));
            }
            for (int q = oldDataCount; q < newDataCount; q++) {
                float xValue = pointValues.get(pointValues.size() - 1).getX();
                xValue += 3;
                PointValue pv = new PointValue(xValue, 0);
                pv.setTarget(xValue, Float.valueOf(averages.get(q)));
                pointValues.add(pv);
            }
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                binding.helloGraph.setDataAnimationListener(new ChartAnimationListener() {
                    @Override
                    public void onAnimationStarted() {

                    }

                    @Override
                    public void onAnimationFinished() {
                        binding.helloGraph.setDataAnimationListener(null);
                        if (oldColumnCount > newColumnCount) {
                            for (int i = oldColumnCount - 1; i >= newColumnCount; i--) {
                                binding.helloGraph.getComboLineColumnChartData().getColumnChartData().getColumns().remove(i);
                            }
                        }
                        if (oldDataCount > newDataCount) {
                            for (int e = oldDataCount - 1; e >= newDataCount; e--) {
                                binding.helloGraph.getComboLineColumnChartData().getLineChartData().getLines().get(0).getValues().remove(e);
                            }
                        }
                        binding.helloGraph.startDataAnimation(2000);
                    }
                });
                binding.helloGraph.startDataAnimation(1000);
            }
        });
    }

    private static class QueryStrings {
        final static String DURATION_QUERY = "select date, time from Data where cardio_type='Bike' and date between '02/01/2017' and '02/15/2017'";// order by date asc";
        final static String DISTANCE_QUERY = "select date, distance from Data order by date asc";
        final static String CALORIES_BURNED_QUERY = "select date, calories from Data order by date asc";
        final static String LAPS_QUERY = "select Data.date, Lap.lap_num, Lap.time from Lap inner join Data on " +
                "Data._id=Lap.workout_id where Data.date=?";
        final static String LAPS_QUERY_ALT = "select Data.date, round(Lap.time * 1.0 / 60000) as Mins, Lap.lap_num from Lap inner join Data on " +
                "Data._id=Lap.workout_id where Data.date=?";

        //these three take the same build for the end of the string
        final static String DURATION_QUERY_2 = "select date, time from Data where cardio_type=";
        final static String DISTANCE_QUERY_2 = "select date, distance from Data where cardio_type=";
        final static String CALORIES_BURNED_QUERY_2 = "select date, calories from Data where cardio_type=";

        //this has a different end build
        final static String LAPS_QUERY_2 = "select Data.date, Lap.time, Lap.lap_num from Lap inner join Data on Data._id=Lap.workout_id where cardio_type=";
    }

    private class ComboTouchListener implements ComboLineColumnChartOnValueSelectListener {

        @Override
        public void onColumnValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
            Log.d(TAG, "ColumnIndex: " + columnIndex + ", SubColumnIndex: " + subcolumnIndex + ", subcolumnvalue: " + value);
            final ComboLineColumnChartData oldData = binding.helloGraph.getComboLineColumnChartData();
            final List<Column> columns = oldData.getColumnChartData().getColumns();
            final Column col = columns.get(columnIndex);
            for (int i = 0; i < col.getValues().size(); i++) {
                Log.d(TAG, "Column Dump: SubColumn Value " + i + ": " + col.getValues().get(i).getValue());
            }

        }

        @Override
        public void onPointValueSelected(int lineIndex, int pointIndex, PointValue value) {
            Log.d(TAG, "lineIndex: " + lineIndex + ", pointIndex: " + pointIndex + ", pointvalue: " + value);
        }

        @Override
        public void onValueDeselected() {
            //do nothing, we don't care
        }
    }
}
