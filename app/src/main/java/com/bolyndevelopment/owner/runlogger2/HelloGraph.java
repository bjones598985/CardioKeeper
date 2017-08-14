package com.bolyndevelopment.owner.runlogger2;

import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityGraphsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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

    private static final String LIMIT = " limit ";

    private static final int WEEK = 15;
    private static final int MONTH = 30;
    private static final int YEAR = 100;

    boolean initialDataLoaded = false;
    int baseAverage = 3;
    int zoomLevel = 2;
    int columnColor = Color.parseColor("#ffffff");
    int lineColor = Color.parseColor("#00ff00");
    int axisColor = Color.parseColor("#ffffff");

    int timeFrame = MONTH;
    String query = QueryStrings.DURATION_QUERY;

    Handler handler = new Handler();
    List<Float> rawData;
    List<AxisValue> axisValues = new ArrayList<>();
    ColumnChartData columnData;
    LineChartData lineData;
    ActivityGraphsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_graphs);

        if (savedInstanceState != null) {
            timeFrame = savedInstanceState.getInt("timeFrame");
            query = savedInstanceState.getString("query");
        }
        initSpinners();
        initGraph();
        initFabs();

        binding.drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        presentChart(QueryStrings.DURATION_QUERY + LIMIT + timeFrame);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("timeFrame", timeFrame);
        outState.putString("query", query);
        super.onSaveInstanceState(outState);
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
                        presentChart(QueryStrings.DURATION_QUERY);
                        break;
                    case 2:
                        presentChart(QueryStrings.DISTANCE_QUERY);
                        break;
                    case 3:
                        presentChart(QueryStrings.CALORIES_BURNED_QUERY);
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
        binding.helloGraph.setOnValueTouchListener(new ValueTouchListener());
        binding.helloGraph.setZoomType(ZoomType.HORIZONTAL);
    }

    private void initFabs() {
        binding.fabPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (zoomLevel < 10) {
                    binding.helloGraph.setZoomLevelWithAnimation(20, 0, zoomLevel++);
                }
            }
        });
        binding.fabMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (zoomLevel > -1) {
                    binding.helloGraph.setZoomLevelWithAnimation(20, 0, zoomLevel--);
                }
            }
        });
    }

    private void presentChart(final String query) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor c = DatabaseAccess.getInstance().rawQuery(query, null);
                if (initialDataLoaded) {
                    updateColumnData(c);
                } else {
                    generateColumnData(c);
                    generateLineData();
                    initialDataLoaded = true;
                }
                Axis axis = new Axis(axisValues);
                axis.setTextColor(axisColor);
                axis.setHasTiltedLabels(true);
                final ComboLineColumnChartData data = new ComboLineColumnChartData(columnData, lineData);
                data.setAxisXBottom(axis);
                Axis axisY = new Axis().setHasLines(true).setTextColor(axisColor);
                data.setAxisYLeft(axisY);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.helloGraph.setComboLineColumnChartData(data);
                    }
                });
            }
        }).start();
    }

    private void generateColumnData(Cursor results) {
        results.moveToFirst();
        rawData = new ArrayList<>();
        List<SubcolumnValue> subcolumnValues;
        List<Column> columns = new ArrayList<>();
        axisValues.clear();
        int count = 0;
        while (!results.isAfterLast()) {
            subcolumnValues = new ArrayList<>();
            final AxisValue av = new AxisValue(count);
            //we only want to put a label on every third piece of data
            int mod = count % baseAverage;
            if (count > 0 && mod == 0) {
                av.setLabel(results.getString(0));
            } else {
                av.setLabel("");
            }
            axisValues.add(av);
            float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
            //we are placing the chart values w/o dates to be used for the average line chart
            rawData.add(raw);
            subcolumnValues.add(new SubcolumnValue(raw, columnColor));
            Column col = new Column(subcolumnValues);
            col.setHasLabelsOnlyForSelected(true);
            columns.add(col);
            results.moveToNext();
            count++;
        }
        results.close();
        columnData = new ColumnChartData(columns);
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
            rawData = new ArrayList<>();
            for (int j = 0; j < oldColumnCount; j++) {
                if (j >= cursorCount) {
                    columns.get(j).getValues().get(0).setTarget(0);
                } else {
                    float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                    columns.get(j).getValues().get(0).setTarget(raw);
                    rawData.add(raw);
                    final AxisValue av = new AxisValue(count);
                    int mod = count % baseAverage;
                    if (count > 0 && mod == 0) {
                        av.setLabel(results.getString(0));
                    } else {
                        av.setLabel("");
                    }
                    axisValues.add(av);
                    results.moveToNext();
                    count++;
                }
            }
        } else {
            rawData = new ArrayList<>();
            List<SubcolumnValue> subcolumnValues;
            for (int j = 0; j < oldColumnCount; j++) {
                float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                columns.get(j).getValues().get(0).setTarget(raw);
                rawData.add(raw);
                final AxisValue av = new AxisValue(count);
                int mod = count % baseAverage;
                if (count > 0 && mod == 0) {
                    av.setLabel(results.getString(0));
                } else {
                    av.setLabel("");
                }
                axisValues.add(av);
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
                final AxisValue av = new AxisValue(count);
                int mod = count % baseAverage;
                if (count > 0 && mod == 0) {
                    av.setLabel(results.getString(0));
                } else {
                    av.setLabel("");
                }
                axisValues.add(av);
                results.moveToNext();
                count++;
            }
        }
        updateLineData(oldColumnCount, cursorCount);
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
        lineData = new LineChartData(lines);
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
            case R.id.duration_tv:
                query = QueryStrings.DURATION_QUERY;
                break;
            case R.id.distance_tv:
                query = QueryStrings.DISTANCE_QUERY;
                break;
            case R.id.cal_burn_tv:
                query = QueryStrings.CALORIES_BURNED_QUERY;
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
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                presentChart(query + LIMIT + timeFrame);
            }
        }, 300);
    }

    private class QueryStrings {
        final static String DURATION_QUERY = "select date, time from Data order by date asc";
        final static String DISTANCE_QUERY = "select date, miles from Data order by date asc";
        final static String CALORIES_BURNED_QUERY = "select date, calories from Data order by date asc";
    }

    private class ValueTouchListener implements ComboLineColumnChartOnValueSelectListener {

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
}
