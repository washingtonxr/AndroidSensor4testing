package com.cypress.cysmart.CommonUtils;

import android.content.Context;
import android.view.MotionEvent;

import org.achartengine.GraphicalView;
import org.achartengine.chart.LineChart;
import org.achartengine.chart.XYChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

public class ChartUtils {

    public static final boolean PAN_X_ENABLED = true;
    public static final boolean PAN_Y_ENABLED = true;
    public static final boolean ZOOM_X_ENABLED = true;
    public static final boolean ZOOM_Y_ENABLED = true;

    public static final GraphicalView getLineChartView(Context context, final XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer multiRenderer) {
        checkParameters(dataset, multiRenderer);

        XYChart chart = new LineChart(dataset, multiRenderer);
        final boolean panEnabled = multiRenderer.isPanEnabled();
        return new GraphicalView(context, chart) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (!panEnabled) { // No pan enabled -> fall back to default implementation
                    return super.onTouchEvent(event);
                }
                XYSeries[] series = dataset.getSeries();
                if (series != null && series.length > 0) {
                    if (series[0].getItemCount() > 4) { // panEnabled + empty series = non working chart
                        return super.onTouchEvent(event);
                    }
                }
                return true; // Ignoring touch event
            }
        };
//        return ChartFactory.getLineChartView(context, dataset, renderer);
    }

    private static void checkParameters(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
        if (dataset == null || renderer == null || dataset.getSeriesCount() != renderer.getSeriesRendererCount()) {
            throw new IllegalArgumentException("Dataset and renderer should be not null and should have the same number of series");
        }
    }
}
