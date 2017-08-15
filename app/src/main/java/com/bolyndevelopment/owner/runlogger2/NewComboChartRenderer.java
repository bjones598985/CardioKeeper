package com.bolyndevelopment.owner.runlogger2;

import android.content.Context;

import lecho.lib.hellocharts.provider.ColumnChartDataProvider;
import lecho.lib.hellocharts.provider.LineChartDataProvider;
import lecho.lib.hellocharts.renderer.ColumnChartRenderer;
import lecho.lib.hellocharts.renderer.ComboLineColumnChartRenderer;
import lecho.lib.hellocharts.renderer.LineChartRenderer;
import lecho.lib.hellocharts.view.Chart;

/**
 * Created by Bobby Jones on 8/14/2017.
 */

public class NewComboChartRenderer extends ComboLineColumnChartRenderer {
    LineChartDataProvider provider;

    public NewComboChartRenderer(Context context, Chart chart, ColumnChartDataProvider columnChartDataProvider, LineChartDataProvider lineChartDataProvider) {
        super(context, chart, columnChartDataProvider, lineChartDataProvider);
        this.provider = lineChartDataProvider;
    }

    public NewComboChartRenderer(Context context, Chart chart, ColumnChartRenderer columnChartRenderer, LineChartDataProvider lineChartDataProvider) {
        super(context, chart, columnChartRenderer, lineChartDataProvider);
        this.provider = lineChartDataProvider;
    }

    public NewComboChartRenderer(Context context, Chart chart, ColumnChartDataProvider columnChartDataProvider, LineChartRenderer lineChartRenderer) {
        super(context, chart, columnChartDataProvider, lineChartRenderer);
    }

    public NewComboChartRenderer(Context context, Chart chart, ColumnChartRenderer columnChartRenderer, LineChartRenderer lineChartRenderer) {
        super(context, chart, columnChartRenderer, lineChartRenderer);
    }
}
