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

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.jfree.experimental.chart.swt.ChartComposite;
import org.locationtech.udig.catalog.util.GeoToolsAdapters;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.style.sld.SLDContent;
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

    private ChartComposite chartComposite;

    private XYSeries xySeries;

    private Map<String, Object> params = new HashMap<String, Object>();

    private ILayer inputLayer, outputLayer;

    private Combo cboLayer, cboField, cboConcept, cboDistance, cboStandard;

    private Text txtOutput;

    private CTabItem inputTab, plotTab, outputTab;

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
        uiBuilder.createLabel(container, "Input Layer", EMPTY, image, 1);
        cboLayer = uiBuilder.createCombo(container, 1, true);
        fillLayers(map, cboLayer, VectorLayerType.ALL);

        uiBuilder.createLabel(container, "Input Field", EMPTY, image, 1);
        cboField = uiBuilder.createCombo(container, 1, true);

        uiBuilder.createLabel(container, "Conceptualization of Spatial Relationships", EMPTY, 1);
        cboConcept = uiBuilder.createCombo(container, 1, true);
        fillEnum(cboConcept, SpatialConcept.class);

        uiBuilder.createLabel(container, "Distance Method", EMPTY, 1);
        cboDistance = uiBuilder.createCombo(container, 1, true);
        fillEnum(cboDistance, DistanceMethod.class);

        uiBuilder.createLabel(container, "Row Standardization", EMPTY, 1);
        cboStandard = uiBuilder.createCombo(container, 1, true);
        fillEnum(cboStandard, StandardizationMethod.class);

        uiBuilder.createLabel(container, "Distance Band or Threshold Distance", EMPTY, 1);
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

        // output
        locationView = new OutputDataWidget(FileDataType.SHAPEFILE, SWT.SAVE);
        locationView.create(container, SWT.BORDER, 1, 1);

        // register events
        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                inputLayer = MapUtils.getLayer(map, cboLayer.getText());
                if (inputLayer == null) {
                    return;
                }
                SimpleFeatureCollection features = MapUtils.getFeatures(map, inputLayer.getName());
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
        outputTab.setText(Messages.ProcessExecutionDialog_taboutput);

        try {
            txtOutput = new Text(parentTabFolder, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
            txtOutput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
            outputTab.setControl(txtOutput);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private void createGraphTab(final CTabFolder parentTabFolder) {
        plotTab = new CTabItem(parentTabFolder, SWT.NONE);
        plotTab.setText(Messages.MoranScatterPlotDialog_title);

        XYPlot plot = new XYPlot();
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setDomainPannable(false);
        plot.setRangePannable(false);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);

        JFreeChart chart = new JFreeChart(EMPTY, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setBorderVisible(false);

        chartComposite = new ChartComposite(parentTabFolder, SWT.NONE | SWT.EMBEDDED, chart, true);
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
                XYDataItem2 dataItem = (XYDataItem2) xySeries.getDataItem(item.getItem());

                Filter selectionFilter = ff.id(ff.featureId(dataItem.getFeature().getID()));
                map.select(selectionFilter, outputLayer);

                System.out.println(dataItem.getFeature());
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

        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setDomainCrosshairLockedOnData(true);
        plot.setRangeCrosshairLockedOnData(true);
        plot.setDomainCrosshairPaint(java.awt.Color.CYAN);
        plot.setRangeCrosshairPaint(java.awt.Color.CYAN);

        plot.setDomainGridlinePaint(java.awt.Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(java.awt.Color.LIGHT_GRAY);

        // 2. Setup Scatter plot
        // Create the scatter data, renderer, and axis
        NumberAxis xAxis = new NumberAxis(propertyName); // ZScore
        xAxis.setAutoRangeIncludesZero(false);
        // xAxis.setLabelFont(JFreeChart.DEFAULT_TITLE_FONT);
        NumberAxis yAxis = new NumberAxis("Moran index"); //$NON-NLS-1$ LMiIndex
        yAxis.setAutoRangeIncludesZero(false);
        // yAxis.setLabelFont(JFreeChart.DEFAULT_TITLE_FONT);

        XYToolTipGenerator toolTipGenerator = new StandardXYToolTipGenerator();

        // Shape shape = ShapeUtilities.createDiagonalCross(2, 1)
        Shape shape = new Ellipse2D.Double(0, 0, 4, 4);
        XYItemRenderer plotRenderer = new XYLineAndShapeRenderer(false, true); // Shapes only
        plotRenderer.setSeriesShape(0, shape);
        plotRenderer.setSeriesPaint(0, java.awt.Color.BLUE); // dot
        plotRenderer.setBaseToolTipGenerator(toolTipGenerator);

        // Set the scatter data, renderer, and axis into plot
        plot.setDataset(0, getScatterPlotData(features));
        plot.setRenderer(0, plotRenderer);
        plot.setDomainAxis(0, xAxis);
        plot.setRangeAxis(0, yAxis);

        // Map the scatter to the first Domain and first Range
        plot.mapDatasetToDomainAxis(0, 0);
        plot.mapDatasetToRangeAxis(0, 0);

        // 3. Setup line
        // Create the line data, renderer, and axis
        XYItemRenderer lineRenderer = new XYLineAndShapeRenderer(true, false); // Lines only
        lineRenderer.setSeriesPaint(0, java.awt.Color.RED); // dot

        // Set the line data, renderer, and axis into plot
        plot.setDataset(1, getLinePlotData());
        plot.setRenderer(1, lineRenderer);
        plot.setDomainAxis(1, new NumberAxis(EMPTY));
        plot.setRangeAxis(1, new NumberAxis(EMPTY));

        // Map the line to the second Domain and second Range
        plot.mapDatasetToDomainAxis(1, 1);
        plot.mapDatasetToRangeAxis(1, 1);

        // 4. Finally, Create the chart with the plot and a legend
        String title = "Moran's I = " + morani; //$NON-NLS-1$
        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setBorderVisible(false);

        chartComposite.setChart(chart);
        chartComposite.forceRedraw();
    }

    private XYDataset getLinePlotData() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Horizontal
        XYSeries horizontal = new XYSeries("Horizontal"); //$NON-NLS-1$
        horizontal.add(minMaxVisitor.getMinX(), 0);
        horizontal.add(minMaxVisitor.getMaxX(), 0);
        dataset.addSeries(horizontal);

        // Vertical
        XYSeries vertical = new XYSeries("Vertical"); //$NON-NLS-1$
        vertical.add(0, minMaxVisitor.getMinY());
        vertical.add(0, minMaxVisitor.getMaxY());
        dataset.addSeries(vertical);

        return dataset;
    }

    @SuppressWarnings("nls")
    private XYDataset getScatterPlotData(SimpleFeatureCollection features) {
        // "LMiIndex", "LMiZScore", "LMiPValue", "COType"
        xySeries = new XYSeries(features.getSchema().getTypeName());
        minMaxVisitor.reset();

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
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

        try {
            PlatformUI.getWorkbench().getProgressService().run(false, true, this);
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), Messages.General_Error, e.getMessage());
        } catch (InterruptedException e) {
            MessageDialog.openInformation(getShell(), Messages.General_Cancelled, e.getMessage());
        }
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        monitor.beginTask(Messages.Task_Running, 100);
        try {
            monitor.setTaskName(String.format(Messages.Task_Executing, windowTitle));
            if (plotTab == null) {
                monitor.subTask("Preparing scatter plot...");
                createGraphTab(inputTab.getParent());
                monitor.worked(increment);
            }
            if (outputTab == null) {
                createOutputTab(inputTab.getParent());
            }

            monitor.subTask("Analyzing global moran...");
            ProgressListener subMonitor = GeoToolsAdapters.progress(SubMonitor.convert(monitor,
                    Messages.Task_Internal, increment));
            Process process = new GlobalMoransIProcess(null);
            Map<String, Object> result = process.execute(params, subMonitor);
            MoransIProcessResult moran = (MoransIProcessResult) result
                    .get(GlobalMoransIProcessFactory.RESULT.key);
            txtOutput.setText(moran.toString());

            monitor.subTask("Analyzing local moran...");
            subMonitor = GeoToolsAdapters.progress(SubMonitor.convert(monitor,
                    Messages.Task_Internal, increment));
            process = new LocalMoransIProcess(null);
            result = process.execute(params, subMonitor);

            SimpleFeatureCollection features = (SimpleFeatureCollection) result
                    .get(GlobalMoransIProcessFactory.RESULT.key);

            monitor.subTask(Messages.Task_AddingLayer);
            if (locationView.getFile().length() == 0) {
                // temporary
                outputLayer = MapUtils.addFeaturesToMap(map, features, "Local Moran's I");
            } else {
                // export
                outputLayer = MapUtils.addFeaturesToMap(map, features, "Local Moran's I");
            }

            SSStyleBuilder ssBuilder = new SSStyleBuilder(outputLayer.getSchema());
            ssBuilder.setOpacity(0.8f);
            Style style = ssBuilder.getLISAStyle("COType"); //$NON-NLS-1$
            if (style != null) {
                // put the style on the blackboard
                outputLayer.getStyleBlackboard().clear();
                outputLayer.getStyleBlackboard().put(SLDContent.ID, style);
                outputLayer.getStyleBlackboard().flush();
                outputLayer.refresh(outputLayer.getBounds(new NullProgressMonitor(), null));
            }
            features = MapUtils.getFeatures(outputLayer);
            monitor.worked(increment);

            monitor.subTask("Updating scatter plot...");
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
