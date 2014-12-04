/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import java.awt.Font;
import java.awt.geom.Ellipse2D;
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
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.process.spatialstatistics.PearsonCorrelationProcess;
import org.geotools.process.spatialstatistics.StatisticsFeaturesProcess;
import org.geotools.process.spatialstatistics.core.FormatUtils;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult.DataStatisticsItem;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult.PropertyName;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult.PropertyName.PearsonItem;
import org.geotools.util.logging.Logging;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
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
import org.opengis.util.ProgressListener;

/**
 * Scatter Plot Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ScatterPlotDialog extends AbstractGeoProcessingDialog implements IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(ScatterPlotDialog.class);

    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private ChartComposite2 chartComposite;

    private ILayer inputLayer;

    private Combo cboLayer, cboXField, cboYField;

    private Button chkStatistics, chkPearson;

    private Browser browser;

    private CTabItem inputTab, plotTab, outputTab;

    private XYMinMaxVisitor minMaxVisitor = new XYMinMaxVisitor();

    public ScatterPlotDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.windowTitle = Messages.ScatterPlotDialog_title;
        this.windowDesc = Messages.ScatterPlotDialog_description;
        this.windowSize = new Point(650, 450);
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

        // 3. Output Tab
        // createOutputTab(parentTabFolder);

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
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // local moran's i
        Image image = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage(); //$NON-NLS-1$
        uiBuilder.createLabel(container, Messages.ScatterPlotDialog_InputLayer, EMPTY, image, 1);
        cboLayer = uiBuilder.createCombo(container, 1, true);
        fillLayers(map, cboLayer, VectorLayerType.ALL);

        uiBuilder.createLabel(container, Messages.ScatterPlotDialog_IndependentField, EMPTY, image,
                1);
        cboXField = uiBuilder.createCombo(container, 1, true);

        uiBuilder
                .createLabel(container, Messages.ScatterPlotDialog_DependentField, EMPTY, image, 1);
        cboYField = uiBuilder.createCombo(container, 1, true);

        uiBuilder.createLabel(container, null, null, 1);
        chkStatistics = uiBuilder.createCheckbox(container, "Calculate Basic Statistics", null, 1);
        chkPearson = uiBuilder.createCheckbox(container,
                "Calculate Pearson Correlation Coefficient", null, 1);

        // register events
        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                inputLayer = MapUtils.getLayer(map, cboLayer.getText());
                if (inputLayer != null) {
                    fillFields(cboXField, inputLayer.getSchema(), FieldType.Number);
                    fillFields(cboYField, inputLayer.getSchema(), FieldType.Number);
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

        XYPlot plot = new XYPlot();
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setDomainPannable(false);
        plot.setRangePannable(false);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        JFreeChart chart = new JFreeChart(EMPTY, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setBorderVisible(false);

        chartComposite = new ChartComposite2(parentTabFolder, SWT.NONE | SWT.EMBEDDED, chart, true);
        chartComposite.setLayout(new FillLayout());
        chartComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        chartComposite.setDomainZoomable(false);
        chartComposite.setRangeZoomable(false);
        chartComposite.setMap(map);
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
                XYSeriesCollection dataSet = (XYSeriesCollection) item.getDataset();
                XYSeries xySeries = dataSet.getSeries(item.getSeriesIndex());
                XYDataItem2 dataItem = (XYDataItem2) xySeries.getDataItem(item.getItem());

                Filter selectionFilter = ff.id(ff.featureId(dataItem.getFeature().getID()));
                map.select(selectionFilter, inputLayer);
            } else {
                map.select(Filter.EXCLUDE, inputLayer);
            }
        }
    }

    private void updateChart(SimpleFeatureCollection features, String xField, String yField) {
        // 1. Create a single plot containing both the scatter and line
        XYPlot plot = new XYPlot();
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setDomainPannable(false);
        plot.setRangePannable(false);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(false);

        // 2. Setup Scatter plot
        // Create the scatter data, renderer, and axis
        int fontStyle = java.awt.Font.BOLD;
        FontData fontData = getShell().getDisplay().getSystemFont().getFontData()[0];
        NumberAxis xPlotAxis = new NumberAxis(xField); // Independent variable
        xPlotAxis.setLabelFont(new Font(fontData.getName(), fontStyle, 12));
        xPlotAxis.setTickLabelFont(new Font(fontData.getName(), fontStyle, 10));

        NumberAxis yPlotAxis = new NumberAxis(yField); // Dependent variable
        yPlotAxis.setLabelFont(new Font(fontData.getName(), fontStyle, 12));
        yPlotAxis.setTickLabelFont(new Font(fontData.getName(), fontStyle, 10));

        XYToolTipGenerator plotToolTip = new StandardXYToolTipGenerator();

        XYItemRenderer plotRenderer = new XYLineAndShapeRenderer(false, true); // Shapes only
        plotRenderer.setSeriesShape(0, new Ellipse2D.Double(0, 0, 3, 3));
        plotRenderer.setSeriesPaint(0, java.awt.Color.BLUE); // dot
        plotRenderer.setBaseToolTipGenerator(plotToolTip);

        // Set the scatter data, renderer, and axis into plot
        plot.setDataset(0, getScatterPlotData(features, xField, yField));

        xPlotAxis.setAutoRange(false);
        xPlotAxis.setRange(minMaxVisitor.getMinX(), minMaxVisitor.getMaxX());

        yPlotAxis.setAutoRange(false);
        yPlotAxis.setRange(minMaxVisitor.getMinY(), minMaxVisitor.getMaxY());

        plot.setRenderer(0, plotRenderer);
        plot.setDomainAxis(0, xPlotAxis);
        plot.setRangeAxis(0, yPlotAxis);

        // Map the scatter to the first Domain and first Range
        plot.mapDatasetToDomainAxis(0, 0);
        plot.mapDatasetToRangeAxis(0, 0);

        // 3. Setup line
        // Create the line data, renderer, and axis
        XYItemRenderer lineRenderer = new XYLineAndShapeRenderer(true, false); // Lines only
        lineRenderer.setSeriesPaint(0, java.awt.Color.GRAY);
        lineRenderer.setSeriesPaint(1, java.awt.Color.GRAY);
        lineRenderer.setSeriesPaint(2, java.awt.Color.RED);

        // Set the line data, renderer, and axis into plot
        NumberAxis xLineAxis = new NumberAxis(EMPTY);
        xLineAxis.setTickMarksVisible(false);
        xLineAxis.setTickLabelsVisible(false);

        NumberAxis yLineAxis = new NumberAxis(EMPTY);
        yLineAxis.setTickMarksVisible(false);
        yLineAxis.setTickLabelsVisible(false);

        plot.setDataset(1, getLinePlotData());
        plot.setRenderer(1, lineRenderer);
        plot.setDomainAxis(1, xLineAxis);
        plot.setRangeAxis(1, yLineAxis);

        // Map the line to the second Domain and second Range
        plot.mapDatasetToDomainAxis(1, 0);
        plot.mapDatasetToRangeAxis(1, 0);

        // 4. Setup Selection
        NumberAxis xSelectionAxis = new NumberAxis(EMPTY);
        xSelectionAxis.setTickMarksVisible(false);
        xSelectionAxis.setTickLabelsVisible(false);

        NumberAxis ySelectionAxis = new NumberAxis(EMPTY);
        ySelectionAxis.setTickMarksVisible(false);
        ySelectionAxis.setTickLabelsVisible(false);

        XYItemRenderer selectionRenderer = new XYLineAndShapeRenderer(false, true); // Shapes only
        selectionRenderer.setSeriesShape(0, new Ellipse2D.Double(0, 0, 6, 6));
        selectionRenderer.setSeriesPaint(0, java.awt.Color.RED); // dot

        plot.setDataset(2, new XYSeriesCollection(new XYSeries(EMPTY)));
        plot.setRenderer(2, selectionRenderer);
        plot.setDomainAxis(2, xSelectionAxis);
        plot.setRangeAxis(2, ySelectionAxis);

        // Map the scatter to the second Domain and second Range
        plot.mapDatasetToDomainAxis(2, 0);
        plot.mapDatasetToRangeAxis(2, 0);

        // 5. Finally, Create the chart with the plot and a legend
        java.awt.Font titleFont = new Font(fontData.getName(), fontStyle, 20);
        JFreeChart chart = new JFreeChart(EMPTY, titleFont, plot, false);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setBorderVisible(false);

        chartComposite.setChart(chart);
        chartComposite.forceRedraw();
    }

    private XYDataset getLinePlotData() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Horizontal
        XYSeries horizontal = new XYSeries("Horizontal"); //$NON-NLS-1$
        horizontal.add(minMaxVisitor.getMinX(), minMaxVisitor.getAverageY());
        horizontal.add(minMaxVisitor.getMaxX(), minMaxVisitor.getAverageY());
        dataset.addSeries(horizontal);

        // Vertical
        XYSeries vertical = new XYSeries("Vertical"); //$NON-NLS-1$
        vertical.add(minMaxVisitor.getAverageX(), minMaxVisitor.getMinY());
        vertical.add(minMaxVisitor.getAverageX(), minMaxVisitor.getMaxY());
        dataset.addSeries(vertical);

        // Deegree
        XYSeries deegree = new XYSeries("Deegree"); //$NON-NLS-1$
        deegree.add(minMaxVisitor.getMinX(), minMaxVisitor.getMinY());
        deegree.add(minMaxVisitor.getMaxX(), minMaxVisitor.getMaxY());
        dataset.addSeries(deegree);

        return dataset;
    }

    @SuppressWarnings("nls")
    private XYDataset getScatterPlotData(SimpleFeatureCollection features, String xField,
            String yField) {
        XYSeries xySeries = new XYSeries(features.getSchema().getTypeName());
        minMaxVisitor.reset();

        Expression xExpression = ff.property(xField);
        Expression yExpression = ff.property(yField);

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();

                Double xVal = xExpression.evaluate(feature, Double.class);
                if (xVal == null || xVal.isNaN() || xVal.isInfinite()) {
                    continue;
                }

                Double yVal = yExpression.evaluate(feature, Double.class);
                if (yVal == null || yVal.isNaN() || yVal.isInfinite()) {
                    continue;
                }

                minMaxVisitor.visit(xVal, yVal);
                xySeries.add(new XYDataItem2(feature, xVal, yVal));
            }
        } finally {
            featureIter.close();
        }

        return new XYSeriesCollection(xySeries);
    }

    @Override
    protected void okPressed() {
        if (invalidWidgetValue(cboLayer, cboXField, cboYField)) {
            openInformation(getShell(), Messages.Task_ParameterRequired);
            return;
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
                monitor.subTask("Preparing scatter plot...");
                createGraphTab(inputTab.getParent());
            }

            if (outputTab == null) {
                createOutputTab(inputTab.getParent());
            }

            monitor.worked(increment);

            String xField = cboXField.getText();
            String yField = cboYField.getText();
            SimpleFeatureCollection features = MapUtils.getFeatures(inputLayer);

            String fields = xField + "," + yField;

            HtmlWriter writer = new HtmlWriter(inputLayer.getName());
            DataStatisticsResult statistics = null;
            PearsonResult pearson = null;

            if (chkStatistics.getSelection()) {
                ProgressListener subMonitor = GeoToolsAdapters.progress(SubMonitor.convert(monitor,
                        Messages.Task_Internal, 20));
                statistics = StatisticsFeaturesProcess.process(features, fields, subMonitor);
                writeStats(writer, statistics);
            }

            if (chkPearson.getSelection()) {
                ProgressListener subMonitor = GeoToolsAdapters.progress(SubMonitor.convert(monitor,
                        Messages.Task_Internal, 20));
                pearson = PearsonCorrelationProcess.process(features, fields, subMonitor);
                writePearson(writer, pearson);
            }

            if (statistics != null || statistics != null) {
                writer.close();
                browser.setText(writer.getHTML());
            }

            monitor.subTask("Updating scatter plot...");
            chartComposite.setLayer(inputLayer);
            updateChart(features, xField, yField);
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

    @SuppressWarnings("nls")
    private void writeStats(HtmlWriter writer, DataStatisticsResult statistics) {
        writer.writeH1("Summary Statistics");
        for (DataStatisticsItem item : statistics.getList()) {
            writer.writeH2(item.getTypeName() + ": " + item.getPropertyName());
            writer.write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

            // header
            writer.write("<colgroup>");
            writer.write("<col width=\"60%\" />");
            writer.write("<col width=\"40%\" />");
            writer.write("</colgroup>");

            writer.write("<tr bgcolor=\"#cccccc\">");
            writer.write("<td><strong>Category</strong></td>");
            writer.write("<td><strong>Value</strong></td>");
            writer.write("</tr>");

            // body
            writer.write("<tr><td>Count</td><td>" + item.getCount() + "</td></tr>");
            writer.write("<tr><td>Invalid Count</td><td>" + item.getInvalidCount() + "</td></tr>");
            writer.write("<tr><td>Minimum</td><td>" + item.getMinimum() + "</td></tr>");
            writer.write("<tr><td>Maximum</td><td>" + item.getMaximum() + "</td></tr>");
            writer.write("<tr><td>Range</td><td>" + item.getRange() + "</td></tr>");
            writer.write("<tr><td>Ranges</td><td>" + item.getRanges() + "</td></tr>");
            writer.write("<tr><td>Sum</td><td>" + item.getSum() + "</td></tr>");
            writer.write("<tr><td>Mean</td><td>" + item.getMean() + "</td></tr>");
            writer.write("<tr><td>Variance</td><td>" + item.getVariance() + "</td></tr>");
            writer.write("<tr><td>Standard Deviation</td><td>" + item.getStandardDeviation()
                    + "</td></tr>");
            writer.write("<tr><td>Coefficient Of Variance</td><td>"
                    + item.getCoefficientOfVariance() + "</td></tr>");
            writer.write("<tr><td>NoData</td><td>" + item.getNoData() + "</td></tr>");
            writer.write("</table>");
        }
    }

    @SuppressWarnings("nls")
    private void writePearson(HtmlWriter writer, PearsonResult pearson) {
        writer.writeH1("Pearson Correlation Coefficient");
        writer.write("<table width=\"100%\" border=\"1\"  rules=\"none\" frame=\"hsides\">");

        // header
        writer.write("<colgroup>");
        writer.write("<col width=\"40%\" />");
        writer.write("<col width=\"30%\" />");
        writer.write("<col width=\"30%\" />");
        writer.write("</colgroup>");

        writer.write("<tr bgcolor=\"#cccccc\">");
        writer.write("<td><strong> </strong></td>");
        for (PropertyName item : pearson.getProeprtyNames()) {
            writer.write("<td><strong>" + item.getName() + "</strong></td>");
        }
        writer.write("</tr>");

        // body
        for (PropertyName item : pearson.getProeprtyNames()) {
            writer.write("<tr>");
            writer.write("<td bgcolor=\"#cccccc\">" + item.getName() + "</td>");
            for (PearsonItem subItem : item.getItems()) {
                writer.write("<td>" + FormatUtils.format(subItem.getValue(), 10) + "</td>");
            }
            writer.write("</tr>");
        }
        writer.write("</table>");
    }

}
