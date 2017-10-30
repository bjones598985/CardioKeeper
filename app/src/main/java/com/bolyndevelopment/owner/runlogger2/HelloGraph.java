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

    private static final String SPINNER_CARDIO = "spinner_cardio";
    private static final String SPINNER_DATA = "spinner_data";
    private static final String SPINNER_TIME = "spinner_time";

    boolean initialDataLoaded = false; //once set to true, column data is update instead of generated

    String distUnit; //miles or kilometers

    int colorIndex = 0; // index of colors to use to color the columns

    List<Integer> columnColorList = new ArrayList<>(); // list of colors for coloring the columns

    Handler handler = new Handler();

    List<Float> rawData;

    List<AxisValue> axisValues;

    ColumnChartData columnData;

    LineChartData lineData;

    ActivityGraphsBinding binding;

    boolean isDataTypeSet = false, isCardioTypeSet = false;

    int dataType, timeFrame = 0;

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
        binding.coordLayout.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE){
                    delayedHide(500);
                }
            }
        });

        /*
         * set distance variable to miles or kilometerss
         */
        setInitialPrefs();

        initialDate = getIntent().getStringExtra("date"); //null if entered through nav view
        cardioType = getIntent().getStringExtra("cType"); //null if entered through nav view

        /*
         * initialize the activity color scheme based on cardio type or null if entering from nav view
         */
        setActivityColorScheme();

        /*
         * initializes rawData, axisValues, lineChartData, and columnChartData
         */
        initVars();

        /*
         * initialize the spinners to their defaults
         */
        initSpinners();

        /*
         * initialize graph settings - zoom and touch
         */
        initGraph();

        if (initialDate != null) { //entered from clicking a list item
            if (savedInstanceState != null) { //from configuration chg
                cardioType = savedInstanceState.getString("cardioType");
                overrideInitVarsOnConfigChange(savedInstanceState);
                setActivityColorScheme();
                presentChart();
            } else {
                overrideInitVars();
                presentChart();
            }
        }
        binding.runQueryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentChart();
            }
        });
    }

    private void setInitialPrefs() {
        final SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String distPref = sPrefs.getString(getResources().getString(R.string.pref_distance), "-1");
        distUnit = distPref.equals("-1") ? getResources().getString(R.string.miles) : getResources().getString(R.string.kilos);
    }

    private void setActivityColorScheme() {
        int color;
        if (cardioType == null) {
            color = getResources().getColor(R.color.colorPrimary);
        } else {
            color = Utils.ColorUtils.getCardioColor(cardioType);
        }
        columnColorList.clear();
        columnColorList = Utils.ColorUtils.makeNNumberOfColors(color, 0);
        GradientDrawable gradBack = (GradientDrawable) getResources().getDrawable(R.drawable.gradient_drawable);
        gradBack.setColors(new int[]{color, Color.WHITE});
        binding.coordLayout.setBackground(gradBack);
        binding.spinnerCardioType.setPopupBackgroundDrawable(new ColorDrawable(color));
        binding.spinnerData.setPopupBackgroundDrawable(new ColorDrawable(color));
        binding.spinnerTimeFrame.setPopupBackgroundDrawable(new ColorDrawable(color));
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
        binding.spinnerTimeFrame.setSelection(0); //default
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

    private void initGraph() {
        binding.helloGraph.setValueTouchEnabled(true);
        binding.helloGraph.setOnValueTouchListener(new ComboTouchListener());
        binding.helloGraph.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);
    }

    private int getNextColor() {
        if (colorIndex >= columnColorList.size()) {
            colorIndex = 0;
        }
        return columnColorList.get(colorIndex++);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("cardioType", cardioType);
        outState.putBoolean("initialDataLoaded", initialDataLoaded);
        outState.putInt(SPINNER_CARDIO, binding.spinnerCardioType.getSelectedItemPosition());
        outState.putInt(SPINNER_DATA, binding.spinnerData.getSelectedItemPosition());
        outState.putInt(SPINNER_TIME, binding.spinnerTimeFrame.getSelectedItemPosition());
        super.onSaveInstanceState(outState);
    }

    private void overrideInitVarsOnConfigChange(final Bundle inState) {
        binding.spinnerCardioType.setSelection(inState.getInt(SPINNER_CARDIO));
        dataType = inState.getInt(SPINNER_DATA);
        binding.spinnerData.setSelection(dataType);
        timeFrame = inState.getInt(SPINNER_TIME);
        binding.spinnerTimeFrame.setSelection(timeFrame);
        isCardioTypeSet = true;
        isDataTypeSet = true;
    }

    private void overrideInitVars() {
        int pos = Arrays.asList(getResources().getStringArray(R.array.cardio_types)).indexOf(cardioType);
        binding.spinnerCardioType.setSelection(pos);
        dataType = 2;
        binding.spinnerData.setSelection(dataType);
        isCardioTypeSet = true;
        isDataTypeSet = true;
    }

    //validate that cardioType and dataType have been chosen before trying to display the chart
    private boolean canPresentChart() {
        return isDataTypeSet && isCardioTypeSet;
    }

    private void presentChart() {
        if (canPresentChart()) {
            binding.chartTitle.setText(cardioType);
            setActivityColorScheme();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String query = getQuery();
                    Log.d(TAG, "Query: " + query);
                    Cursor c = DataModel.getInstance().rawQuery(query, null);
                    if (initialDataLoaded) {
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
        String ex = "\'" + cardioType + "\'";
        if (timeFrame == 5) {
            return ex;
        }
        String endOfQuery = null;
        String today;
        if (initialDate != null) {
            today = initialDate;
            //secondDate = initialDate;
        } else {
            today = Utils.convertDateToString(new Date(), Utils.DB_DATE_FORMAT);
        }
        String secondDate = getSecondDate();

        if (dataType == 1) {
            endOfQuery = ex + " and Data.date between \'" + secondDate + "\' and \'" + today + "\'";
        } else {
            endOfQuery = ex + " and date between '" + secondDate + "\' and \'" + today + "\'";
        }
        return endOfQuery;
    }

    private String getSecondDate() {
        Calendar cal = Calendar.getInstance();
        if (initialDate != null) {
            cal.setTime(Utils.convertStringToDate(initialDate, "MM/dd/yyyy"));
        }
        switch (timeFrame) {
            case 0:
                cal.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case 1:
                cal.add(Calendar.MONTH, -1);
                break;
            case 2:
                cal.add(Calendar.MONTH, -3);
                break;
            case 3:
                cal.add(Calendar.MONTH, -6);
                break;
            case 4:
                cal.set(Calendar.MONTH, 0);
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
        //int count = 0;
        while (!results.isAfterLast()) {
            String date = results.getString(0);
            subcolumnValues = new ArrayList<>();
            setAxisValues(colCount, results.getString(0));
            while (!results.isAfterLast() && date.equals(results.getString(0))) {
                float raw = results.getFloat(1);
                subcolumnValues.add(new SubcolumnValue(raw).setColor(getNextColor()));
                results.moveToNext();
                //count++;
            }
            Column col = new Column(subcolumnValues);
            col.setHasLabels(true);
            col.setHasLabelsOnlyForSelected(true);
            columns.add(col);
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
        //if (columns.size() > 0) {
            //sublistSize = columns.get(colCount).getValues().size();
        //}
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