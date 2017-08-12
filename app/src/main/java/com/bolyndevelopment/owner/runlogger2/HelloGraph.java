package com.bolyndevelopment.owner.runlogger2;

import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityGraphsBinding;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.ComboLineColumnChartView;

/**
 * Created by Bobby Jones on 8/11/2017.
 */

public class HelloGraph extends AppCompatActivity {
    public static final String TAG = "HelloGraph";

    private static final String LIMIT = " limit ";
    private static final int VIEWPORT_BASELINE = 0;
    private static final int VIEWPORT_MAX_HORIZ_OFFSET = 10;
    private static final int VIEWPORT_HORIZ_OFFSET = 10;
    private static final int DURATION = 1;
    private static final int DISTANCE = 2;
    private static final int CAL_BURNED = 3;
    private static final int WEEK = 15;
    private static final int MONTH = 30;
    private static final int YEAR = 100;

    boolean hasRanOnceAlready = false;

    int timeFrame = MONTH;
    String query = QueryStrings.DURATION_QUERY;

    int viewportOffset = VIEWPORT_BASELINE;
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

        //handler = new Handler();
        //axisValues = new ArrayList<>();
        initSpinners();
        initGraph();
        initFabs();

        binding.drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        presentChart(QueryStrings.DURATION_QUERY + LIMIT + timeFrame);

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
                if (viewportOffset <= (VIEWPORT_BASELINE + VIEWPORT_MAX_HORIZ_OFFSET)) {
                    binding.helloGraph.setZoomLevelWithAnimation(20, 0, 3);
                    //Viewport viewport = binding.helloGraph.getCurrentViewport();
                    //viewport.inset(VIEWPORT_HORIZ_OFFSET, 0);
                    //binding.helloGraph.setCurrentViewportWithAnimation(viewport);
                    viewportOffset++;
                }
            }
        });
        binding.fabMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewportOffset >= (VIEWPORT_BASELINE - VIEWPORT_MAX_HORIZ_OFFSET)) {
                    Viewport viewport = binding.helloGraph.getCurrentViewport();
                    viewport.inset(-VIEWPORT_HORIZ_OFFSET, 0);
                    binding.helloGraph.setCurrentViewportWithAnimation(viewport);
                    viewportOffset--;
                }
            }
        });
    }

    private void presentChart(final String query) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor c = DatabaseAccess.getInstance().rawQuery(query, null);
                if (hasRanOnceAlready) {
                    updateColumnData(c);
                } else {
                    generateColumnData(c);
                    hasRanOnceAlready = true;
                }
                generateLineData();
                Axis axis = new Axis(axisValues);
                axis.setTextColor(Color.BLACK);
                final ComboLineColumnChartData data = new ComboLineColumnChartData(columnData, lineData);
                data.setAxisXBottom(axis);
                Axis axisY = new Axis().setHasLines(true).setTextColor(Color.BLACK);
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
        //int pos = binding.spinnerData.getSelectedItemPosition();
        rawData = new ArrayList<>();
        List<SubcolumnValue> subcolumnValues;
        List<Column> columns = new ArrayList<>();
        int count = 0;
        while (!results.isAfterLast()) {
            subcolumnValues = new ArrayList<>();
            final AxisValue av = new AxisValue(count);
            int mod = count % 3;
            if (count > 0 && mod == 0) {
                av.setLabel(results.getString(0));
            } else {
                av.setLabel("");
            }
            axisValues.add(av);
            float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
            rawData.add(raw);
            subcolumnValues.add(new SubcolumnValue(raw, Color.BLUE));
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
        ComboLineColumnChartData oldData = binding.helloGraph.getComboLineColumnChartData();
        final int cursorCount = results.getCount();
        final int oldColumnCount = oldData.getColumnChartData().getColumns().size();
        Log.d(TAG, "new: " + cursorCount + ", old: " + oldColumnCount);
        int count = 0;
        int maxValue;
        boolean cursCountSmaller;
        List<Column> columns = oldData.getColumnChartData().getColumns();
        Log.d(TAG, "Columns size: " + columns.size());
        if (cursorCount < oldColumnCount) {
            maxValue = oldColumnCount;
            cursCountSmaller = true;
            rawData = new ArrayList<>();
            List<SubcolumnValue> subcolumnValues;


            for (int j = 0; j < oldColumnCount; j++) {
                if (j >= cursorCount) {
                    columns.get(j).getValues().get(0).setTarget(0);
                } else {
                    float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                    columns.get(j).getValues().get(0).setTarget(raw);
                    //subcolumnValues = new ArrayList<>();
                    final AxisValue av = new AxisValue(count);
                    int mod = count % 3;
                    if (count > 0 && mod == 0) {
                        av.setLabel(results.getString(0));
                    } else {
                        av.setLabel("");
                    }
                    axisValues.add(av);
                    //float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                    //rawData.add(raw);
                    //subcolumnValues.add(new SubcolumnValue(raw, Color.BLUE));
                    //Column col = new Column(subcolumnValues);
                    //col.setHasLabelsOnlyForSelected(true);
                    //columns.add(col);
                    results.moveToNext();
                    count++;
                    Log.d(TAG, "j: " + j);
                }
            }
            //for (int i = oldColumnCount - 1; i >= cursorCount; i--) {
                //Log.d(TAG, "i: " + i);
                //columns.remove(i);
            //}
        } else {
            maxValue = oldColumnCount;
            cursCountSmaller = false;
            rawData = new ArrayList<>();
            List<SubcolumnValue> subcolumnValues;

            for (int j = 0; j < oldColumnCount; j++) {
                float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                columns.get(j).getValues().get(0).setTarget(raw);
                //subcolumnValues = new ArrayList<>();
                final AxisValue av = new AxisValue(count);
                int mod = count % 3;
                if (count > 0 && mod == 0) {
                    av.setLabel(results.getString(0));
                } else {
                    av.setLabel("");
                }
                axisValues.add(av);
                //float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                //rawData.add(raw);
                //subcolumnValues.add(new SubcolumnValue(raw, Color.BLUE));
                //Column col = new Column(subcolumnValues);
                //col.setHasLabelsOnlyForSelected(true);
                //columns.add(col);
                results.moveToNext();
                count++;
            }
            for (int i = oldColumnCount; i < cursorCount; i++) {
                float raw = query.equals(QueryStrings.DURATION_QUERY) ? Utils.convertMillisToFloatMinutes(results.getLong(1)) : results.getFloat(1);
                //rawData.add(raw);
                subcolumnValues = new ArrayList<>();
                subcolumnValues.add(new SubcolumnValue(0, Color.BLUE));
                subcolumnValues.get(0).setTarget(raw);
                Column col = new Column(subcolumnValues);
                col.setHasLabelsOnlyForSelected(true);
                columns.add(col);
                results.moveToNext();
                count++;
            }
        }
        //columnData = new ColumnChartData(columns);
        handler.post(new Runnable() {
            @Override
            public void run() {
                binding.helloGraph.setDataAnimationListener(new ChartAnimationListener() {
                    @Override
                    public void onAnimationStarted() {

                    }

                    @Override
                    public void onAnimationFinished() {
                        Log.d(TAG, "onAnimationFinished");
                        binding.helloGraph.setDataAnimationListener(null);
                        if (oldColumnCount > cursorCount) {
                            for (int i = oldColumnCount - 1; i >= cursorCount; i--) {
                                Log.d(TAG, "i: " + i);
                                binding.helloGraph.getComboLineColumnChartData().getColumnChartData().getColumns().remove(i);
                                binding.helloGraph.startDataAnimation(5000);
                            }
                        }
                    }
                });
                binding.helloGraph.startDataAnimation(1000);
            }
        });

    }

    private void generateLineData() {
        List<PointValue> pointValues = new ArrayList<>();
        List<String> averages = new ArrayList<>();
        int remainder = rawData.size() % 3;
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
        line.setColor(Color.RED);
        line.setCubic(true);
        line.setHasLabels(true);
        line.setHasLines(true);
        line.setHasPoints(true);
        List<Line> lines = new ArrayList<>();
        lines.add(line);
        lineData = new LineChartData(lines);
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
