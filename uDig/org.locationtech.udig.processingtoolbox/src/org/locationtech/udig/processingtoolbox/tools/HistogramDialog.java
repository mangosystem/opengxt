/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.RectangularShape;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.PlatformUI;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.process.spatialstatistics.StatisticsFeaturesProcess;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult;
import org.geotools.util.logging.Logging;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.GradientPaintTransformer;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;
import org.locationtech.udig.catalog.util.GeoToolsAdapters;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.util.ProgressListener;

/**
 * Histogram Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HistogramDialog extends AbstractGeoProcessingDialog implements IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(HistogramDialog.class);

    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private ChartComposite chartComposite;

    private ILayer inputLayer;

    private Combo cboLayer, cboField;

    private Spinner spinner;

    private Button chkStatistics;

    private Browser browser;

    // default = binCount / total
    private HistogramType histogramType = HistogramType.RELATIVE_FREQUENCY;

    private CTabItem inputTab, plotTab, outputTab;

    private XYMinMaxVisitor minMaxVisitor = new XYMinMaxVisitor();

    public HistogramDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.windowTitle = Messages.HistogramDialog_title;
        this.windowDesc = Messages.HistogramDialog_description;
        this.windowSize = ToolboxPlugin.rescaleSize(parentShell, 650, 450);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        // 0. Tab Folder
        final CTabFolder parentTabFolder = new CTabFolder(area, SWT.BOTTOM);
        parentTabFolder.setUnselectedCloseVisible(false);
        parentTabFolder.setLayout(new FillLayout());
        parentTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // 1. Input Tab
        createInputTab(parentTabFolder);

        // 2. Graph Tab
        // createGraphTab(parentTabFolder);

        parentTabFolder.setSelection(inputTab);
        parentTabFolder.pack();
        area.pack(true);
        return area;
    }

    private void createInputTab(final CTabFolder parentTabFolder) {
        inputTab = new CTabItem(parentTabFolder, SWT.NONE);
        inputTab.setText(Messages.ProcessExecutionDialog_tabparameters);

        ScrolledComposite scroller = new ScrolledComposite(parentTabFolder, SWT.NONE | SWT.V_SCROLL
                | SWT.H_SCROLL);
        scroller.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite container = new Composite(scroller, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // local moran's i
        Image image = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage(); //$NON-NLS-1$
        uiBuilder.createLabel(container, Messages.HistogramDialog_InputLayer, EMPTY, image, 2);
        cboLayer = uiBuilder.createCombo(container, 2, true);
        fillLayers(map, cboLayer, VectorLayerType.ALL);

        uiBuilder.createLabel(container, Messages.HistogramDialog_InputField, EMPTY, image, 2);
        cboField = uiBuilder.createCombo(container, 2, true);

        uiBuilder.createLabel(container, Messages.HistogramDialog_BinCount, EMPTY, image, 2);
        spinner = uiBuilder.createSpinner(container, 10, 1, 50, 0, 1, 5, 2);

        // yXais Type
        uiBuilder.createLabel(container, Messages.HistogramDialog_YAxisType, EMPTY, image, 1);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;

        Composite subCon = new Composite(container, SWT.NONE);
        subCon.setLayout(layout);
        subCon.setLayoutData(new GridData(SWT.LEFT_TO_RIGHT, SWT.CENTER, false, false, 1, 1));
        final Button chkRatio = uiBuilder.createRadioButton(subCon, Messages.HistogramDialog_Ratio,
                null, 1);
        final Button chkFrequency = uiBuilder.createRadioButton(subCon,
                Messages.HistogramDialog_Frequency, null, 1);
        chkFrequency.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (chkFrequency.getSelection()) {
                    histogramType = HistogramType.FREQUENCY;
                }
            }
        });
        chkRatio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (chkRatio.getSelection()) {
                    histogramType = HistogramType.RELATIVE_FREQUENCY;
                }
            }
        });
        chkRatio.setSelection(true);

        uiBuilder.createLabel(container, null, null, 2);
        chkStatistics = uiBuilder.createCheckbox(container,
                Messages.ScatterPlotDialog_BasicStatistics, null, 2);

        // register events
        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                inputLayer = MapUtils.getLayer(map, cboLayer.getText());
                if (inputLayer != null) {
                    fillFields(cboField, inputLayer.getSchema(), FieldType.Number);
                }
            }
        });

        // finally
        scroller.setContent(container);
        inputTab.setControl(scroller);

        scroller.setMinSize(450, container.getSize().y - 2);
        scroller.setExpandVertical(true);
        scroller.setExpandHorizontal(true);

        scroller.pack();
        container.pack();
    }

    private void createOutputTab(final CTabFolder parentTabFolder) {
        outputTab = new CTabItem(parentTabFolder, SWT.NONE);
        outputTab.setText(Messages.ScatterPlotDialog_Summary);

        try {
            browser = new Browser(parentTabFolder, SWT.NONE);
            GridData layoutData = new GridData(GridData.FILL_BOTH);
            browser.setLayoutData(layoutData);
            outputTab.setControl(browser);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private void createGraphTab(final CTabFolder parentTabFolder) {
        plotTab = new CTabItem(parentTabFolder, SWT.NONE);
        plotTab.setText(Messages.ScatterPlotDialog_Graph);

        JFreeChart chart = new JFreeChart(EMPTY, JFreeChart.DEFAULT_TITLE_FONT, new XYPlot(), false);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setBorderVisible(false);

        chartComposite = new ChartComposite2(parentTabFolder, SWT.NONE | SWT.EMBEDDED, chart, true);
        chartComposite.setLayout(new FillLayout());
        chartComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        chartComposite.setDomainZoomable(false);
        chartComposite.setRangeZoomable(false);
        chartComposite.addChartMouseListener(new PlotMouseListener());

        plotTab.setControl(chartComposite);

        chartComposite.pack();
    }

    class PlotMouseListener implements ChartMouseListener {
        @Override
        public void chartMouseMoved(ChartMouseEvent event) {
        }

        @Override
        public void chartMouseClicked(ChartMouseEvent event) {
            ChartEntity entity = event.getEntity();
            if (entity != null && (entity instanceof XYItemEntity)) {
                XYItemEntity item = (XYItemEntity) entity;
                HistogramDataset dataSet = (HistogramDataset) item.getDataset();
                double min = dataSet.getStartXValue(0, item.getItem());
                double max = dataSet.getEndXValue(0, item.getItem());
                CustomXYBarPainter.selectedColumn = item.getItem();

                PropertyName property = ff.property(cboField.getText());
                Filter selectionFilter = ff.between(property, ff.literal(min), ff.literal(max));
                map.select(selectionFilter, inputLayer);
            } else {
                map.select(Filter.EXCLUDE, inputLayer);
                CustomXYBarPainter.selectedColumn = -1;
            }
        }
    }

    private void updateChart(SimpleFeatureCollection features, String field) {
        int bin = spinner.getSelection();

        double[] values = getValues(features, field);
        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries(field, values, bin, minMaxVisitor.getMinX(), minMaxVisitor.getMaxX());
        dataset.setType(histogramType);

        JFreeChart chart = ChartFactory.createHistogram(EMPTY, null, null, dataset,
                PlotOrientation.VERTICAL, false, false, false);

        // 1. Create a single plot containing both the scatter and line
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setBorderVisible(false);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setForegroundAlpha(0.85F);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setOrientation(PlotOrientation.VERTICAL);

        plot.setDomainGridlinePaint(java.awt.Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(java.awt.Color.LIGHT_GRAY);

        int fontStyle = java.awt.Font.BOLD;
        FontData fontData = getShell().getDisplay().getSystemFont().getFontData()[0];

        NumberAxis valueAxis = new NumberAxis(cboField.getText());
        valueAxis.setLabelFont(new Font(fontData.getName(), fontStyle, 12));
        valueAxis.setTickLabelFont(new Font(fontData.getName(), fontStyle, 10));

        valueAxis.setAutoRange(false);
        valueAxis.setRange(minMaxVisitor.getMinX(), minMaxVisitor.getMaxX());

        String rangeAxisLabel = histogramType == HistogramType.FREQUENCY ? "Frequency" : "Ratio"; //$NON-NLS-1$ //$NON-NLS-2$
        NumberAxis rangeAxis = new NumberAxis(rangeAxisLabel);
        rangeAxis.setLabelFont(new Font(fontData.getName(), fontStyle, 12));
        rangeAxis.setTickLabelFont(new Font(fontData.getName(), fontStyle, 10));
        if (histogramType == HistogramType.FREQUENCY) {
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        }

        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setShadowVisible(false);
        CustomXYBarPainter.selectedColumn = -1; // init
        renderer.setBarPainter(new CustomXYBarPainter());
        renderer.setAutoPopulateSeriesFillPaint(true);
        renderer.setAutoPopulateSeriesPaint(true);
        renderer.setShadowXOffset(3);
        renderer.setMargin(0.01);
        renderer.setBaseItemLabelsVisible(true);

        ItemLabelPosition pos = new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.TOP_CENTER);
        renderer.setBasePositiveItemLabelPosition(pos);

        XYToolTipGenerator plotToolTip = new StandardXYToolTipGenerator();
        renderer.setBaseToolTipGenerator(plotToolTip);

        // color
        GradientPaint gp0 = new GradientPaint(0.0f, 0.0f, java.awt.Color.GRAY, 0.0f, 0.0f,
                java.awt.Color.LIGHT_GRAY);
        renderer.setSeriesPaint(0, gp0);

        plot.setDomainAxis(0, valueAxis);
        plot.setRangeAxis(0, rangeAxis);

        // 3. Setup line
        // Create the line data, renderer, and axis
        XYItemRenderer lineRenderer = new XYLineAndShapeRenderer(true, false); // Lines only
        lineRenderer.setSeriesPaint(0, java.awt.Color.RED);
        lineRenderer.setSeriesStroke(0, new BasicStroke(2f));

        // Set the line data, renderer, and axis into plot
        NumberAxis xLineAxis = new NumberAxis(EMPTY);
        xLineAxis.setTickMarksVisible(false);
        xLineAxis.setTickLabelsVisible(false);
        xLineAxis.setAutoRange(false);

        NumberAxis yLineAxis = new NumberAxis(EMPTY);
        yLineAxis.setTickMarksVisible(false);
        yLineAxis.setTickLabelsVisible(false);
        yLineAxis.setAutoRange(false);

        double maxYValue = Double.MIN_VALUE;
        for (int i = 0; i < dataset.getItemCount(0); i++) {
            maxYValue = Math.max(maxYValue, dataset.getYValue(0, i));
        }

        XYSeriesCollection lineDatset = new XYSeriesCollection();

        // Vertical Average
        XYSeries vertical = new XYSeries("Average"); //$NON-NLS-1$
        vertical.add(minMaxVisitor.getAverageX(), 0);
        vertical.add(minMaxVisitor.getAverageX(), maxYValue);
        lineDatset.addSeries(vertical);

        plot.setDataset(1, lineDatset);
        plot.setRenderer(1, lineRenderer);
        plot.setDomainAxis(1, xLineAxis);
        plot.setRangeAxis(1, yLineAxis);

        // Map the line to the second Domain and second Range
        plot.mapDatasetToDomainAxis(1, 0);
        plot.mapDatasetToRangeAxis(1, 0);

        chartComposite.setChart(chart);
        chartComposite.forceRedraw();
    }

    private double[] getValues(SimpleFeatureCollection features, String field) {
        minMaxVisitor.reset();

        double[] values = new double[features.size()];
        SimpleFeatureIterator featureIter = features.features();
        try {
            Expression expression = ff.property(field);
            int index = 0;
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Double val = expression.evaluate(feature, Double.class);
                if (val == null || val.isNaN() || val.isInfinite()) {
                    continue;
                }
                values[index++] = val;
                minMaxVisitor.visit(val, val);
            }
        } finally {
            featureIter.close();
        }

        return values;
    }

    @Override
    protected void okPressed() {
        if (invalidWidgetValue(cboLayer, cboField)) {
            openInformation(getShell(), Messages.Task_ParameterRequired);
            return;
        }

        if (inputLayer.getFilter() != Filter.EXCLUDE) {
            map.select(Filter.EXCLUDE, inputLayer);
        }

        try {
            PlatformUI.getWorkbench().getProgressService().run(false, true, this);
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), Messages.General_Error, e.getMessage());
        } catch (InterruptedException e) {
            MessageDialog.openInformation(getShell(), Messages.General_Cancelled, e.getMessage());
        }
    }

    @SuppressWarnings("nls")
    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        monitor.beginTask(String.format(Messages.Task_Executing, windowTitle), 100);
        try {
            if (plotTab == null) {
                monitor.subTask("Preparing Histogram...");
                createGraphTab(inputTab.getParent());
            }
            monitor.worked(increment);

            String field = cboField.getText();
            SimpleFeatureCollection features = MapUtils.getFeatures(inputLayer);

            if (chkStatistics.getSelection()) {
                monitor.subTask("Calculating Basic Statistics...");
                if (outputTab == null) {
                    createOutputTab(inputTab.getParent());
                }
                ProgressListener subMonitor = GeoToolsAdapters.progress(SubMonitor.convert(monitor,
                        Messages.Task_Internal, 20));
                DataStatisticsResult statistics = StatisticsFeaturesProcess.process(features,
                        field, subMonitor);

                HtmlWriter writer = new HtmlWriter(inputLayer.getName());
                writer.writeDataStatistics(statistics);
                browser.setText(writer.getHTML());
            }

            monitor.subTask("Updating Histogram...");
            updateChart(features, field);
            plotTab.getParent().setSelection(plotTab);
            monitor.worked(increment);
        } catch (Exception e) {
            ToolboxPlugin.log(e.getMessage());
            throw new InvocationTargetException(e.getCause(), e.getMessage());
        } finally {
            ToolboxPlugin.log(String.format(Messages.Task_Completed, windowTitle));
            monitor.done();
        }
    }

    // Custom XYBarPainter for selection item
    static final class CustomXYBarPainter extends StandardXYBarPainter {

        private static final long serialVersionUID = 1334758953034367259L;

        static int selectedColumn = -1;

        private Paint selectionPaint = java.awt.Color.CYAN;

        public CustomXYBarPainter() {

        }

        @Override
        public void paintBar(Graphics2D g2, XYBarRenderer renderer, int row, int column,
                RectangularShape bar, RectangleEdge base) {
            Paint itemPaint = selectedColumn == column ? selectionPaint : renderer.getItemPaint(
                    row, column);

            GradientPaintTransformer t = renderer.getGradientPaintTransformer();
            if (t != null && itemPaint instanceof GradientPaint) {
                itemPaint = t.transform((GradientPaint) itemPaint, bar);
            }
            g2.setPaint(itemPaint);
            g2.fill(bar);

            // draw the outline...
            if (renderer.isDrawBarOutline()) {
                // && state.getBarWidth() > BAR_OUTLINE_WIDTH_THRESHOLD) {
                Stroke stroke = renderer.getItemOutlineStroke(row, column);
                Paint paint = renderer.getItemOutlinePaint(row, column);
                if (stroke != null && paint != null) {
                    g2.setStroke(stroke);
                    g2.setPaint(paint);
                    g2.draw(bar);
                }
            }
        }
    }
}
