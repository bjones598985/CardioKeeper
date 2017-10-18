package com.bolyndevelopment.owner.runlogger2;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
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
    String distUnit;

    int colorIndex = 0;

    List<Integer> columnColorList = new ArrayList<>();

    Handler handler = new Handler();
    List<Float> rawData;
    List<AxisValue> axisValues;
    ColumnChartData columnData;
    LineChartData lineData;
    ActivityGraphsBinding binding;

    boolean isDataTypeSet = false, isCardioTypeSet = false, isStatusBarVisible;
    int dataType, timeFrame = 1;
    String cardioType, yAxisLabel, initialDate;

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
    }

    private void delayedHide(int delayMillis) {
        handler.postDelayed(new Runnable() {
            @SuppressLint("InlinedApi")
            @Override
            public void run() {
                // Delayed removal of status and navigation bar

                // Note that some of these constants are new as of API 16 (Jelly Bean)
                // and API 19 (KitKat). It is safe to use them, as they are inlined
                // at compile-time and do nothing on earlier devices.
                binding.coordLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }, delayMillis);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_graphs);
        Log.d(TAG, "oncreate Timeframe: " + timeFrame);

        binding.coordLayout.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE){
                    delayedHide(500);
                }
            }
        });

        setInitialPrefs();
        initialDate = getIntent().getStringExtra("date");
        final String cType = getIntent().getStringExtra("cType");

        int cardioColor = getResources().getColor(R.color.colorPrimary);
        if (cType != null) {
            cardioColor = Utils.ColorUtils.getCardioColor(cType);
            setActivityColorScheme(cardioColor);
        }

        initVars();

        initSpinners();
        initGraph();
        if (initialDate == null) {
            if (savedInstanceState != null) {
                //timeFrame = binding.spinnerTimeFrame.getSelectedItemPosition();
                //initialDataLoaded = savedInstanceState.getBoolean("initialDataLoaded");
                overrideInitVars(savedInstanceState.getString("cardioType"));
                //presentChart();
                Log.d(TAG, "savedinstance != null timeframe: " + timeFrame);
            }
            //binding.spinnerCardioType.performClick();
        } else {
            overrideInitVars(cType);
            columnColorList = Utils.ColorUtils.makeNNumberOfColors(cardioColor, 0);
            presentChart();
        }
        binding.runQueryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentChart();
            }
        });
    }

    private void setActivityColorScheme(int color) {
        columnColorList.clear();
        columnColorList = Utils.ColorUtils.makeNNumberOfColors(color, 0);
        GradientDrawable gradBack = (GradientDrawable) getResources().getDrawable(R.drawable.gradient_drawable);
        gradBack.setColors(new int[]{color, Color.WHITE});
        binding.coordLayout.setBackground(gradBack);
        binding.spinnerCardioType.setPopupBackgroundDrawable(new ColorDrawable(color));
        binding.spinnerData.setPopupBackgroundDrawable(new ColorDrawable(color));
        binding.spinnerTimeFrame.setPopupBackgroundDrawable(new ColorDrawable(color));
    }

    private void setInitialPrefs() {
        final SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String distPref = sPrefs.getString(getResources().getString(R.string.pref_distance), "-1");
        distUnit = distPref.equals("-1") ? getResources().getString(R.string.miles) : getResources().getString(R.string.kilos);
    }

    private int getNextColor() {
        if (colorIndex >= columnColorList.size()) {
            colorIndex = 0;
        }
        return columnColorList.get(colorIndex++);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (initialDate == null) {
            timeFrame = binding.spinnerTimeFrame.getSelectedItemPosition();
            presentChart();
        }
        Log.d(TAG, "restoreinstance != null timeframe: " + timeFrame);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("cardioType", cardioType);
        outState.putBoolean("initialDataLoaded", initialDataLoaded);
        super.onSaveInstanceState(outState);
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
        axisValues = new ArrayList<>();
        rawData = new ArrayList<>();
        columnData = new ColumnChartData();
        lineData = new LineChartData();
    }

    private void initSpinners() {
        final ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(this, R.array.graph_data_type, R.layout.spinner_item);
        dataAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        binding.spinnerData.setAdapter(dataAdapter);
        //this is necessary so that on layout it doesnt fire
        binding.spinnerData.setSelection(0, false);
        binding.spinnerData.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dataType = position;
                isDataTypeSet = position != 0;
                //only stack columns when displaying lap data
                columnData.setStacked(position == 1);
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
        binding.spinnerCardioType.setSelection(0, false);
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
            binding.chartTitle.setText(cardioType);
            int newColor = Utils.ColorUtils.getCardioColor(cardioType);
            setActivityColorScheme(newColor);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String query = getQuery();
                    Cursor c = DataModel.getInstance().rawQuery(query, null);
                    //dumpCursorToScreen(c);
                    if (initialDataLoaded) {
                        //updateColumnData(c);
                        updateColumnValues(c);
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
                yAxisLabel = distUnit;
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
                yAxisLabel = "Calories / " + distUnit;
                break;
        }
        return query;
    }

    private String buildAndRetrieveMidQueryPart() {
        String endOfQuery = null;
        String ex = "\'" + cardioType + "\'";
        String today, secondDate;
        if (initialDate != null && !initialDataLoaded) {
            today = initialDate;
            secondDate = initialDate;
        } else {
            today = Utils.convertDateToString(new Date(), Utils.DB_DATE_FORMAT);
            secondDate = getSecondDate();
        }

        if (dataType == 1) {
            endOfQuery = ex + " and Data.date between \'" + secondDate + "\' and \'" + today + "\'";
        } else {
            endOfQuery = ex + " and date between '" + secondDate + "\' and \'" + today + "\'";
        }
        return endOfQuery;
    }

    private String getSecondDate() {
        Calendar cal = Calendar.getInstance();
        Log.d(TAG, "getseconddate timeframe: " + timeFrame);
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

    private void generateColumnData(Cursor results) {
        results.moveToFirst();
        //rawData.clear();
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
                subcolumnValues.add(new SubcolumnValue(raw).setColor(getNextColor()));
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

    private void updateColumnValues(Cursor results) {
        int newColCount = getNewColumnCount(results);
        final ComboLineColumnChartData oldData = binding.helloGraph.getComboLineColumnChartData();
        int colCount = 0;
        results.moveToFirst();

        final List<Column> columns = oldData.getColumnChartData().getColumns();
        int sublistSize = 0;
        if (columns.size() > 0) {
            sublistSize = columns.get(colCount).getValues().size();
            //Log.d(TAG, "Line 572 - sublistsize: " + sublistSize);
        }
        int oldColumnCount = columns.size();

        //rawData.clear();
        if (newColCount == 0) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toasty.info(getBaseContext(), "Uh oh, your search turned up no results", Toast.LENGTH_LONG, true).show();
                    for (Column col : columns) {
                        for (SubcolumnValue sc : col.getValues()) {
                            sc.setTarget(0);
                        }
                    }
                }
            });
        }

        if (newColCount > 0) {
            if (oldColumnCount == 0) {
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
                    if (subColCount >= sublistSize) {
                        columns.get(colCount).getValues().add(new SubcolumnValue().setValue(0).setTarget(raw).setColor(getNextColor()));
                    } else {
                        columns.get(colCount).getValues().get(subColCount).setTarget(raw).setColor(getNextColor());
                    }
                    results.moveToNext();
                    subColCount++;
                }
                if (columns.get(colCount).getValues().size() > subColCount) {
                    for (int z = subColCount; z < columns.get(colCount).getValues().size(); z++) {
                        columns.get(colCount).getValues().get(z).setTarget(0);//.setColor(Color.TRANSPARENT);
                    }
                }
                colCount++;

            } while (!results.isAfterLast() && (colCount < oldColumnCount));
        }
        if ((newColCount > 0) && (newColCount < oldColumnCount)) {
            for (int k = newColCount; k < oldColumnCount; k++) {
                int subSize = columns.get(k).getValues().size();
                for (int l = 0; l < subSize; l++) {
                    columns.get(k).getValues().get(l).setTarget(0);
                }
            }
        }
        if (newColCount > oldColumnCount) {
            while (!results.isAfterLast()) {
                int count = 0;
                String date = results.getString(0);
                setAxisValues(colCount, results.getString(0));
                List<SubcolumnValue> sublist = new ArrayList<>();
                while (!results.isAfterLast() && date.equals(results.getString(0))) {
                    float raw = results.getFloat(1);
                    sublist.add(new SubcolumnValue(0).setTarget(raw).setColor(getNextColor()));
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
        final int colCountFinal = colCount;
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
                        if (colCountFinal > 0 && oldColumnCountFinal > colCountFinal) {
                            for (int i = oldColumnCountFinal - 1; i >= colCountFinal; i--) {
                                binding.helloGraph.getComboLineColumnChartData().getColumnChartData().getColumns().remove(i);
                            }
                        }
                        for (Column col : columns) {
                            for (int i = 0; i < col.getValues().size(); i++) {
                                if (col.getValues().get(i).getValue() == 0) {
                                    col.getValues().get(i).setColor(Color.TRANSPARENT);
                                }
                            }
                        }
                        binding.helloGraph.setDataAnimationListener(null);
                        //see if we can't animate the viewport here
                        binding.helloGraph.startDataAnimation(1500);

                    }
                });
                binding.helloGraph.startDataAnimation(1500);
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
        av.setLabel(data);
        axisValues.add(av);
    }

    private void setAxesAndDisplay() {
        boolean hasTiltedLabels = axisValues.size() > 5;
        final Axis axis = new Axis(axisValues).setTextColor(Color.BLACK).setHasTiltedLabels(hasTiltedLabels).setName(" ");
        final ComboLineColumnChartData data = new ComboLineColumnChartData(columnData, lineData);
        data.setAxisXBottom(axis);
        final Axis axisY = new Axis().setHasLines(true).setLineColor(Color.BLACK)
                .setTextColor(Color.BLACK).setName(yAxisLabel).setTextSize(16);
        data.setAxisYLeft(axisY);
        handler.post(new Runnable() {
            @Override
            public void run() {
                binding.helloGraph.setComboLineColumnChartData(data);
            }
        });
    }

    private void generateStackedColumnData(Cursor c, int timeFrame) {
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
                SubcolumnValue scv = new SubcolumnValue(c.getLong(2), getNextColor()).setLabel("Lap " + c.getInt(1));
                subs.add(new SubcolumnValue(c.getLong(2), getNextColor()).setLabel("Lap " + c.getInt(1)));
                c.moveToNext();
                count++;
            }
            Column col = new Column(subs);
            col.setHasLabels(true);
            columns.add(col);
            colCount++;
        }
        c.close();
        //stackedColumnData.setColumns(columns);
        //stackedColumnData.setStacked(isStacked);
        //binding.helloGraph.getChartRenderer().onChartDataChanged();
    }

    private void updateStackedColumnData(Cursor c, int timeFrame) {
        c.moveToFirst();

    }

    private void dumpCursorToScreen(Cursor c) {
        String s = DatabaseUtils.dumpCursorToString(c);
        //Log.d(TAG, "Dump: " + s);
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
        //line.setColor(lineColor);
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
        //line.setColor(lineColor);
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
        final int remainder = rawData.size() % 3;
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

    private class ComboTouchListener implements ComboLineColumnChartOnValueSelectListener {

        @Override
        public void onColumnValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
            final AxisValue av = axisValues.get(columnIndex);
            String label = String.valueOf(av.getLabelAsChars());
            Toasty.Config.getInstance().setTextSize(24).apply();
            Toasty.normal(getBaseContext(), "" + label + " - " + value.getValue() + " " + yAxisLabel,
                    Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onPointValueSelected(int lineIndex, int pointIndex, PointValue value) {

        }

        @Override
        public void onValueDeselected() {
            //do nothing, we don't care
        }
    }
}
