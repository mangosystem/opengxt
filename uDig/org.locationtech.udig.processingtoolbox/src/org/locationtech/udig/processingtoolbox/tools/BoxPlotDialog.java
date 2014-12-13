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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
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
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.RectangleInsets;
import org.locationtech.udig.catalog.util.GeoToolsAdapters;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

/**
 * Box Plot(Box and Whisker) Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class BoxPlotDialog extends AbstractGeoProcessingDialog implements IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(BoxPlotDialog.class);

    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private ChartComposite chartComposite;

    private ILayer inputLayer;

    private Combo cboLayer;

    private Table schemaTable;

    private Button chkStatistics;

    private Browser browser;

    private String selectedFields;

    private CTabItem inputTab, plotTab, outputTab;

    private XYMinMaxVisitor minMaxVisitor = new XYMinMaxVisitor();

    public BoxPlotDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.windowTitle = Messages.BoxPlotDialog_title;
        this.windowDesc = Messages.BoxPlotDialog_description;
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

        uiBuilder.createLabel(container, Messages.BoxPlotDialog_Fields, EMPTY, image, 1);
        schemaTable = uiBuilder.createTable(container, new String[] { Messages.General_Name }, 1);
        schemaTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                StringBuffer buffer = new StringBuffer();
                for (TableItem item : schemaTable.getItems()) {
                    if (item.getChecked()) {
                        if (buffer.length() > 0) {
                            buffer.append(",").append(item.getText()); //$NON-NLS-1$
                        } else {
                            buffer.append(item.getText());
                        }
                    }
                }
                selectedFields = buffer.toString();
            }
        });

        uiBuilder.createLabel(container, null, null, 1);
        chkStatistics = uiBuilder.createCheckbox(container,
                Messages.ScatterPlotDialog_BasicStatistics, null, 1);

        // register events
        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                schemaTable.removeAll();
                inputLayer = MapUtils.getLayer(map, cboLayer.getText());
                if (inputLayer != null) {
                    for (AttributeDescriptor dsc : inputLayer.getSchema().getAttributeDescriptors()) {
                        Class<?> binding = dsc.getType().getBinding();
                        if (Number.class.isAssignableFrom(binding)) {
                            TableItem item = new TableItem(schemaTable, SWT.NULL);
                            item.setText(dsc.getLocalName());
                        }
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

        CategoryPlot plot = new CategoryPlot();
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setRangePannable(false);

        JFreeChart chart = new JFreeChart(EMPTY, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setBorderVisible(false);

        chartComposite = new ChartComposite(parentTabFolder, SWT.NONE | SWT.EMBEDDED, chart, true);
        chartComposite.setLayout(new FillLayout());
        chartComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        chartComposite.setDomainZoomable(false);
        chartComposite.setRangeZoomable(false);
        // chartComposite.setMap(map);
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
            // ChartEntity entity = event.getEntity();
        }
    }

    private void updateChart(SimpleFeatureCollection features, String[] fields) {
        // Setup Box plot
        int fontStyle = java.awt.Font.BOLD;
        FontData fontData = getShell().getDisplay().getSystemFont().getFontData()[0];

        CategoryAxis xPlotAxis = new CategoryAxis(EMPTY); // Type
        xPlotAxis.setLabelFont(new Font(fontData.getName(), fontStyle, 12));
        xPlotAxis.setTickLabelFont(new Font(fontData.getName(), fontStyle, 12));

        NumberAxis yPlotAxis = new NumberAxis("Value"); // Value //$NON-NLS-1$
        yPlotAxis.setLabelFont(new Font(fontData.getName(), fontStyle, 12));
        yPlotAxis.setTickLabelFont(new Font(fontData.getName(), fontStyle, 10));
        yPlotAxis.setAutoRangeIncludesZero(false);

        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setMedianVisible(true);
        renderer.setMeanVisible(false);
        renderer.setFillBox(true);
        renderer.setSeriesFillPaint(0, java.awt.Color.CYAN);
        renderer.setBaseFillPaint(java.awt.Color.CYAN);
        renderer.setBaseToolTipGenerator(new BoxAndWhiskerToolTipGenerator());

        // Set the scatter data, renderer, and axis into plot
        CategoryDataset dataset = getDataset(features, fields);
        CategoryPlot plot = new CategoryPlot(dataset, xPlotAxis, yPlotAxis, renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setRangePannable(false);

        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(false);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setForegroundAlpha(0.85f);

        // Map the scatter to the first Domain and first Range
        plot.mapDatasetToDomainAxis(0, 0);
        plot.mapDatasetToRangeAxis(0, 0);

        // 3. Setup Selection
        /*****************************************************************************************
         * CategoryAxis xSelectionAxis = new CategoryAxis(EMPTY);
         * xSelectionAxis.setTickMarksVisible(false); xSelectionAxis.setTickLabelsVisible(false);
         * 
         * NumberAxis ySelectionAxis = new NumberAxis(EMPTY);
         * ySelectionAxis.setTickMarksVisible(false); ySelectionAxis.setTickLabelsVisible(false);
         * 
         * BoxAndWhiskerRenderer selectionRenderer = new BoxAndWhiskerRenderer();
         * selectionRenderer.setSeriesShape(0, new Ellipse2D.Double(0, 0, 6, 6));
         * selectionRenderer.setSeriesPaint(0, java.awt.Color.RED); // dot
         * 
         * plot.setDataset(2, new DefaultBoxAndWhiskerCategoryDataset()); plot.setRenderer(2,
         * selectionRenderer); plot.setDomainAxis(2, xSelectionAxis); plot.setRangeAxis(2,
         * ySelectionAxis);
         * 
         * // Map the scatter to the second Domain and second Range plot.mapDatasetToDomainAxis(2,
         * 0); plot.mapDatasetToRangeAxis(2, 0);
         *****************************************************************************************/

        // 5. Finally, Create the chart with the plot and a legend
        java.awt.Font titleFont = new Font(fontData.getName(), fontStyle, 20);
        JFreeChart chart = new JFreeChart(EMPTY, titleFont, plot, false);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setBorderVisible(false);

        chartComposite.setChart(chart);
        chartComposite.forceRedraw();
    }

    private BoxAndWhiskerCategoryDataset getDataset(SimpleFeatureCollection features,
            String[] fields) {
        minMaxVisitor.reset();

        Expression[] expression = new Expression[fields.length];
        Map<String, List<Double>> listMap = new TreeMap<String, List<Double>>();
        for (int index = 0; index < expression.length; index++) {
            expression[index] = ff.property(fields[index]);
            listMap.put(fields[index], new ArrayList<Double>());
        }

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                for (int index = 0; index < expression.length; index++) {
                    Double val = expression[index].evaluate(feature, Double.class);
                    if (val == null || val.isNaN() || val.isInfinite()) {
                        continue;
                    }
                    minMaxVisitor.visit(val, val);
                    listMap.get(fields[index]).add(val);
                }
            }
        } finally {
            featureIter.close();
        }

        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        for (int index = 0; index < fields.length; index++) {
            dataset.add(listMap.get(fields[index]), "Series1", fields[index]); //$NON-NLS-1$
        }
        return dataset;
    }

    @Override
    protected void okPressed() {
        if (invalidWidgetValue(cboLayer, schemaTable) || selectedFields == null
                || selectedFields.length() == 0) {
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
                monitor.subTask("Preparing box plot...");
                createGraphTab(inputTab.getParent());
            }

            monitor.worked(increment);

            SimpleFeatureCollection features = MapUtils.getFeatures(inputLayer);
            if (chkStatistics.getSelection()) {
                if (outputTab == null) {
                    createOutputTab(inputTab.getParent());
                }

                ProgressListener subMonitor = GeoToolsAdapters.progress(SubMonitor.convert(monitor,
                        Messages.Task_Internal, 20));
                HtmlWriter writer = new HtmlWriter(inputLayer.getName());
                DataStatisticsResult statistics = null;
                statistics = StatisticsFeaturesProcess
                        .process(features, selectedFields, subMonitor);
                writer.writeDataStatistics(statistics);
                browser.setText(writer.getHTML());
            }

            monitor.subTask("Updating box plot...");
            // chartComposite.setLayer(inputLayer);
            updateChart(features, selectedFields.split(","));
            plotTab.getParent().setSelection(plotTab);
            monitor.worked(increment);
        } catch (Exception e) {
            e.printStackTrace();
            ToolboxPlugin.log(e.getMessage());
            throw new InvocationTargetException(e.getCause(), e.getMessage());
        } finally {
            ToolboxPlugin.log(String.format(Messages.Task_Completed, windowTitle));
            monitor.done();
        }
    }
}
