package com.bolyndevelopment.owner.runlogger2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.widget.Toast;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityGraphsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import lecho.lib.hellocharts.animation.ChartAnimationListener;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
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

    private static final String LIMIT = " limit ";

    private static final int DAY = 1;
    private static final int WEEK = 15;
    private static final int MONTH = 30;
    private static final int YEAR = 100;

    private static final int COMBO_GRAPH = -1;
    private static final int COLUMN_ONLY_GRAPH = 1;

    boolean initialDataLoaded = false;
    int baseAverage;
    int zoomLevel;
    int columnColor;
    int lineColor;
    int axisColor;
    boolean isStacked;

    int timeFrame = MONTH;
    String query = QueryStrings.DURATION_QUERY;

    int[] colors = new int[]{Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA,
            Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN, Color.GRAY, Color.MAGENTA};

    Handler handler = new Handler();
    List<Float> rawData;
    List<AxisValue> axisValues;
    ColumnChartData columnData, stackedColumnData;
    LineChartData lineData;
    ActivityGraphsBinding binding;
    String date;
    int visibleGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_graphs);
        date = getIntent().getStringExtra("date");

        if (savedInstanceState != null) {
            timeFrame = savedInstanceState.getInt("timeFrame");
            query = savedInstanceState.getString("query");
        }
        initVars();
        //initSpinners();
        initGraph();
        //initFabs();

        binding.drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        /*
        if (date == null) {
            fadeInOutCharts(COMBO_GRAPH);
            presentGeneralStatsChart(query + LIMIT + timeFrame, null);
        } else {
            query = QueryStrings.LAPS_QUERY;
            fadeInOutCharts(COLUMN_ONLY_GRAPH);
            presentGeneralStatsChart(query, new String[]{date});
        }
        */
        isStacked = true;
        query = QueryStrings.LAPS_QUERY;
        fadeInOutCharts(COMBO_GRAPH);
        presentGeneralStatsChart(query, new String[]{date});
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("timeFrame", timeFrame);
        outState.putString("query", query);
        super.onSaveInstanceState(outState);
    }

    private void initVars() {
        baseAverage = 3;
        zoomLevel = 2;
        columnColor = Color.WHITE;
        lineColor = getResources().getColor(R.color.colorAccent);
        axisColor = Color.WHITE;

        timeFrame = MONTH;
        query = QueryStrings.DURATION_QUERY;
        axisValues = new ArrayList<>();
        rawData = new ArrayList<>();
        columnData = new ColumnChartData();
        lineData = new LineChartData();
        stackedColumnData = new ColumnChartData();
    }

    private void initSpinners() {
        final ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(this, R.array.graph_array_list, android.R.layout.simple_spinner_item);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerData.setAdapter(dataAdapter);
        binding.spinnerData.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        presentGeneralStatsChart(QueryStrings.DURATION_QUERY, null);
                        break;
                    case 2:
                        presentGeneralStatsChart(QueryStrings.DISTANCE_QUERY, null);
                        break;
                    case 3:
                        presentGeneralStatsChart(QueryStrings.CALORIES_BURNED_QUERY, null);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing, we don't care
            }
        });

        final ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(this, R.array.time_frame, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTimeFrame.setAdapter(timeAdapter);
        binding.spinnerTimeFrame.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:

                        break;
                    case 1:

                        break;
                    case 2:

                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing, we don't care
            }
        });
    }

    private void initGraph() {
        binding.helloGraph.setValueTouchEnabled(true);
        binding.helloGraph.setOnValueTouchListener(new ComboTouchListener());
        binding.helloGraph.setZoomType(ZoomType.HORIZONTAL);
        binding.helloGraph2.setValueTouchEnabled(true);
        binding.helloGraph2.setOnValueTouchListener(new StackedTouchListener());
        binding.helloGraph2.setZoomType(ZoomType.HORIZONTAL);
    }

    private void initFabs() {
        binding.fabPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (zoomLevel < 10) {
                    binding.helloGraph.setZoomLevelWithAnimation(1, 0, zoomLevel++);
                }
            }
        });
        binding.fabMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (zoomLevel > -1) {
                    binding.helloGraph.setZoomLevelWithAnimation(1, 0, zoomLevel--);
                }
            }
        });
    }

    private void fadeInOutCharts(int which) {
        if (which == COMBO_GRAPH) {
            binding.helloGraph.setAlpha(0f);
            binding.helloGraph.setVisibility(View.VISIBLE);
            binding.helloGraph.animate().alpha(1f).setDuration(500).setListener(null).start();
            binding.helloGraph2.animate().alpha(0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    binding.helloGraph2.setVisibility(View.GONE);
                }
            }).start();
            visibleGraph = COMBO_GRAPH;
        } else {
            binding.helloGraph2.setAlpha(0f);
            binding.helloGraph2.setVisibility(View.VISIBLE);
            binding.helloGraph2.animate().alpha(1f).setDuration(500).setListener(null).start();
            binding.helloGraph.animate().alpha(0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    binding.helloGraph.setVisibility(View.GONE);
                }
            }).start();
            visibleGraph = COLUMN_ONLY_GRAPH;
        }
    }
    private void presentLapStatsChart(int timeFrame) {

    }

    private void presentGeneralStatsChart(@NonNull final String query, @Nullable final String[] args) {
        //if (args == null) {
            if (visibleGraph == COLUMN_ONLY_GRAPH) {
                fadeInOutCharts(COMBO_GRAPH);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Cursor c = DatabaseAccess.getInstance().rawQuery(query, args);
                    if (initialDataLoaded) {
                        updateColumnData(c);
                    } else {
                        generateColumnData(c);
                        //generateAverageLine();
                        //generateLineData();
                        initialDataLoaded = true;
                    }
                    setAxesAndDisplay();
                }
            }).start();
            /*
        } else {
            if (visibleGraph == COMBO_GRAPH) {
                fadeInOutCharts(COLUMN_ONLY_GRAPH);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Cursor c = DatabaseAccess.getInstance().rawQuery(query, args);
                    //dumpCursorToScreen(c);
                    if (initialDataLoaded) {
                        generateStackedColumnData(c, 0, false);
                        setAxesAndDisplay();
                    } else {
                        generateStackedColumnData(c, 0, false);
                        setAxesAndDisplay();
                        //initialDataLoaded = true;
                    }
                }
            }).start();
        }
        */
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
            setAxisValues(colCount, c.getString(0), COLUMN_ONLY_GRAPH);
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

    private void setAxesAndDisplay() {
        switch (visibleGraph) {
            case COMBO_GRAPH:
                final Axis axis = new Axis(axisValues).setTextColor(axisColor).setHasTiltedLabels(true).setName("    ");
                final ComboLineColumnChartData data = new ComboLineColumnChartData(columnData, lineData);
                data.setAxisXBottom(axis);
                final Axis axisY = new Axis().setHasLines(true).setTextColor(axisColor).setName("Minutes").setTextSize(16);
                data.setAxisYLeft(axisY);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.helloGraph.setComboLineColumnChartData(data);
                    }
                });
                break;
            case COLUMN_ONLY_GRAPH:
                final Axis axisX = new Axis(axisValues).setTextColor(axisColor).setHasTiltedLabels(false).setName("    ");
                stackedColumnData.setAxisXBottom(axisX);
                final Axis axisY2 = new Axis().setHasLines(true).setTextColor(axisColor).setName("Minutes").setTextSize(16);
                stackedColumnData.setAxisYLeft(axisY2);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.helloGraph2.setColumnChartData(stackedColumnData);
                    }
                });
        }
    }

    private void dumpCursorToScreen(Cursor c) {
        String s = DatabaseUtils.dumpCursorToString(c);
        Log.d(TAG, "Dump: " + s);
    }

    private void generateColumnData(Cursor results) {
        results.moveToFirst();
        Log.d(TAG, "Count: " + results.getCount());
        rawData.clear();
        List<SubcolumnValue> subcolumnValues;
        List<Column> columns = new ArrayList<>();
        axisValues.clear();
        int colCount = 0;
        int count = 0;
        while (!results.isAfterLast()) {
            String date = results.getString(0);
            subcolumnValues = new ArrayList<>();
            setAxisValues(colCount, results.getString(0), COMBO_GRAPH);
            while (!results.isAfterLast() && date.equals(results.getString(0))) {
                float raw;
                if (query.equals(QueryStrings.DURATION_QUERY) || query.equals(QueryStrings.LAPS_QUERY_ALT)) {
                    raw = Utils.convertMillisToFloatMinutes(results.getLong(1));
                } else {
                    raw = results.getFloat(1);
                }
                //SubcolumnValue scv = new SubcolumnValue(results.getLong(1), colors[count]);
                subcolumnValues.add(new SubcolumnValue(raw, colors[count]));
                results.moveToNext();
                count++;
                //float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                //we are placing the chart values w/o dates to be used for the average line chart
                //rawData.add(raw);
                //subcolumnValues.add(new SubcolumnValue(raw, columnColor));
            }
            Column col = new Column(subcolumnValues);
            col.setHasLabelsOnlyForSelected(true);
            columns.add(col);
            results.moveToNext();
            colCount++;
        }
        results.close();
        columnData.setColumns(columns);
        columnData.setStacked(isStacked);
    }

    private void setAxisValues(int count, String data, int whichGraph) {
        final AxisValue av = new AxisValue(count);
        /*we only want to put a label on every third piece of data
        if (whichGraph == COMBO_GRAPH) {
            int mod = count % baseAverage;
            if (count > 0 && mod == 0) {
                av.setLabel(data);
            } else {
                av.setLabel("");
            }
        } else {
            av.setLabel(data);
        }
        */
        av.setLabel(data);
        axisValues.add(av);
    }

    private void updateColumnData(Cursor results) {
        results.moveToFirst();
        final ComboLineColumnChartData oldData = binding.helloGraph.getComboLineColumnChartData();
        final int cursorCount = results.getCount();
        final int oldColumnCount = oldData.getColumnChartData().getColumns().size();
        axisValues.clear();
        int count = 0;
        List<Column> columns = oldData.getColumnChartData().getColumns();
        if (cursorCount <= oldColumnCount) {
            rawData.clear();
            for (int j = 0; j < oldColumnCount; j++) {
                if (j >= cursorCount) {
                    columns.get(j).getValues().get(0).setTarget(0);
                } else {
                    float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                    columns.get(j).getValues().get(0).setTarget(raw);
                    rawData.add(raw);
                    setAxisValues(count, results.getString(0), COMBO_GRAPH);
                    results.moveToNext();
                    count++;
                }
            }
        } else {
            rawData.clear();
            List<SubcolumnValue> subcolumnValues;
            for (int j = 0; j < oldColumnCount; j++) {
                float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                columns.get(j).getValues().get(0).setTarget(raw);
                rawData.add(raw);
                setAxisValues(count, results.getString(0), COMBO_GRAPH);
                results.moveToNext();
                count++;
            }
            for (int i = oldColumnCount; i < cursorCount; i++) {
                float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                rawData.add(raw);
                subcolumnValues = new ArrayList<>();
                subcolumnValues.add(new SubcolumnValue(0, columnColor));
                subcolumnValues.get(0).setTarget(raw);
                Column col = new Column(subcolumnValues);
                col.setHasLabelsOnlyForSelected(true);
                columns.add(col);
                setAxisValues(count, results.getString(0), COMBO_GRAPH);
                results.moveToNext();
                count++;
            }
        }
        //generateAverageLine();
        //updateLineData(oldColumnCount, cursorCount);
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
                        if (oldColumnCount > cursorCount) {
                            for (int i = oldColumnCount - 1; i >= cursorCount; i--) {
                                binding.helloGraph.getComboLineColumnChartData().getColumnChartData().getColumns().remove(i);
                            }
                        }
                        binding.helloGraph.startDataAnimation(2000);
                    }
                });
                binding.helloGraph.startDataAnimation(1000);
            }
        });
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

    public void onNavClick(View v) {
        switch (v.getId()) {
            case R.id.lap_tv:
                query = QueryStrings.LAPS_QUERY;
                break;
            case R.id.duration_tv:
                query = QueryStrings.DURATION_QUERY;
                break;
            case R.id.distance_tv:
                query = QueryStrings.DISTANCE_QUERY;
                break;
            case R.id.cal_burn_tv:
                query = QueryStrings.CALORIES_BURNED_QUERY;
                break;
            case R.id.days:
                timeFrame = DAY;
                break;
            case R.id.weeks:
                timeFrame = WEEK;
                break;
            case R.id.months:
                timeFrame = MONTH;
                break;
            case R.id.year:
                timeFrame = YEAR;
                break;
            default:
                timeFrame = MONTH;
                query = QueryStrings.DURATION_QUERY;
                break;
        }
        binding.drawerLayout.closeDrawer(binding.chartsNavViewRight);
        Timer t = new Timer(true);
        //t.schedule(new TimerTask() {
            //@Override
            //public void run() {
                if (query.equals(QueryStrings.LAPS_QUERY)) {
                    isStacked = true;
                    presentGeneralStatsChart(query, new String[]{date});
                } else {
                    if (timeFrame == DAY) {
                        timeFrame = WEEK;
                    }
                    isStacked = false;
                    presentGeneralStatsChart(query + LIMIT + timeFrame, null);
                }
            //}
        //}, 300);
    }

    private static class QueryStrings {
        final static String DURATION_QUERY = "select date, time from Data order by date asc";
        final static String DISTANCE_QUERY = "select date, distance from Data order by date asc";
        final static String CALORIES_BURNED_QUERY = "select date, calories from Data order by date asc";
        final static String LAPS_QUERY = "select Data.date, Lap.lap_num, Lap.time from Lap inner join Data on Data._id=Lap.workout_id where Data.date=?";
        final static String LAPS_QUERY_ALT = "select Data.date, Lap.time, Lap.lap_num from Lap inner join Data on Data._id=Lap.workout_id where Data.date=?";
    }

    private class ComboTouchListener implements ComboLineColumnChartOnValueSelectListener {

        @Override
        public void onColumnValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
            Log.d(TAG, "ColumnIndex: " + columnIndex + ", SubColumnIndex: " + subcolumnIndex + ", subcolumnvalue: " + value);
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

    private class StackedTouchListener implements ColumnChartOnValueSelectListener {

        @Override
        public void onValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
            Toast.makeText(getBaseContext(), "Column: " + columnIndex
                    + "\nSubColumn Value: " + value.getValue(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onValueDeselected() {

        }
    }
}
