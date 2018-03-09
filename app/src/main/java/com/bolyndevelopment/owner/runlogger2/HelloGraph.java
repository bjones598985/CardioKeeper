package com.bolyndevelopment.owner.runlogger2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityGraphsV2Binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import es.dmoral.toasty.Toasty;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ComboLineColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.ComboLineColumnChartData;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;

public class HelloGraph extends AppCompatActivity {
    //Created by Bobby Jones on 8/11/2017
    public static final String TAG = "HelloGraph";

    private static final String SPINNER_CARDIO = "spinner_cardio";
    private static final String SPINNER_DATA = "spinner_data";
    private static final String SPINNER_TIME = "spinner_time";

    int mainColor; //color based on the cardioType

    int screenWidth;

    boolean isMenuButtonHidden = false; //flag for use in toggleMenuButton()

    boolean initialDataLoaded = false; //once set to true, column data is updated instead of generated

    String distUnit; //miles or kilometers

    int colorIndex = 0; // index of colors to use to color the columns

    List<Integer> columnColorList = new ArrayList<>(); // list of colors for coloring the columns

    Handler handler = new Handler();

    List<Float> rawData;

    List<AxisValue> axisValues;

    ColumnChartData columnData;

    LineChartData lineData;

    ActivityGraphsV2Binding binding;

    boolean isDataTypeSet = false, isCardioTypeSet = false, includeLapData = false;


    int dataType, timeFrame = 0;

    String cardioType, yAxisLabel, initialDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_graphs_v2);

        /*
         * set distance variable to miles or kilometers
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
        binding.menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenuButton();
            }
        });

        binding.include.runQueryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentChart();
                toggleMenuButton();
            }
        });
        binding.include.cancelMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenuButton();
            }
        });

        getDisplayInfo();

    }

    private void getDisplayInfo() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
    }

    private void setInitialPrefs() {
        final SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String distPref = sPrefs.getString(getResources().getString(R.string.pref_distance), "-1");
        distUnit = distPref.equals("-1") ? getResources().getString(R.string.miles) : getResources().getString(R.string.kilos);
    }

    private void setActivityColorScheme() {
        if (cardioType == null) {
            mainColor = getResources().getColor(R.color.colorPrimary);
        } else {
            mainColor = Utils.ColorUtils.getCardioColor(cardioType);
        }
        columnColorList.clear();
        columnColorList = Utils.ColorUtils.makeNNumberOfColors(mainColor, 0);
        GradientDrawable gradBack = (GradientDrawable) getResources().getDrawable(R.drawable.gradient_drawable);
        gradBack.setColors(new int[]{mainColor, Color.WHITE});
        binding.coordLayout.setBackground(gradBack);
        binding.include.spinnerCardioType.setPopupBackgroundDrawable(new ColorDrawable(mainColor));
        binding.include.spinnerData.setPopupBackgroundDrawable(new ColorDrawable(mainColor));
        binding.include.spinnerTimeFrame.setPopupBackgroundDrawable(new ColorDrawable(mainColor));
    }

    public void onCheckBoxClicked(View v) {
        includeLapData = ((CheckBox) v).isChecked();
        columnData.setStacked(includeLapData);
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
        binding.include.spinnerData.setAdapter(dataAdapter);
        //this is necessary so that on layout it doesnt fire
        binding.include.spinnerData.setSelection(0, false);
        binding.include.spinnerData.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dataType = position;
                isDataTypeSet = position != 0;
                //only stack columns when displaying lap data
                //columnData.setStacked(position == 1);
                if (position == 1) {
                    binding.include.includeLapsCheckbox.setVisibility(View.VISIBLE);
                } else {
                    binding.include.includeLapsCheckbox.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing, we don't care
            }
        });

        final ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(this, R.array.time_frame, R.layout.spinner_item);
        timeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        binding.include.spinnerTimeFrame.setAdapter(timeAdapter);
        binding.include.spinnerTimeFrame.setSelection(0); //default
        binding.include.spinnerTimeFrame.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        binding.include.spinnerCardioType.setAdapter(typeAdapter);
        binding.include.spinnerCardioType.setSelection(0, false);
        binding.include.spinnerCardioType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        outState.putInt(SPINNER_CARDIO, binding.include.spinnerCardioType.getSelectedItemPosition());
        outState.putInt(SPINNER_DATA, binding.include.spinnerData.getSelectedItemPosition());
        outState.putInt(SPINNER_TIME, binding.include.spinnerTimeFrame.getSelectedItemPosition());
        super.onSaveInstanceState(outState);
    }

    private void overrideInitVarsOnConfigChange(final Bundle inState) {
        binding.include.spinnerCardioType.setSelection(inState.getInt(SPINNER_CARDIO));
        dataType = inState.getInt(SPINNER_DATA);
        binding.include.spinnerData.setSelection(dataType);
        timeFrame = inState.getInt(SPINNER_TIME);
        binding.include.spinnerTimeFrame.setSelection(timeFrame);
        isCardioTypeSet = true;
        isDataTypeSet = true;
    }

    private void overrideInitVars() {
        int pos = Arrays.asList(getResources().getStringArray(R.array.cardio_types)).indexOf(cardioType);
        binding.include.spinnerCardioType.setSelection(pos);
        dataType = 2;
        binding.include.spinnerData.setSelection(dataType);
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
            binding.helloGraph.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String query = getQuery();
                    Log.d(TAG, "query: " + query);
                    Cursor c = DataModel.getInstance().rawQuery(query, null);
                    DatabaseUtils.dumpCursor(c);
                    if (initialDataLoaded) {
                        if(includeLapData) {
                            updateColumnValuesWithLapData(c);
                        } else {
                            updateColumnValues(c);
                        }
                    } else {
                        generateColumnData(c);
                        initialDataLoaded = true;
                    }
                    setAxesAndDisplay();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            binding.helloGraph.setOnValueTouchListener(new ComboTouchListener());
                        }
                    });
                }
            }).start();
        }
    }

    private String getQuery() {
        String query = null;
        String midQueryPart = buildAndRetrieveMidQueryPart();
        switch (dataType) {
            case 1:
                if (includeLapData) {
                    query = "select Data.date, round(Lap.time * 1.0 / 60000) as Mins, Lap.lap_num, Data.sequence from Lap inner join Data on Data._id=Lap.workout_id where cardio_type=" + midQueryPart + " order by Data.date asc";
                    yAxisLabel = "Minutes";
                    break;
                } else {
                    query = "select date, round(time * 1.0 / 60000) as Mins from Data where cardio_type=" + midQueryPart + " order by date asc";
                    yAxisLabel = "Minutes";
                    break;
                }
            case 2:
                query = "select date, distance from Data where cardio_type=" + midQueryPart + " order by date asc";
                yAxisLabel = distUnit;
                break;
            case 3:
                query = "select date, calories from Data where cardio_type=" + midQueryPart + " order by date asc";
                yAxisLabel = "Calories";
                break;
            case 4:
                query = "select date, distance / round(time * 1.0 / 3600000) from Data where cardio_type=" + midQueryPart + " order by date asc";
                yAxisLabel = "Speed";
                break;
            case 5:
                query = "select date, calories / round(time * 1.0 / 3600000) from Data where cardio_type=" + midQueryPart + " order by date asc";
                yAxisLabel = "Calories / Hour";
                break;
            case 6:
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
        String endOfQuery;
        String today;
        if (initialDate != null) {
            today = initialDate;
        } else {
            today = Utils.convertDateToString(new Date(), DataModel.DATE_FORMAT);
        }
        String secondDate = getSecondDate();

        if (dataType == 1) {
            endOfQuery = ex + " and Data.date >= \'" + secondDate + "\' and Data.date <= \'" + today + "\'";
        } else {
            endOfQuery = ex + " and date >= '" + secondDate + "\' and date <= \'" + today + "\'";
        }
        return endOfQuery;
    }

    private String getSecondDate() {
        Calendar cal = Calendar.getInstance();
        if (initialDate != null) {
            cal.setTime(Utils.convertStringToDate(initialDate, DataModel.DATE_FORMAT));
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
                cal.add(Calendar.YEAR, -1);
                break;
            case 5:
                cal.add(Calendar.YEAR, -10);
            default:
        }
        return Utils.convertDateToString(cal.getTime(), DataModel.DATE_FORMAT);
    }

    private void generateColumnData(Cursor results) {
        results.moveToFirst();
        if (results.getCount() == 0) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toasty.info(getBaseContext(), "Uh oh, your search turned up no results", Toast.LENGTH_LONG, true).show();
                }
            });
        }
        List<SubcolumnValue> subcolumnValues;
        List<Column> columns = new ArrayList<>();
        axisValues.clear();
        int colCount = 0;
        while (!results.isAfterLast()) {
            String date = results.getString(0);
            subcolumnValues = new ArrayList<>();
            setAxisValues(colCount, results.getString(0));
            while (!results.isAfterLast() && date.equals(results.getString(0))) {
                float raw = results.getFloat(1);
                subcolumnValues.add(new SubcolumnValue(raw).setColor(getNextColor()));
                results.moveToNext();
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
        final ComboLineColumnChartData chartData = binding.helloGraph.getComboLineColumnChartData();
        final List<Column> columnsList = chartData.getColumnChartData().getColumns();

        int newColCount = getNewColumnCount(results);
        int oldColumnCount = columnsList.size();

        results.moveToFirst();
        /*
        if the cursor returns no results, we'll reduce the
        currently displaying column/subcolumn values to zero
         */
        if (newColCount == 0) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toasty.info(getBaseContext(), "Uh oh, your search turned up no results", Toast.LENGTH_LONG, true).show();
                }
            });
        }
        int colCount = 0;
        int sublistSize = 0;

        /*
        if the new columns are greater than zero, we're going to pass through
        at least one time and based on date and sequence, we may interate
        more times
         */
        if (newColCount > 0) {
            if (oldColumnCount == 0) {
                columnsList.add(new Column());
                oldColumnCount = 1;
            }
            axisValues.clear();

            do {
                int subColCount = 0;
                String date = results.getString(0);
                setAxisValues(colCount, date);
                sublistSize = columnsList.get(colCount).getValues().size();
                DatabaseUtils.dumpCurrentRow(results);
                while (!results.isAfterLast() && date.equals(results.getString(0))) {
                    float raw = results.getFloat(1);
                    if (subColCount >= sublistSize) {
                        columnsList.get(colCount).getValues().add(new SubcolumnValue().setValue(0).setTarget(raw).setColor(getNextColor()));
                    } else {
                        columnsList.get(colCount).getValues().get(subColCount).setTarget(raw).setColor(getNextColor());
                    }
                    results.moveToNext();
                    subColCount++;
                }
                if (columnsList.get(colCount).getValues().size() > subColCount) {
                    for (int z = subColCount; z < columnsList.get(colCount).getValues().size(); z++) {
                        columnsList.get(colCount).getValues().get(z).setTarget(0);
                    }
                }
                colCount++;

            } while (!results.isAfterLast() && (colCount < oldColumnCount));
        }

        if (newColCount > oldColumnCount) {
            while (!results.isAfterLast()) {
                String date = results.getString(0);
                setAxisValues(colCount, date);
                List<SubcolumnValue> sublist = new ArrayList<>();
                DatabaseUtils.dumpCurrentRow(results);
                while (!results.isAfterLast() && date.equals(results.getString(0))) {
                    float raw = results.getFloat(1);
                    sublist.add(new SubcolumnValue(0).setTarget(raw).setColor(getNextColor()));
                    results.moveToNext();
                }
                columnsList.add(new Column(sublist).setHasLabelsOnlyForSelected(true));
                colCount++;
            }
        }
        //obvious
        for (Column col : columnsList) {
            col.setHasLabelsOnlyForSelected(true);
        }

        //let's get rid of the data we no longer need

        if (colCount > 0 && oldColumnCount > colCount) {
            for (int i = oldColumnCount - 1; i >= colCount; i--) {
                binding.helloGraph.getComboLineColumnChartData().getColumnChartData().getColumns().remove(i);
            }
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                binding.helloGraph.startDataAnimation(1500);
            }
        });
    }

    private void updateColumnValuesWithLapData(Cursor results) {
        final ComboLineColumnChartData chartData = binding.helloGraph.getComboLineColumnChartData();
        final List<Column> columnsList = chartData.getColumnChartData().getColumns();

        int newColCount = getNewColumnCountWithLaps(results);
        int oldColumnCount = columnsList.size();

        results.moveToFirst();
        /*
        if the cursor returns no results, we'll reduce the
        currently displaying column/subcolumn values to zero
         */
        if (newColCount == 0) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toasty.info(getBaseContext(), "Uh oh, your search turned up no results", Toast.LENGTH_LONG, true).show();
                }
            });
        }

        int colCount = 0;
        int sublistSize = 0;

        /*
        if the new columns are greater than zero, we're going to pass through
        at least one time and based on date and sequence, we may interate
        more times
         */
        if (newColCount > 0) {
            if (oldColumnCount == 0) {
                columnsList.add(new Column());
                oldColumnCount = 1;
            }
            axisValues.clear();

            do {
                int subColCount = 0;
                String date = results.getString(0);
                int seq = results.getInt(3);
                setAxisValues(colCount, date);
                sublistSize = columnsList.get(colCount).getValues().size();
                DatabaseUtils.dumpCurrentRow(results);
                while (!results.isAfterLast() && date.equals(results.getString(0)) && seq == results.getInt(3)) {
                    float raw = results.getFloat(1);
                    if (subColCount >= sublistSize) {
                        columnsList.get(colCount).getValues().add(new SubcolumnValue().setValue(0).setTarget(raw).setColor(getNextColor()));
                    } else {
                        columnsList.get(colCount).getValues().get(subColCount).setTarget(raw).setColor(getNextColor());
                    }
                    results.moveToNext();
                    subColCount++;
                }
                if (columnsList.get(colCount).getValues().size() > subColCount) {
                    for (int z = subColCount; z < columnsList.get(colCount).getValues().size(); z++) {
                        columnsList.get(colCount).getValues().get(z).setTarget(0);
                    }
                }
                colCount++;

            } while (!results.isAfterLast() && (colCount < oldColumnCount));
        }

        if (newColCount > oldColumnCount) {
            while (!results.isAfterLast()) {
                String date = results.getString(0);
                int seq = results.getInt(3);
                setAxisValues(colCount, date);
                List<SubcolumnValue> sublist = new ArrayList<>();
                DatabaseUtils.dumpCurrentRow(results);
                while (!results.isAfterLast() && date.equals(results.getString(0)) && seq == results.getInt(3)) {
                    float raw = results.getFloat(1);
                    sublist.add(new SubcolumnValue(0).setTarget(raw).setColor(getNextColor()));
                    results.moveToNext();
                }
                columnsList.add(new Column(sublist).setHasLabelsOnlyForSelected(true));
                colCount++;
            }
        }
        //obvious
        for (Column col : columnsList) {
            col.setHasLabelsOnlyForSelected(true);
        }

        //let's get rid of the data we no longer need

        if (colCount > 0 && oldColumnCount > colCount) {
            for (int i = oldColumnCount - 1; i >= colCount; i--) {
                binding.helloGraph.getComboLineColumnChartData().getColumnChartData().getColumns().remove(i);
            }
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
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

    private int getNewColumnCountWithLaps(Cursor results) {
        results.moveToFirst();
        int colCount = 0;
        while (!results.isAfterLast()) {
            String date = results.getString(0);
            int seq = results.getInt(3);
            while (!results.isAfterLast() && date.equals(results.getString(0)) && (seq == results.getInt(3))) {
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

    private void toggleMenuButton() {
        if (isMenuButtonHidden) {
            binding.include.spinnerLayout.animate().alpha(0).setDuration(250L).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    binding.include.spinnerLayout.animate().setListener(null);
                    binding.include.spinnerLayout.setVisibility(View.INVISIBLE);
                    binding.menuButton.animate().alpha(1).setDuration(100L).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            binding.menuButton.animate().setListener(null);
                            binding.menuButton.setVisibility(View.VISIBLE);
                        }
                    }).start();
                }
            }).start();
            isMenuButtonHidden = false;
        } else {
            binding.menuButton.animate().alpha(0).setDuration(100L).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    binding.menuButton.animate().setListener(null);
                    binding.menuButton.setVisibility(View.INVISIBLE);
                    binding.include.spinnerLayout.setBackgroundColor(Utils.ColorUtils.darkenColor(mainColor));
                    binding.include.spinnerLayout.animate().alpha(1).setDuration(250L).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            binding.include.spinnerLayout.animate().setListener(null);
                            binding.include.spinnerLayout.setVisibility(View.VISIBLE);
                            binding.include.spinnerLayout.setElevation(32f);
                        }
                    }).start();
                }
            }).start();
            isMenuButtonHidden = true;
        }
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