package com.bolyndevelopment.owner.runlogger2;

import android.content.Context;

import lecho.lib.hellocharts.provider.LineChartDataProvider;
import lecho.lib.hellocharts.renderer.LineChartRenderer;
import lecho.lib.hellocharts.view.Chart;

/**
 * Created by Bobby Jones on 8/14/2017.
 */

public class NewLineChartRenderer extends LineChartRenderer {
    public NewLineChartRenderer(Context context, Chart chart, LineChartDataProvider dataProvider) {
        super(context, chart, dataProvider);
        labelPaint.setColor(context.getResources().getColor(R.color.colorPrimary));
    }
}
