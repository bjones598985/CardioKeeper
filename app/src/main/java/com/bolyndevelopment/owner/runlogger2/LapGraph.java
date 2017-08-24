package com.bolyndevelopment.owner.runlogger2;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityGraphsBinding;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.animation.ChartAnimationListener;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.ComboLineColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.view.ColumnChartView;

public class LapGraph extends AppCompatActivity {

    public static final String TAG = "HelloGraph";

    private static final String LIMIT = " limit ";

    private static final int DAY = 1;
    private static final int WEEK = 15;
    private static final int MONTH = 30;
    private static final int YEAR = 100;

    boolean initialDataLoaded = false;
    int baseAverage;
    int zoomLevel;
    int columnColor;
    int lineColor;
    int axisColor;

    ColumnChartView chart;

    Handler handler = new Handler();
    List<Float> rawData;
    List<AxisValue> axisValues;
    ColumnChartData columnData;
    LineChartData lineData;
    String date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lap_graph);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Lap Graphs");
        setSupportActionBar(toolbar);

        chart = (ColumnChartView) findViewById(R.id.lap_graph);
        date = getIntent().getStringExtra("date");


        //initVars();
        //initSpinners();
        //initGraph();
        //initFabs();



    }

    /*

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void initVars() {
        baseAverage = 3;
        zoomLevel = 2;
        columnColor = Color.WHITE;
        lineColor = getResources().getColor(R.color.colorAccent);
        axisColor = Color.WHITE;

        axisValues = new ArrayList<>();
    }

    private void initGraph() {
        //binding.helloGraph.setValueTouchEnabled(true);
        //binding.helloGraph.setOnValueTouchListener(new HelloGraph.ValueTouchListener());
        //binding.helloGraph.setZoomType(ZoomType.HORIZONTAL);
    }

    private void presentChart(@NonNull final String query, @Nullable final String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor c;
                if (args == null) {
                    c = DatabaseAccess.getInstance().rawQuery(query, null);
                } else {
                    c = DatabaseAccess.getInstance().rawQuery(query, args);
                }
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
                        //binding.helloGraph.setComboLineColumnChartData(data);
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
            setAxisValues(count, results.getString(0));
            //float raw = query.equals(HelloGraph.QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
            //we are placing the chart values w/o dates to be used for the average line chart
            //rawData.add(raw);
            //subcolumnValues.add(new SubcolumnValue(raw, columnColor));
            Column col = new Column(subcolumnValues);
            col.setHasLabelsOnlyForSelected(true);
            columns.add(col);
            results.moveToNext();
            count++;
        }
        results.close();
        columnData = new ColumnChartData(columns);
    }

    private void setAxisValues(int count, String data) {
        final AxisValue av = new AxisValue(count);
        //we only want to put a label on every third piece of data
        int mod = count % baseAverage;
        if (count > 0 && mod == 0) {
            av.setLabel(data);
        } else {
            av.setLabel("");
        }
        axisValues.add(av);
    }

    private void updateColumnData(Cursor results) {
        results.moveToFirst();
        //final ComboLineColumnChartData oldData = binding.helloGraph.getComboLineColumnChartData();
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
                    //float raw = query.equals(HelloGraph.QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                    ///columns.get(j).getValues().get(0).setTarget(raw);
                    //rawData.add(raw);
                    setAxisValues(count, results.getString(0));
                    results.moveToNext();
                    count++;
                }
            }
        } else {
            rawData = new ArrayList<>();
            List<SubcolumnValue> subcolumnValues;
            for (int j = 0; j < oldColumnCount; j++) {
                float raw = query.equals(HelloGraph.QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                columns.get(j).getValues().get(0).setTarget(raw);
                rawData.add(raw);
                setAxisValues(count, results.getString(0));
                results.moveToNext();
                count++;
            }
            for (int i = oldColumnCount; i < cursorCount; i++) {
                float raw = query.equals(HelloGraph.QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                rawData.add(raw);
                subcolumnValues = new ArrayList<>();
                subcolumnValues.add(new SubcolumnValue(0, columnColor));
                subcolumnValues.get(0).setTarget(raw);
                Column col = new Column(subcolumnValues);
                col.setHasLabelsOnlyForSelected(true);
                columns.add(col);
                setAxisValues(count, results.getString(0));
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

    */

}
