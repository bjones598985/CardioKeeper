package com.bolyndevelopment.owner.runlogger2;

import android.content.Context;
import android.util.AttributeSet;

import lecho.lib.hellocharts.renderer.ComboLineColumnChartRenderer;
import lecho.lib.hellocharts.renderer.LineChartRenderer;
import lecho.lib.hellocharts.view.ComboLineColumnChartView;

/**
 * Created by Bobby Jones on 8/14/2017.
 */

public class ComboChartView extends ComboLineColumnChartView {
    public ComboChartView(Context context) {
        super(context);
    }

    public ComboChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ComboChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setChartRenderer(new ComboLineColumnChartRenderer(context, this, columnChartDataProvider,
                new NewLineChartRenderer(context, this, lineChartDataProvider)));
    }
}
