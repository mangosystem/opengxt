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
import java.awt.geom.Ellipse2D;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.process.Process;
import org.geotools.process.spatialstatistics.GlobalMoransIProcess;
import org.geotools.process.spatialstatistics.GlobalMoransIProcess.MoransIProcessResult;
import org.geotools.process.spatialstatistics.GlobalMoransIProcessFactory;
import org.geotools.process.spatialstatistics.LocalMoransIProcess;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.process.spatialstatistics.styler.SSStyleBuilder;
import org.geotools.styling.Style;
import org.geotools.util.Converters;
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
import org.opengis.util.ProgressListener;

/**
 * Moran Scatter Plot Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MoranScatterPlotDialog extends AbstractGeoProcessingDialog implements
        IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(MoranScatterPlotDialog.class);

    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private ChartComposite2 chartComposite;

    private Map<String, Object> params = new HashMap<String, Object>();

    private ILayer inputLayer, outputLayer;

    private Combo cboLayer, cboField, cboConcept, cboDistance, cboStandard;

    private Browser browser;

    private CTabItem inputTab, plotTab, outputTab;

    private boolean crossCenter = false;

    private XYMinMaxVisitor minMaxVisitor = new XYMinMaxVisitor();

    public MoranScatterPlotDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.windowTitle = Messages.MoranScatterPlotDialog_title;
        this.windowDesc = Messages.MoranScatterPlotDialog_description;
        this.windowSize = new Point(650, 520);
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
        uiBuilder.createLabel(container, Messages.MoranScatterPlotDialog_InputLayer, EMPTY, image,
                1);
        cboLayer = uiBuilder.createCombo(container, 1, true);
        fillLayers(map, cboLayer, VectorLayerType.ALL);

        uiBuilder.createLabel(container, Messages.MoranScatterPlotDialog_InputField, EMPTY, image,
                1);
        cboField = uiBuilder.createCombo(container, 1, true);

        uiBuilder.createLabel(container, Messages.MoranScatterPlotDialog_Conceptualization, EMPTY,
                1);
        cboConcept = uiBuilder.createCombo(container, 1, true);
        cboConcept.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                for (Object enumVal : SpatialConcept.class.getEnumConstants()) {
                    if (enumVal.toString().equalsIgnoreCase(cboConcept.getText())) {
                        params.put(GlobalMoransIProcessFactory.spatialConcept.key, enumVal);
                        break;
                    }
                }
            }
        });
        fillEnum(cboConcept, SpatialConcept.class);

        uiBuilder.createLabel(container, Messages.MoranScatterPlotDialog_DistanceMethod, EMPTY, 1);
        cboDistance = uiBuilder.createCombo(container, 1, true);
        cboDistance.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                for (Object enumVal : DistanceMethod.class.getEnumConstants()) {
                    if (enumVal.toString().equalsIgnoreCase(cboDistance.getText())) {
                        params.put(GlobalMoransIProcessFactory.distanceMethod.key, enumVal);
                        break;
                    }
                }
            }
        });
        fillEnum(cboDistance, DistanceMethod.class);

        uiBuilder.createLabel(container, Messages.MoranScatterPlotDialog_Standardization, EMPTY, 1);
        cboStandard = uiBuilder.createCombo(container, 1, true);
        cboStandard.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                for (Object enumVal : StandardizationMethod.class.getEnumConstants()) {
                    if (enumVal.toString().equalsIgnoreCase(cboStandard.getText())) {
                        params.put(GlobalMoransIProcessFactory.standardization.key, enumVal);
                        break;
                    }
                }
            }
        });
        fillEnum(cboStandard, StandardizationMethod.class);

        uiBuilder.createLabel(container, Messages.MoranScatterPlotDialog_DistanceBand, EMPTY, 1);
        final Text txtDistance = uiBuilder.createText(container, EMPTY, 1, true);
        txtDistance.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Object obj = Converters.convert(txtDistance.getText(), Double.class);
                if (obj == null) {
                    params.put(GlobalMoransIProcessFactory.searchDistance.key, Double.valueOf(0d));
                } else {
                    params.put(GlobalMoransIProcessFactory.searchDistance.key, obj);
                }
            }
        });

        // register events
        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                inputLayer = MapUtils.getLayer(map, cboLayer.getText());
                if (inputLayer == null) {
                    return;
                }
                SimpleFeatureCollection features = MapUtils.getFeatures(inputLayer);
                params.put(GlobalMoransIProcessFactory.inputFeatures.key, features);
                fillFields(cboField, inputLayer.getSchema(), FieldType.Number);
            }
        });

        cboField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                params.put(GlobalMoransIProcessFactory.inputField.key, cboField.getText());
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
        plotTab.setText(Messages.MoranScatterPlotDialog_Graph);

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
                map.select(selectionFilter, outputLayer);
            } else {
                map.select(Filter.EXCLUDE, outputLayer);
            }
        }
    }

    private void updateChart(SimpleFeatureCollection features, String propertyName, String morani) {
        // 1. Create a single plot containing both the scatter and line
        XYPlot plot = new XYPlot();
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setDomainPannable(false);
        plot.setRangePannable(false);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(false);
        plot.setDomainCrosshairLockedOnData(true);
        plot.setRangeCrosshairLockedOnData(true);
        plot.setDomainCrosshairPaint(java.awt.Color.CYAN);
        plot.setRangeCrosshairPaint(java.awt.Color.CYAN);

        plot.setDomainGridlinePaint(java.awt.Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(java.awt.Color.LIGHT_GRAY);

        // 2. Setup Scatter plot
        // Create the scatter data, renderer, and axis
        int fontStyle = java.awt.Font.BOLD;
        FontData fontData = getShell().getDisplay().getSystemFont().getFontData()[0];
        NumberAxis xPlotAxis = new NumberAxis(propertyName); // ZScore
        xPlotAxis.setLabelFont(new Font(fontData.getName(), fontStyle, 12));
        xPlotAxis.setTickLabelFont(new Font(fontData.getName(), fontStyle, 10));

        NumberAxis yPlotAxis = new NumberAxis("Moran index"); //$NON-NLS-1$ LMiIndex
        yPlotAxis.setLabelFont(new Font(fontData.getName(), fontStyle, 12));
        yPlotAxis.setTickLabelFont(new Font(fontData.getName(), fontStyle, 10));

        XYToolTipGenerator plotToolTip = new StandardXYToolTipGenerator();

        XYItemRenderer plotRenderer = new XYLineAndShapeRenderer(false, true); // Shapes only
        plotRenderer.setSeriesShape(0, new Ellipse2D.Double(0, 0, 3, 3));
        plotRenderer.setSeriesPaint(0, java.awt.Color.BLUE); // dot
        plotRenderer.setBaseToolTipGenerator(plotToolTip);

        // Set the scatter data, renderer, and axis into plot
        plot.setDataset(0, getScatterPlotData(features));
        plot.setRenderer(0, plotRenderer);
        plot.setDomainAxis(0, xPlotAxis);
        plot.setRangeAxis(0, yPlotAxis);

        // Map the scatter to the first Domain and first Range
        plot.mapDatasetToDomainAxis(0, 0);
        plot.mapDatasetToRangeAxis(0, 0);

        // 3. Setup line
        // Create the line data, renderer, and axis
        XYItemRenderer lineRenderer = new XYLineAndShapeRenderer(true, false); // Lines only
        lineRenderer.setSeriesPaint(0, java.awt.Color.RED); // dot

        // Set the line data, renderer, and axis into plot
        NumberAxis xLineAxis = new NumberAxis(EMPTY);
        xLineAxis.setTickMarksVisible(false);
        xLineAxis.setTickLabelsVisible(false);
        NumberAxis yLineAxis = new NumberAxis(EMPTY);
        yLineAxis.setTickMarksVisible(false);
        yLineAxis.setTickLabelsVisible(false);

        plot.setDataset(1, getLinePlotData(crossCenter));
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
        String title = "Moran's I = " + morani; //$NON-NLS-1$
        java.awt.Font titleFont = new Font(fontData.getName(), fontStyle, 20);
        JFreeChart chart = new JFreeChart(title, titleFont, plot, false);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setBorderVisible(false);

        chartComposite.setChart(chart);
        chartComposite.forceRedraw();
    }

    private XYDataset getLinePlotData(boolean center) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Horizontal
        XYSeries horizontal = new XYSeries("Horizontal"); //$NON-NLS-1$
        if (center) {
            horizontal.add(-minMaxVisitor.getAbsMaxX(), 0);
            horizontal.add(minMaxVisitor.getAbsMaxX(), 0);
        } else {
            horizontal.add(minMaxVisitor.getMinX(), 0);
            horizontal.add(minMaxVisitor.getMaxX(), 0);
        }
        dataset.addSeries(horizontal);

        // Vertical
        XYSeries vertical = new XYSeries("Vertical"); //$NON-NLS-1$
        if (center) {
            vertical.add(0, -minMaxVisitor.getAbsMaxY());
            vertical.add(0, minMaxVisitor.getAbsMaxY());
        } else {
            vertical.add(0, minMaxVisitor.getMinY());
            vertical.add(0, minMaxVisitor.getMaxY());
        }
        dataset.addSeries(vertical);

        return dataset;
    }

    @SuppressWarnings("nls")
    private XYDataset getScatterPlotData(SimpleFeatureCollection features) {
        // "LMiIndex", "LMiZScore", "LMiPValue", "COType"
        XYSeries xySeries = new XYSeries(features.getSchema().getTypeName());
        minMaxVisitor.reset();

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                // TODO: recalculate local moran's i
                // The X axis of the scatter plot represents the standardised Z values of your
                // variable (that is, they’ve been standardised to their Z scores, with a mean of
                // zero, and a standard deviation of 1.)
                // The Y axis represents the standardised values of the neighbouring values around
                // your point of interest, that is the lagged values. These are calculated according
                // to the spatial weights matrix that you specify. So, for instance, if you specify
                // a contiguous spatial weights matrix, with a first order queen contiguity, the
                // value of the y axis represents the mean value of the variable for all of the
                // areas that share a border with the area of interest.
                Double x = Converters.convert(feature.getAttribute("LMiZScore"), Double.class);
                Double y = Converters.convert(feature.getAttribute("LMiIndex"), Double.class);
                if (x != null && y != null) {
                    minMaxVisitor.visit(x, y);
                    xySeries.add(new XYDataItem2(feature, x, y));
                }
            }
        } finally {
            featureIter.close();
        }

        return new XYSeriesCollection(xySeries);
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
                monitor.subTask("Preparing scatter plot...");
                createGraphTab(inputTab.getParent());
            }
            if (outputTab == null) {
                createOutputTab(inputTab.getParent());
            }
            monitor.worked(increment);

            monitor.subTask("Analyzing global moran...");
            ProgressListener subMonitor = GeoToolsAdapters.progress(SubMonitor.convert(monitor,
                    Messages.Task_Internal, increment));
            Process process = new GlobalMoransIProcess(null);
            Map<String, Object> result = process.execute(params, subMonitor);
            MoransIProcessResult moran = (MoransIProcessResult) result
                    .get(GlobalMoransIProcessFactory.RESULT.key);

            // write html
            HtmlWriter writer = new HtmlWriter(inputLayer.getName());
            writer.writeMoransI(moran);
            browser.setText(writer.getHTML());

            monitor.subTask("Analyzing local moran...");
            subMonitor = GeoToolsAdapters.progress(SubMonitor.convert(monitor,
                    Messages.Task_Internal, increment));
            process = new LocalMoransIProcess(null);
            result = process.execute(params, subMonitor);

            SimpleFeatureCollection features = (SimpleFeatureCollection) result
                    .get(GlobalMoransIProcessFactory.RESULT.key);

            monitor.subTask(Messages.Task_AddingLayer);

            SSStyleBuilder ssBuilder = new SSStyleBuilder(features.getSchema());
            ssBuilder.setOpacity(0.85f);
            Style style = ssBuilder.getLISAStyle("COType"); //$NON-NLS-1$

            outputLayer = MapUtils.addFeaturesToMap(map, features, "Local Moran's I", style);

            features = MapUtils.getFeatures(outputLayer);
            monitor.worked(increment);

            monitor.subTask("Updating scatter plot...");
            chartComposite.setLayer(outputLayer);
            updateChart(features, moran.getPropertyName(), moran.getMoran_Index());
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
}
