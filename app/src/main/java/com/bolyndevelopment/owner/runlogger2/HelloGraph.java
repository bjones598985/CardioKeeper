package com.bolyndevelopment.owner.runlogger2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
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
import java.util.Random;

import es.dmoral.toasty.Toasty;
import lecho.lib.hellocharts.animation.ChartAnimationListener;
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

    boolean isFabHidden = false; //flag for use in toggleFabMenu()

    boolean initialDataLoaded = false; //once set to true, column data is update instead of generated

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
        binding.fabMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFabMenu();
            }
        });

        binding.include.runQueryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentChart();
                toggleFabMenu();
            }
        });
        binding.include.cancelMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFabMenu();
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
                    Cursor c = DataModel.getInstance().rawQuery(query, null);
                    if (initialDataLoaded) {
                        updateColumnValues(c);
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
                    query = "select Data.date, round(Lap.time * 1.0 / 60000) as Mins, Lap.lap_num from Lap inner join Data on Data._id=Lap.workout_id where cardio_type=" + midQueryPart + " order by Data.date asc";
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
        if (results.getCount() == 0) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toasty.info(getBaseContext(), "Uh oh, your search turned up no results", Toast.LENGTH_LONG, true).show();
                }
            });
        }
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

    private void toggleFabMenu() {
        final FloatingActionButton fab = binding.fabMenuButton;
        final ViewGroup layout = binding.include.spinnerLayout;

        if (isFabHidden) {
            fab.setTranslationX(0);
            fab.setTranslationY(0);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fab.show();
                }
            }, 300);
            layout.animate().alpha(0).setDuration(500).start();
            layout.setVisibility(View.INVISIBLE);
            isFabHidden = false;
        } else {
            layout.setBackgroundColor(Utils.ColorUtils.darkenColor(mainColor));
            int[] spinLayCoords = new int[2];
            layout.getLocationOnScreen(spinLayCoords);
            int[] newCoordinates = getNewCoordinates();
            final Path path = getPath(spinLayCoords[0] + newCoordinates[0], spinLayCoords[1] + newCoordinates[1]);

            final Animator transAnim = getTranslateAnim(path);
            final Animator colorAnim = getColorAnim();
            final Animator circularRevealAnim = getCircleRevealAnim(newCoordinates[0] + fab.getWidth() / 2, newCoordinates[1] + fab.getHeight() / 2);

            AnimatorSet setOne = new AnimatorSet();
            setOne.playTogether(transAnim, colorAnim);
            AnimatorSet setTwo = new AnimatorSet();
            setTwo.playSequentially(setOne, circularRevealAnim);
            setTwo.start();
            isFabHidden = true;
        }
    }

    private int[] getNewCoordinates() {
        int fabDiam = binding.fabMenuButton.getWidth();
        int spinRadX = binding.include.spinnerLayout.getWidth();
        int spinRadY = binding.include.spinnerLayout.getHeight();
        spinRadX -= fabDiam;
        spinRadY -= fabDiam;
        Random random = new Random();
        int x = random.nextInt(spinRadX);
        int y = random.nextInt(spinRadY);
        return new int[]{x, y};
    }

    private Animator getTranslateAnim(Path p) {
        final ObjectAnimator transAnim = ObjectAnimator.ofFloat(binding.fabMenuButton, View.X, View.Y, p);
        transAnim.setDuration(500);
        transAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                transAnim.removeAllListeners();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                binding.fabMenuButton.setCompatElevation(0f);
            }
        });
        return transAnim;
    }

    private Animator getColorAnim() {
        final float[] from = new float[3], to = new float[3];
        Color.colorToHSV(binding.fabMenuButton.getBackgroundTintList().getDefaultColor(), from);
        Color.colorToHSV(mainColor, to);
        ValueAnimator colorAnim = ValueAnimator.ofFloat(0, 1);
        colorAnim.setDuration(500);
        final float[] hsv  = new float[3];
        colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                // Transition along each axis of HSV (hue, saturation, value)
                //code copied from https://stackoverflow.com/questions/18216285/android-animate-color-change-from-color-to-color
                hsv[0] = from[0] + (to[0] - from[0]) * animation.getAnimatedFraction();
                hsv[1] = from[1] + (to[1] - from[1]) * animation.getAnimatedFraction();
                hsv[2] = from[2] + (to[2] - from[2]) * animation.getAnimatedFraction();
                binding.fabMenuButton.setBackgroundTintList(ColorStateList.valueOf(Color.HSVToColor(hsv)));
            }
        });
        return colorAnim;
    }

    private Animator getCircleRevealAnim(int xCoord, int yCoord) {
        float finalRadius = (float) Math.hypot(xCoord, yCoord);
        final Animator circularRevealAnim = ViewAnimationUtils.createCircularReveal(binding.include.spinnerLayout, xCoord, yCoord, binding.fabMenuButton.getWidth() / 2, finalRadius*10);
        circularRevealAnim.setDuration(500);
        circularRevealAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                binding.include.spinnerLayout.setAlpha(1f);
                binding.include.spinnerLayout.setVisibility(View.VISIBLE);
                binding.include.spinnerLayout.setElevation(32f);
            }
        });
        circularRevealAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                binding.fabMenuButton.hide();
                binding.fabMenuButton.setCompatElevation(32f);
                //binding.include.spinnerLayout.setElevation(32f);
            }
        });
        return circularRevealAnim;
    }

    private Path getPath(float endX, float endY) {
        int seed = screenWidth - binding.fabMenuButton.getWidth();
        Path path = new Path();
        final View view = binding.fabMenuButton;
        path.moveTo(view.getX(),view.getY());
        path.cubicTo(view.getX(),view.getY(),
                view.getX() - (new Random().nextInt(seed) + binding.fabMenuButton.getWidth() / 2),view.getY(),
                endX, endY);
        return path;
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