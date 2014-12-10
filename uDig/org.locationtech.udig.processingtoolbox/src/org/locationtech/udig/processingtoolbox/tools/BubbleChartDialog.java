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

import java.awt.Font;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.process.spatialstatistics.StatisticsFeaturesProcess;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult;
import org.geotools.util.logging.Logging;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.BubbleXYItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardXYZToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBubbleRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.RectangleInsets;
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
import org.opengis.filter.identity.FeatureId;
import org.opengis.util.ProgressListener;

/**
 * Bubble Chart Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class BubbleChartDialog extends AbstractGeoProcessingDialog implements IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(BubbleChartDialog.class);

    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private ChartComposite3 chartComposite;

    private ILayer inputLayer;

    private Combo cboLayer, cboXField, cboYField, cboSize;

    private Button chkStatistics;

    private Browser browser;

    private CTabItem inputTab, plotTab, outputTab;

    private XYMinMaxVisitor minMaxVisitor = new XYMinMaxVisitor();

    public BubbleChartDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.windowTitle = Messages.BubbleChartDialog_title;
        this.windowDesc = Messages.BubbleChartDialog_description;
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

        uiBuilder.createLabel(container, Messages.BubbleChartDialog_XField, EMPTY, image, 1);
        cboXField = uiBuilder.createCombo(container, 1, true);

        uiBuilder.createLabel(container, Messages.BubbleChartDialog_YField, EMPTY, image, 1);
        cboYField = uiBuilder.createCombo(container, 1, true);

        uiBuilder.createLabel(container, Messages.BubbleChartDialog_SizeField, EMPTY, image, 1);
        cboSize = uiBuilder.createCombo(container, 1, true);

        uiBuilder.createLabel(container, null, null, 1);
        chkStatistics = uiBuilder.createCheckbox(container,
                Messages.ScatterPlotDialog_BasicStatistics, null, 1);

        // register events
        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                inputLayer = MapUtils.getLayer(map, cboLayer.getText());
                if (inputLayer != null) {
                    fillFields(cboXField, inputLayer.getSchema(), FieldType.Number);
                    fillFields(cboYField, inputLayer.getSchema(), FieldType.Number);
                    fillFields(cboSize, inputLayer.getSchema(), FieldType.Number);
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

        chartComposite = new ChartComposite3(parentTabFolder, SWT.NONE | SWT.EMBEDDED, chart, true);
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
            DefaultXYZDataset2 ds = (DefaultXYZDataset2) chartComposite.getChart().getXYPlot()
                    .getDataset(2);
            ChartEntity entity = event.getEntity();
            if (entity != null && (entity instanceof XYItemEntity)) {
                XYItemEntity item = (XYItemEntity) entity;
                if (item.getSeriesIndex() == 0) {
                    DefaultXYZDataset2 dataSet = (DefaultXYZDataset2) item.getDataset();
                    String featureID = dataSet.getFeatrureID(0, item.getItem());
                    Filter selectionFilter = ff.id(ff.featureId(featureID));
                    map.select(selectionFilter, inputLayer);

                    ds.addSeries(EMPTY,
                            new double[][] { new double[] { dataSet.getXValue(0, item.getItem()) },
                                    new double[] { dataSet.getYValue(0, item.getItem()) },
                                    new double[] { dataSet.getZValue(0, item.getItem()) } });
                    ds.addFeatrureIDS(EMPTY, new String[] { featureID });
                } else {
                    map.select(Filter.EXCLUDE, inputLayer);
                    ds.removeSeries(EMPTY);
                }
            } else {
                map.select(Filter.EXCLUDE, inputLayer);
                ds.removeSeries(EMPTY);
            }
        }
    }

    private void updateChart(SimpleFeatureCollection features, String xField, String yField,
            String sizeField) {
        // 1. Create a single plot containing both the scatter and line
        XYPlot plot = new XYPlot();
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setDomainPannable(false);
        plot.setRangePannable(false);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(false);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setForegroundAlpha(0.75f);

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

        XYItemRenderer plotRenderer = new XYBubbleRenderer(XYBubbleRenderer.SCALE_ON_RANGE_AXIS);
        plotRenderer.setSeriesPaint(0, java.awt.Color.ORANGE); // dot
        plotRenderer.setBaseItemLabelGenerator(new BubbleXYItemLabelGenerator());
        plotRenderer.setBaseToolTipGenerator(new StandardXYZToolTipGenerator());
        plotRenderer.setBasePositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.CENTER,
                TextAnchor.CENTER));

        // Set the scatter data, renderer, and axis into plot
        plot.setDataset(0, getScatterPlotData(features, xField, yField, sizeField));

        xPlotAxis.setAutoRange(false);
        xPlotAxis.setRange(minMaxVisitor.getMinX(), minMaxVisitor.getMaxX());

        //yPlotAxis.setAutoRange(false);
        //yPlotAxis.setRange(minMaxVisitor.getMinY(), minMaxVisitor.getMaxY());
        yPlotAxis.setAutoRangeIncludesZero(false);

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
        lineRenderer.setSeriesPaint(2, java.awt.Color.GRAY);

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

        XYItemRenderer selectionRenderer = new XYBubbleRenderer(
                XYBubbleRenderer.SCALE_ON_RANGE_AXIS);
        selectionRenderer.setSeriesPaint(0, java.awt.Color.RED); // dot
        selectionRenderer.setSeriesOutlinePaint(0, java.awt.Color.RED);

        plot.setDataset(2, new DefaultXYZDataset2());
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
        // XYSeries deegree = new XYSeries("Deegree"); //$NON-NLS-1$
        // deegree.add(minMaxVisitor.getMinX(), minMaxVisitor.getMinY());
        // deegree.add(minMaxVisitor.getMaxX(), minMaxVisitor.getMaxY());
        // dataset.addSeries(deegree);

        return dataset;
    }

    private XYZDataset getScatterPlotData(SimpleFeatureCollection features, String xField,
            String yField, String sizeField) {
        DefaultXYZDataset2 xyzDataset = new DefaultXYZDataset2();

        // 1. prepare bubble size
        minMaxVisitor.visit(features, xField, yField, sizeField);

        final double minVal = minMaxVisitor.getMinZ();
        final double maxVal = minMaxVisitor.getMaxZ();
        final double diffVal = maxVal - minVal;
        final double scale = Math.min(minMaxVisitor.getMaxX(), minMaxVisitor.getMaxY()) / 10d;

        // 2. calculate x, y, z values
        final int featureCount = features.size();
        double[] xAxis = new double[featureCount];
        double[] yAxis = new double[featureCount];
        double[] zAxis = new double[featureCount];
        String[] featureIDS = new String[featureCount];

        Expression xExpression = ff.property(xField);
        Expression yExpression = ff.property(yField);
        Expression sizeExpression = ff.property(sizeField);

        int index = 0;
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                featureIDS[index] = feature.getID();

                Double xVal = xExpression.evaluate(feature, Double.class);
                if (xVal == null || xVal.isNaN() || xVal.isInfinite()) {
                    continue;
                }

                Double yVal = yExpression.evaluate(feature, Double.class);
                if (yVal == null || yVal.isNaN() || yVal.isInfinite()) {
                    continue;
                }

                Double sizeVal = sizeExpression.evaluate(feature, Double.class);
                if (sizeVal == null || sizeVal.isNaN() || sizeVal.isInfinite()) {
                    continue;
                }

                xAxis[index] = xVal;
                yAxis[index] = yVal;

                double transformed = 0;
                if (diffVal != 0) {
                    transformed = (sizeVal - minVal) / diffVal;
                }

                zAxis[index] = transformed * scale;
                index++;
            }
        } finally {
            featureIter.close();
        }

        xyzDataset.addSeries(EMPTY, new double[][] { xAxis, yAxis, zAxis });
        xyzDataset.addFeatrureIDS(EMPTY, featureIDS);

        return xyzDataset;
    }

    @Override
    protected void okPressed() {
        if (invalidWidgetValue(cboLayer, cboXField, cboYField, cboSize)) {
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
                monitor.subTask("Preparing bubble chart...");
                createGraphTab(inputTab.getParent());
            }

            monitor.worked(increment);

            String xField = cboXField.getText();
            String yField = cboYField.getText();
            String sizeField = cboSize.getText();
            SimpleFeatureCollection features = MapUtils.getFeatures(inputLayer);

            String fields = xField + "," + yField + "," + sizeField;

            if (chkStatistics.getSelection()) {
                if (outputTab == null) {
                    createOutputTab(inputTab.getParent());
                }

                ProgressListener subMonitor = GeoToolsAdapters.progress(SubMonitor.convert(monitor,
                        Messages.Task_Internal, 20));
                DataStatisticsResult statistics = StatisticsFeaturesProcess.process(features,
                        fields, subMonitor);
                HtmlWriter writer = new HtmlWriter(inputLayer.getName());
                writer.writeDataStatistics(statistics);
                browser.setText(writer.getHTML());
            }

            monitor.subTask("Updating bubble chart...");
            chartComposite.setLayer(inputLayer);
            updateChart(features, xField, yField, sizeField);
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

    class ChartComposite3 extends ChartComposite {

        private final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

        private org.locationtech.udig.project.internal.Map map;

        private ILayer layer;

        public ChartComposite3(Composite comp, int style, JFreeChart chart, boolean useBuffer) {
            super(comp, style, chart, useBuffer);
        }

        public org.locationtech.udig.project.internal.Map getMap() {
            return map;
        }

        public void setMap(org.locationtech.udig.project.internal.Map map) {
            this.map = map;
        }

        public ILayer getLayer() {
            return layer;
        }

        public void setLayer(ILayer layer) {
            this.layer = layer;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void zoom(Rectangle selection) {
            if (map == null || layer == null) {
                return;
            }

            DefaultXYZDataset2 ds = (DefaultXYZDataset2) getChart().getXYPlot().getDataset(2);
            List<XYZItem> itemList = new ArrayList<XYZItem>();
            try {
                EntityCollection entities = this.getChartRenderingInfo().getEntityCollection();
                Iterator iter = entities.iterator();
                while (iter.hasNext()) {
                    ChartEntity entity = (ChartEntity) iter.next();
                    if (entity instanceof XYItemEntity) {
                        XYItemEntity item = (XYItemEntity) entity;
                        if (item.getSeriesIndex() != 0
                                || !(item.getDataset() instanceof DefaultXYZDataset2)) {
                            continue;
                        }

                        java.awt.Rectangle bound = item.getArea().getBounds();
                        if (selection.intersects(bound.x, bound.y, bound.width, bound.height)) {
                            DefaultXYZDataset2 dataSet = (DefaultXYZDataset2) item.getDataset();
                            String featureID = dataSet.getFeatrureID(0, item.getItem());
                            itemList.add(new XYZItem(featureID,
                                    dataSet.getXValue(0, item.getItem()), dataSet.getYValue(0,
                                            item.getItem()), dataSet.getZValue(0, item.getItem())));
                        }
                    }
                }
            } catch (Exception e) {
                // skip
            } finally {
                if (itemList.size() > 0) {
                    Set<FeatureId> selected = new HashSet<FeatureId>();
                    double[] xAxis = new double[itemList.size()];
                    double[] yAxis = new double[itemList.size()];
                    double[] zAxis = new double[itemList.size()];
                    String[] featureIDS = new String[itemList.size()];
                    for (int i = 0; i < itemList.size(); i++) {
                        XYZItem item = itemList.get(i);
                        xAxis[i] = item.x;
                        yAxis[i] = item.y;
                        zAxis[i] = item.z;
                        featureIDS[i] = item.featureID;
                        selected.add(ff.featureId(item.featureID));
                    }

                    ds.addSeries(EMPTY, new double[][] { xAxis, yAxis, zAxis });
                    ds.addFeatrureIDS(EMPTY, featureIDS);
                    map.select(ff.id(selected), layer);
                } else {
                    map.select(Filter.EXCLUDE, layer);
                    ds.removeSeries(EMPTY);
                }
                this.forceRedraw();
            }
        }

        @Override
        public void restoreAutoBounds() {
            return;
        }

        final class XYZItem {
            public double x;

            public double y;

            public double z;

            public String featureID;

            public XYZItem() {

            }

            public XYZItem(String featureID, double x, double y, double z) {
                this.featureID = featureID;
                this.x = x;
                this.y = y;
                this.z = z;
            }
        }

    }
}
