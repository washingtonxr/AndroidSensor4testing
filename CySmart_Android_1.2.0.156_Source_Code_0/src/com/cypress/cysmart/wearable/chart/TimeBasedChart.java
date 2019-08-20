/*
 * Copyright Cypress Semiconductor Corporation, 2014-2018 All rights reserved.
 *
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign), United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate license agreement between you and Cypress, this Software
 * must be treated like any other copyrighted material. Reproduction,
 * modification, translation, compilation, or representation of this
 * Software in any other form (e.g., paper, magnetic, optical, silicon)
 * is prohibited without Cypress's express written permission.
 *
 * Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * NONINFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE. Cypress reserves the right to make changes
 * to the Software without notice. Cypress does not assume any liability
 * arising out of the application or use of Software or any product or
 * circuit described in the Software. Cypress does not authorize its
 * products for use as critical components in any products where a
 * malfunction or failure may reasonably be expected to result in
 * significant injury or death ("High Risk Product"). By including
 * Cypress's product in a High Risk Product, the manufacturer of such
 * system or application assumes all risk of such use and in doing so
 * indemnifies Cypress against all liability.
 *
 * Use of this Software may be limited by and subject to the applicable
 * Cypress software license agreement.
 *
 *
 */

package com.cypress.cysmart.wearable.chart;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;

import com.cypress.cysmart.CommonUtils.ChartUtils;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.LinkedList;
import java.util.List;

public class TimeBasedChart {

    private static final boolean FILL_POINTS = false;
    private static final int LINE_WIDTH = 3;
    private static final int AXIS_TITLE_TEXT_SIZE = 50;
    private static final int CHART_TITLE_TEXT_SIZE = 50;
    private static final int LABELS_TEXT_SIZE = 30;
    private static final int LEGEND_TEXT_SIZE = 30;
    private static final boolean ZOOM_ENABLED = false;
    public static final int[] sColors = {Color.RED, Color.GREEN, Color.BLUE};

    private final Context mContext;
    private final XYMultipleSeriesRenderer mMultiRenderer;
    private final XYMultipleSeriesDataset mMultiDataset;
    public final GraphicalView mChart;

    private TimeBasedChart(Context context, String chartTitle, String[] seriesTitles) {
        this.mContext = context;
        mMultiDataset = new XYMultipleSeriesDataset();
        mMultiRenderer = new XYMultipleSeriesRenderer();
        for (int i = 0; i < seriesTitles.length; i++) {
            mMultiDataset.addSeries(new XYSeries(seriesTitles[i]));
            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(sColors[i % sColors.length]);
            r.setFillPoints(FILL_POINTS);
            r.setLineWidth(LINE_WIDTH);
            mMultiRenderer.addSeriesRenderer(r);
        }
        mMultiRenderer.setMargins(new int[]{
                40, 90, 25, 10
        });
        mMultiRenderer.setAxisTitleTextSize(AXIS_TITLE_TEXT_SIZE);
        mMultiRenderer.setChartTitleTextSize(CHART_TITLE_TEXT_SIZE);
        mMultiRenderer.setLabelsTextSize(LABELS_TEXT_SIZE);
        mMultiRenderer.setLegendTextSize(LEGEND_TEXT_SIZE);
        mMultiRenderer.setChartTitle(chartTitle);
        mMultiRenderer.setMarginsColor(Color.argb(0xFF, 0xFF, 0xFF, 0xFF));
        mMultiRenderer.setPanEnabled(ChartUtils.PAN_X_ENABLED, ChartUtils.PAN_Y_ENABLED);
        mMultiRenderer.setZoomEnabled(ChartUtils.ZOOM_X_ENABLED, ChartUtils.ZOOM_Y_ENABLED);
        mMultiRenderer.setGridColor(Color.LTGRAY);
        mMultiRenderer.setLabelsColor(Color.BLACK);
        mMultiRenderer.setYLabelsAlign(Paint.Align.RIGHT);
        mMultiRenderer.setXLabelsColor(Color.BLACK);
        mMultiRenderer.setYLabelsColor(0, Color.BLACK);
        mMultiRenderer.setApplyBackgroundColor(true);
        mMultiRenderer.setBackgroundColor(0xFFFFFFFF);
        mMultiRenderer.setShowGrid(true);
        mMultiRenderer.setShowLegend(true);
        mChart = ChartFactory.getLineChartView(context, mMultiDataset, mMultiRenderer);
    }

    public static TimeBasedChart newInstance(Context context, String chartTitle, String[] seriesTitles) {
        return new TimeBasedChart(context, chartTitle, seriesTitles);
    }

    public static TimeBasedChart newXyzInstance(Context context, String chartTitle) {
        return newInstance(context, chartTitle, new String[]{"x", "y", "z"});
    }

    public static TimeBasedChart newRollPitchYawInstance(Context context, String chartTitle) {
        TimeBasedChart f = new TimeBasedChart(context, chartTitle, new String[]{"Roll", "Pitch", "Yaw"});
        f.mMultiRenderer.setYAxisMin(-180);
        f.mMultiRenderer.setYAxisMax(180);
        return f;
    }

    public static TimeBasedChart newInstanceFrom(TimeBasedChart old) {
        List<String> seriesTitles = new LinkedList<>();
        for (XYSeries s : old.mMultiDataset.getSeries()) {
            seriesTitles.add(s.getTitle());
        }
        TimeBasedChart c = new TimeBasedChart(old.mContext, old.mMultiRenderer.getChartTitle(), seriesTitles.toArray(new String[0]));
        c.mMultiRenderer.setYAxisMin(old.mMultiRenderer.getYAxisMin());
        c.mMultiRenderer.setYAxisMax(old.mMultiRenderer.getYAxisMax());
        return c;
    }

    public void repaint(LinkedList<double[]> deque) {
        if (deque.size() > 0) {
            double last = 0;
            for (double[] tuple = null; (tuple = deque.pollFirst()) != null; ) {
                for (int i = 0; i < tuple.length; i++) {
                    last = tuple[0];
                    for (int j = 0; j < mMultiDataset.getSeriesCount(); j++) {
                        mMultiDataset.getSeriesAt(j).add(tuple[0], tuple[1 + j]);
                    }
                }
            }
            // the following two lines required for the chart to get automatically scrolled on the right side
            mMultiRenderer.setXAxisMin(0);
            mMultiRenderer.setXAxisMax(last);
        }
        mChart.repaint();
    }
}
