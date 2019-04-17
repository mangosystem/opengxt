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

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.enumeration.ResampleType;
import org.geotools.process.spatialstatistics.gridcoverage.GridTransformer;
import org.geotools.process.spatialstatistics.gridcoverage.RasterClipOperation;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHelper;
import org.geotools.process.spatialstatistics.gridcoverage.RasterReprojectOperation;
import org.geotools.process.spatialstatistics.gridcoverage.RasterResampleOperation;
import org.geotools.process.spatialstatistics.storage.RasterExportOperation;
import org.geotools.process.spatialstatistics.styler.SSStyleBuilder;
import org.geotools.process.spatialstatistics.transformation.GXTSimpleFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.jaitools.tiledimage.DiskMemImage;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.internal.ui.WidgetBuilder;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.parameter.Parameter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Raster Calculator Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class RasterCalculatorDialog extends AbstractGeoProcessingDialog implements
        IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(RasterCalculatorDialog.class);

    private final String space = " ";

    private final Color warningColor = new Color(Display.getCurrent(), 255, 255, 200);

    private IMap map = null;

    private Button btnClear;

    private Table layerTable, functionTable;

    private Combo cboExtent, cboCellSize;

    private Text txtExpression;

    private double minSizeX, minSizeY, maxSizeX, maxSizeY;

    private double cellSizeX, cellSizeY;

    private Map<String, GridCoverage2D> coverages = new TreeMap<String, GridCoverage2D>();

    public RasterCalculatorDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        this.map = map;
        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);

        this.windowTitle = Messages.RasterCalculatorDialog_title;
        this.windowDesc = Messages.RasterCalculatorDialog_description;
        this.windowSize = ToolboxPlugin.rescaleSize(parentShell, 650, 550);
    }

    /**
     * Create contents of the button bar.
     * 
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // Clear, Test, Save..., Load..., OK, Cancel
        btnClear = createButton(parent, 2000, Messages.ExpressionBuilderDialog_Clear, false);
        btnClear.setEnabled(false);

        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

        btnClear.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                txtExpression.setText("");
            }
        });
    }

    /**
     * Create contents of the dialog.
     * 
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = layout.marginHeight = layout.marginRight = layout.marginBottom = 2;
        area.setLayout(layout);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        WidgetBuilder widget = WidgetBuilder.newInstance();
        final int scale = (int) getShell().getDisplay().getDPI().x / 96;

        // calculate cell size
        calculateMinMaxCellSize();

        // ========================================================
        // 1. Extent & Cell Size
        // ========================================================
        Group grpExtent = widget.createGroup(container,
                Messages.RasterCalculatorDialog_ExtentandCell, false, 1, 80 * scale, 2);
        grpExtent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        widget.createLabel(grpExtent, Messages.RasterCalculatorDialog_Extent, null, 1);
        cboExtent = widget.createCombo(grpExtent, 1, true);

        cboExtent.add(Messages.RasterCalculatorDialog_Intersection);
        cboExtent.add(Messages.RasterCalculatorDialog_Union);

        widget.createLabel(grpExtent, Messages.RasterCalculatorDialog_Cell, null, 1);
        cboCellSize = widget.createCombo(grpExtent, 1, true);

        cboCellSize.add(Messages.RasterCalculatorDialog_Maximum);
        cboCellSize.add(Messages.RasterCalculatorDialog_Minimum);
        for (Entry<String, GridCoverage2D> entry : coverages.entrySet()) {
            cboCellSize.add(entry.getKey());
            cboExtent.add(entry.getKey());
        }

        cboCellSize.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                int index = cboCellSize.getSelectionIndex();
                if (index == 0) {
                    // Maximum
                    cellSizeX = maxSizeX;
                    cellSizeY = maxSizeY;
                } else if (index == 1) {
                    // Minimum
                    cellSizeX = minSizeX;
                    cellSizeY = minSizeY;
                } else if (index >= 2) {
                    GridCoverage2D coverage = coverages.get(cboCellSize.getText());
                    GridGeometry2D gridGeometry2D = coverage.getGridGeometry();
                    AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

                    cellSizeX = Math.abs(gridToWorld.getScaleX());
                    cellSizeY = Math.abs(gridToWorld.getScaleY());
                }
            }
        });

        // ========================================================
        // 2. Layers & Functions
        // ========================================================
        Group grpFunctions = widget.createGroup(container,
                Messages.RasterCalculatorDialog_LayersandFunctions, false, 1);

        final int defaultWidth = 300 * scale;
        Group grpFields = widget.createGroup(grpFunctions,
                Messages.RasterCalculatorDialog_RasterLayers, false, 1);
        GridData gridDataField = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
        gridDataField.widthHint = defaultWidth;
        grpFields.setLayoutData(gridDataField);
        grpFields.setLayout(new GridLayout(1, true));

        layerTable = widget.createListTable(grpFields,
                new String[] { Messages.FieldCalculatorDialog_Fields }, 1, 100 * scale);
        updateLayers();
        layerTable.getColumns()[0].setWidth(defaultWidth - 40);

        // double click event
        layerTable.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                String selection = layerTable.getSelection()[0].getText();
                updateExpression(selection);
            }
        });

        // ========================================================
        // filter functions
        // http://docs.geotools.org/latest/userguide/library/main/filter.html
        // ========================================================
        Group grpValues = widget.createGroup(grpFunctions,
                Messages.FieldCalculatorDialog_Functions, false, 1);
        grpValues.setLayout(new GridLayout(1, true));

        functionTable = widget.createListTable(grpValues,
                new String[] { Messages.FieldCalculatorDialog_Functions }, 1, 100 * scale);
        updateFunctions();
        functionTable.getColumns()[0].setWidth(280 * scale);
        grpValues.setText(Messages.FieldCalculatorDialog_Functions + "("
                + functionTable.getItemCount() + ")");

        // double click event
        functionTable.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (functionTable.getSelectionCount() > 0) {
                    String selection = functionTable.getSelection()[0].getText();
                    updateExpression(selection);
                }
            }
        });

        // ========================================================
        // 3. Operators & Expression
        // ========================================================
        Group grpOperations = new Group(container, SWT.SHADOW_ETCHED_IN);
        grpOperations.setText(Messages.FieldCalculatorDialog_Operators);
        grpOperations.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        grpOperations.setLayout(new GridLayout(16, true));

        // operators
        GridData btnLayout = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        final String[] operations = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "0", "+", "-", "*", "/", "(", ")" };
        final List<String> exceptions = Arrays.asList(new String[] { "+", "-", "*", "/" });

        Button[] btnOp = new Button[operations.length];
        for (int idx = 0; idx < operations.length; idx++) {
            btnOp[idx] = widget.createButton(grpOperations, operations[idx], operations[idx],
                    btnLayout);
            btnOp[idx].addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    Button current = (Button) event.widget;
                    if (exceptions.contains(current.getText())) {
                        updateExpression(space + current.getText() + space);
                    } else {
                        updateExpression(current.getText());
                    }
                }
            });
        }

        // expression
        txtExpression = new Text(grpOperations, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData txtGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 16, 1);
        txtGridData.heightHint = 60 * scale;
        txtExpression.setLayoutData(txtGridData);

        final Color oldBackColor = txtExpression.getBackground();
        txtExpression.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                String expression = txtExpression.getText();
                btnClear.setEnabled(expression.length() > 0);

                if (expression.length() == 0) {
                    txtExpression.setBackground(oldBackColor);
                } else {
                    try {
                        ECQL.toExpression(expression);
                        txtExpression.setBackground(oldBackColor);
                    } catch (CQLException e1) {
                        txtExpression.setBackground(warningColor);
                    }
                }
            }
        });

        // init variables
        cboExtent.select(0);
        cboCellSize.select(0);

        // 5. select output folder
        locationView = new OutputDataWidget(FileDataType.RASTER, SWT.SAVE);
        locationView.create(container, SWT.BORDER, 3, 1);
        locationView.setFile(getUniqueName(ToolboxView.getWorkspace(), "calc_"));

        container.pack(true);
        area.pack(true);
        return area;
    }

    private File getUniqueName(final String directory, final String prefix) {
        final String fileExtension = ".tif";

        File[] files = new File(directory).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.startsWith(prefix.toLowerCase()) && name.endsWith(fileExtension);
            }
        });

        int max = 1;
        if (files.length > 0) {
            for (File file : files) {
                try {
                    String name = file.getName().toLowerCase().substring(prefix.length());
                    name = name.substring(0, name.length() - fileExtension.length());
                    int num = Integer.parseInt(name);
                    max = Math.max(max, ++num);
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.FINER, e.getMessage());
                }
            }
        }

        return new File(directory, prefix + String.format("%02d", max) + fileExtension);
    }

    private void calculateMinMaxCellSize() {
        double min, max;

        min = minSizeX = minSizeY = Integer.MAX_VALUE;
        max = maxSizeX = maxSizeY = Integer.MIN_VALUE;

        for (ILayer layer : map.getMapLayers()) {
            if (layer.hasResource(GridCoverageReader.class)
                    || layer.getGeoResource().canResolve(GridCoverageReader.class)) {
                GridCoverage2D coverage = MapUtils.getGridCoverage(layer);
                coverages.put(layer.getName(), coverage);

                GridGeometry2D gridGeometry2D = coverage.getGridGeometry();
                AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

                double cellSizeX = Math.abs(gridToWorld.getScaleX());
                double cellSizeY = Math.abs(gridToWorld.getScaleY());

                if (min > cellSizeX || min > cellSizeY) {
                    minSizeX = cellSizeX;
                    minSizeY = cellSizeY;
                    min = Math.min(cellSizeX, cellSizeY);
                }

                if (max < cellSizeX || max < cellSizeY) {
                    maxSizeX = cellSizeX;
                    maxSizeY = cellSizeY;
                    max = Math.max(cellSizeX, cellSizeY);
                }
            }
        }
    }

    private void updateExpression(String insert) {
        String val = txtExpression.getText();
        if (val.length() == 0) {
            txtExpression.setText(insert);
            txtExpression.setSelection(txtExpression.getText().length());
        } else {
            if (txtExpression.getSelectionCount() == 0) {
                final int pos = txtExpression.getCaretPosition();
                String sql = val.substring(0, pos) + insert + val.substring(pos);
                txtExpression.setText(sql);
                txtExpression.setSelection(pos + insert.length() + 1);
            } else {
                final Point pos = txtExpression.getSelection();
                String sql = val.substring(0, pos.x) + insert + val.substring(pos.y);
                txtExpression.setText(sql);
                txtExpression.setSelection(pos.x + insert.length());
            }
        }
        txtExpression.setFocus();
    }

    private void updateLayers() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (ILayer layer : map.getMapLayers()) {
                    if (layer.hasResource(GridCoverageReader.class)
                            || layer.getGeoResource().canResolve(GridCoverageReader.class)) {
                        GridCoverage2D coverage = MapUtils.getGridCoverage(layer);

                        int numBands = coverage.getNumSampleDimensions();
                        if (numBands > 1) {
                            // multi bands
                            for (int index = 0; index < numBands; index++) {
                                String name = layer.getName() + "_Band_" + index;
                                TableItem item = new TableItem(layerTable, SWT.NULL);
                                item.setText("[" + name + "]");
                                item.setData(coverage);
                            }
                        } else {
                            // single band
                            TableItem item = new TableItem(layerTable, SWT.NULL);
                            item.setText("[" + layer.getName() + "]");
                            item.setData(coverage);
                        }
                    }
                }
            }
        };

        try {
            BusyIndicator.showWhile(Display.getCurrent(), runnable);
        } catch (Exception e) {
            ToolboxPlugin.log(e);
        }
    }

    private void updateFunctions() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Set<FunctionFactory> functionFactories = CommonFactoryFinder
                        .getFunctionFactories(null);
                for (FunctionFactory factory : functionFactories) {
                    String factoryName = factory.toString();
                    if (factoryName
                            .contains("org.geotools.process.function.ProcessFunctionFactory")) {
                        continue;
                    }

                    List<FunctionName> functionNames = factory.getFunctionNames();
                    ArrayList<FunctionName> sorted = new ArrayList<FunctionName>(functionNames);
                    Collections.sort(sorted, new Comparator<FunctionName>() {
                        @Override
                        public int compare(FunctionName o1, FunctionName o2) {
                            if (o1 == null && o2 == null) {
                                return 0;
                            } else if (o1 == null && o2 != null) {
                                return 1;
                            } else if (o1 != null && o2 == null) {
                                return -1;
                            } else {
                                return o1.getName().compareTo(o2.getName());
                            }
                        }
                    });

                    // add table
                    final String regex = "^[A-Z].*";
                    for (FunctionName functionName : sorted) {
                        Parameter<?> returnArg = functionName.getReturn();

                        if (functionName.getName().matches(regex)
                                || !Number.class.isAssignableFrom(returnArg.getType())) {
                            continue;
                        }

                        TableItem item = new TableItem(functionTable, SWT.NULL);
                        int i = 0;
                        StringBuffer buffer = new StringBuffer(functionName.getName() + "( ");
                        for (Parameter<?> argument : functionName.getArguments()) {
                            if (i++ > 0) {
                                buffer.append(", ");
                            }
                            if (Geometry.class.isAssignableFrom(argument.getType())) {
                                buffer.append("[geom]");
                            } else {
                                buffer.append(argument.getName());
                            }
                        }
                        buffer.append(" )");
                        if (i == 0) {
                            item.setText(buffer.toString().replace("  ", ""));
                        } else {
                            item.setText(buffer.toString());
                        }
                    }
                }
            }
        };

        try {
            BusyIndicator.showWhile(Display.getCurrent(), runnable);
        } catch (Exception e) {
            ToolboxPlugin.log(e);
        }
    }

    @Override
    protected void okPressed() {
        if (txtExpression.getText().length() == 0) {
            openInformation(getShell(), Messages.FieldCalculatorDialog_Warning);
            return;
        }

        try {
            PlatformUI.getWorkbench().getProgressService().run(false, true, this);
            openInformation(getShell(), Messages.General_Completed);
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), Messages.General_Error, e.getMessage());
        } catch (InterruptedException e) {
            MessageDialog.openInformation(getShell(), Messages.General_Cancelled, e.getMessage());
        }
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        monitor.beginTask(String.format(Messages.Task_Executing, windowTitle), 100);
        monitor.worked(increment);

        try {
            ToolboxPlugin.log(String.format(Messages.Task_Executing, windowTitle));

            Expression expression = ECQL.toExpression(txtExpression.getText());
            Map<String, GridCoverage2D> layers = getLayersFromExpression(expression.toString());

            // check extent & cell size = cellSize
            monitor.worked(increment);
            ReferencedEnvelope extent = resolveExtent(layers);

            // convert to features
            CoveragesToPointFeatureCollection features = null;
            features = new CoveragesToPointFeatureCollection(layers, extent, cellSizeX, cellSizeY);

            // calculate
            monitor.worked(increment);
            GridCoverage2D coverage = calculateExpression(features, expression, extent, cellSizeX,
                    cellSizeY, monitor);

            if (coverage == null) {
                return;
            }

            Object minValue = coverage.getProperty("Minimum"); //$NON-NLS-1$
            Object maxValue = coverage.getProperty("Maximum"); //$NON-NLS-1$
            int numBands = coverage.getNumSampleDimensions();

            monitor.worked(increment);
            RasterExportOperation saveAs = new RasterExportOperation();
            coverage = saveAs.saveAsGeoTiff(coverage, locationView.getFile());

            Style style = null;
            monitor.worked(increment);
            if (minValue != null && minValue instanceof Number && maxValue != null
                    && maxValue instanceof Number && numBands == 1) {
                Double noData = RasterHelper.getNoDataValue(coverage);
                style = buildCoverageStyle((Double) minValue, (Double) maxValue, noData);
            } else {
                SSStyleBuilder builder = new SSStyleBuilder(null);
                style = builder.getDefaultGridCoverageStyle(coverage);
            }

            monitor.worked(increment);
            MapUtils.addGridCoverageToMap(map, coverage, new File(locationView.getFile()), style);
        } catch (Exception e) {
            // always show log
            boolean showLog = ToolboxView.getShowLog();
            ToolboxView.setShowLog(true);
            ToolboxPlugin.log(e.getMessage());
            ToolboxView.setShowLog(showLog);
        } finally {
            ToolboxPlugin.log(String.format(Messages.Task_Completed, windowTitle));
            monitor.done();
        }
    }

    private GridCoverage2D calculateExpression(SimpleFeatureCollection features,
            Expression expression, ReferencedEnvelope extent, double cellSizeX, double cellSizeY,
            IProgressMonitor monitor) {
        extent = RasterHelper.getResolvedEnvelope(extent, cellSizeX, cellSizeY);
        SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE,
                128, 128, 1);
        ColorModel cm = PlanarImage.createColorModel(sampleModel);

        Dimension dm = RasterHelper.getDimension(extent, cellSizeX, cellSizeY);
        double noDataValue = -Double.MAX_VALUE;
        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;

        DiskMemImage outputImage = null;
        outputImage = new DiskMemImage(0, 0, dm.width, dm.height, 0, 0, sampleModel, cm);
        outputImage.setUseCommonCache(true);

        WritableRectIter writerIter = RectIterFactory.createWritable(outputImage,
                outputImage.getBounds());

        SubMonitor progress = SubMonitor.convert(monitor, dm.height).newChild(dm.height);
        progress.beginTask(String.format(Messages.Task_Executing, windowTitle), dm.height);
        progress.setWorkRemaining(dm.height);

        SimpleFeatureIterator featureIter = features.features();
        writerIter.startLines();
        while (!writerIter.finishedLines()) {
            progress.worked(1);
            if (progress.isCanceled()) {
                ToolboxPlugin.log(String.format(Messages.Task_Canceled, windowTitle));
                featureIter.close();
                return null;
            }

            writerIter.startPixels();
            while (!writerIter.finishedPixels()) {
                if (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    Double value = expression.evaluate(feature, Double.class);
                    if (value == null || value.isInfinite() || value.isNaN()) {
                        writerIter.setSample(0, noDataValue);
                    } else {
                        double val = value.doubleValue();
                        writerIter.setSample(0, val);
                        minValue = Math.min(minValue, val);
                        maxValue = Math.max(maxValue, val);
                    }
                } else {
                    writerIter.setSample(0, noDataValue);
                }
                writerIter.nextPixel();
            }
            writerIter.nextLine();
        }
        featureIter.close();

        return RasterHelper.createGridCoverage("result", outputImage, 1, noDataValue, minValue,
                maxValue, extent);
    }

    private ReferencedEnvelope resolveExtent(Map<String, GridCoverage2D> selectedLayers) {
        ReferencedEnvelope extent = null;

        switch (cboExtent.getSelectionIndex()) {
        case 0: // intersection
            for (Entry<String, GridCoverage2D> entry : selectedLayers.entrySet()) {
                ReferencedEnvelope env = new ReferencedEnvelope(entry.getValue().getEnvelope());
                if (extent == null) {
                    extent = env;
                } else {
                    extent = extent.intersection(env);
                }
            }
            break;
        case 1: // union
            for (Entry<String, GridCoverage2D> entry : selectedLayers.entrySet()) {
                ReferencedEnvelope env = new ReferencedEnvelope(entry.getValue().getEnvelope());
                if (extent == null) {
                    extent = env;
                } else {
                    extent.expandToInclude(env);
                }
            }
            break;
        default: // scan all layers
            String layerName = cboExtent.getText().replace("[", "").replace("]", "");
            extent = new ReferencedEnvelope(coverages.get(layerName).getEnvelope());
            break;
        }

        return extent;
    }

    private Map<String, GridCoverage2D> getLayersFromExpression(String expression) {
        Map<String, GridCoverage2D> map = new TreeMap<String, GridCoverage2D>();

        for (Entry<String, GridCoverage2D> entry : coverages.entrySet()) {
            if (expression.indexOf(entry.getKey()) != -1) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        return map;
    }

    static class CoveragesToPointFeatureCollection extends GXTSimpleFeatureCollection {
        protected static final Logger LOGGER = Logging
                .getLogger(CoveragesToPointFeatureCollection.class);

        static final String TYPE_NAME = "Coverage";

        private SimpleFeatureType schema;

        private Map<String, GridCoverage2D> coverages;

        private ReferencedEnvelope extent;

        private double cellSizeX;

        private double cellSizeY;

        public CoveragesToPointFeatureCollection(Map<String, GridCoverage2D> coverages,
                ReferencedEnvelope extent, double cellSizeX, double cellSizeY) {
            super(null);

            this.coverages = coverages;
            this.extent = extent;
            this.cellSizeX = cellSizeX;
            this.cellSizeY = cellSizeY;
            this.createTemplateFeatureType();
        }

        private void createTemplateFeatureType() {
            CoordinateReferenceSystem targetCRS = extent.getCoordinateReferenceSystem();
            schema = FeatureTypes.getDefaultType(TYPE_NAME,
                    com.vividsolutions.jts.geom.Point.class, targetCRS);

            for (Entry<String, GridCoverage2D> entry : coverages.entrySet()) {
                GridCoverage2D coverage = entry.getValue();
                String fieldName = entry.getKey();

                schema = FeatureTypes.add(schema, fieldName, Double.class); // default, landsat8
                for (int bndIdx = 0; bndIdx < coverage.getNumSampleDimensions(); bndIdx++) {
                    schema = FeatureTypes.add(schema, fieldName + "_Band_" + bndIdx, Double.class);
                }
            }
        }

        @Override
        public SimpleFeatureIterator features() {
            return new CoveragesToPointFeatureIterator(getSchema());
        }

        @Override
        public SimpleFeatureType getSchema() {
            return schema;
        }

        @Override
        public SimpleFeatureCollection subCollection(Filter filter) {
            if (filter == Filter.INCLUDE) {
                return this;
            }
            return new SubFeatureCollection(this, filter);
        }

        @Override
        public int size() {
            int columns = (int) Math.floor((extent.getWidth() / cellSizeX) + 0.5);
            int rows = (int) Math.floor((extent.getHeight() / cellSizeY) + 0.5);
            return columns * rows;
        }

        @Override
        public ReferencedEnvelope getBounds() {
            return extent;
        }

        final class CoveragesToPointFeatureIterator implements SimpleFeatureIterator {
            private GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools
                    .getDefaultHints());

            private RandomIter[] readIter;

            private GridTransformer[] trans;

            private double[] noData;

            private int[] bands;

            private java.awt.Rectangle[] bounds;

            private int currentRow = 0;

            private int rowCount = Integer.MAX_VALUE;

            private SimpleFeatureBuilder builder;

            private SimpleFeature next;

            private int featureID = 0;

            private Map<Integer, List<Coordinate>> rowCells = new TreeMap<Integer, List<Coordinate>>();

            public CoveragesToPointFeatureIterator(SimpleFeatureType schema) {
                this.builder = new SimpleFeatureBuilder(schema);

                readIter = new RandomIter[coverages.size()];

                trans = new GridTransformer[coverages.size()];

                noData = new double[coverages.size()];
                bands = new int[coverages.size()];
                bounds = new java.awt.Rectangle[coverages.size()];

                int covIdx = 0;
                CoordinateReferenceSystem targetCRS = schema.getCoordinateReferenceSystem();
                for (Entry<String, GridCoverage2D> entry : coverages.entrySet()) {
                    GridCoverage2D coverage = entry.getValue();

                    // 1. reproject
                    CoordinateReferenceSystem sourceCRS = coverage.getCoordinateReferenceSystem();
                    if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
                        // Reproject
                        RasterReprojectOperation project = new RasterReprojectOperation();
                        coverage = project.execute(coverage, targetCRS, ResampleType.NEAREST,
                                cellSizeX, cellSizeY);
                    }

                    // 2. resample
                    GridGeometry2D gridGeometry2D = coverage.getGridGeometry();
                    AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();

                    double cellX = Math.abs(gridToWorld.getScaleX());
                    double cellY = Math.abs(gridToWorld.getScaleY());

                    if (!SSUtils.compareDouble(cellX, cellSizeX)
                            || !SSUtils.compareDouble(cellY, cellSizeY)) {
                        RasterResampleOperation resample = new RasterResampleOperation();
                        coverage = resample.execute(coverage, cellSizeX, cellSizeY,
                                ResampleType.NEAREST);
                    }

                    // 3. clip
                    ReferencedEnvelope env = new ReferencedEnvelope(coverage.getEnvelope());
                    if (!extent.equals(env)) {
                        RasterClipOperation clip = new RasterClipOperation();
                        coverage = clip.execute(coverage, extent);
                    }

                    trans[covIdx] = new GridTransformer(coverage);

                    noData[covIdx] = RasterHelper.getNoDataValue(coverage);
                    bands[covIdx] = coverage.getNumSampleDimensions();

                    PlanarImage inputImage = (PlanarImage) coverage.getRenderedImage();
                    java.awt.Rectangle bound = inputImage.getBounds();

                    readIter[covIdx] = RandomIterFactory.create(inputImage, bound);
                    bounds[covIdx] = bound;
                    rowCount = Math.min(rowCount, bound.height);
                    covIdx++;
                }

                currentRow = 0;
                rowCells.clear();
            }

            @Override
            public void close() {
                // nothing to do
            }

            private void extractValues() {
                for (int covIdx = 0; covIdx < readIter.length; covIdx++) {
                    List<Coordinate> coords = new ArrayList<Coordinate>();
                    rowCells.put(Integer.valueOf(covIdx), coords);

                    int minX = bounds[covIdx].x; // Minimum inclusive
                    int maxX = bounds[covIdx].width + minX; // Maximum exclusive
                    int row = bounds[covIdx].y + currentRow;

                    for (int column = minX; column < maxX; column++) {
                        double[] vals = readIter[covIdx].getPixel(column, row,
                                new double[bands[covIdx]]);

                        Coordinate coord = trans[covIdx].gridToWorldCoordinate(column, row);
                        coord.z = getRasterValue(vals[0], noData[covIdx]);
                        coords.add(coord);

                        for (int bndIdx = 0; bndIdx < bands[covIdx]; bndIdx++) {
                            coord = (Coordinate) coord.clone();
                            coord.z = getRasterValue(vals[bndIdx], noData[covIdx]);
                            coords.add(coord);
                        }
                    }
                }
                currentRow++;
            }

            public boolean hasNext() {
                while (next == null && (rowCount > currentRow || rowCells.size() > 0)) {
                    if (rowCells.size() == 0) {
                        extractValues();
                    }

                    if (rowCells.size() > 0) {
                        int feldIndex = 1; // 0 = geom
                        for (int covIdx = 0; covIdx < rowCells.size(); covIdx++) {
                            List<Coordinate> coordinates = rowCells.get(covIdx);
                            if (coordinates.size() > 0) {
                                if (next == null) {
                                    next = builder.buildFeature(buildID(TYPE_NAME, ++featureID));
                                    next.setDefaultGeometry(gf.createPoint(coordinates.get(0)));
                                }

                                for (int bndIdx = 0; bndIdx <= bands[covIdx]; bndIdx++) {
                                    Coordinate coord = coordinates.get(0);
                                    next.setAttribute(feldIndex++, getFeatureValue(coord.z));
                                    coordinates.remove(0);
                                }
                            } else {
                                rowCells.clear();
                                break;
                            }
                        }
                    }
                }

                return next != null;
            }

            public SimpleFeature next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException("hasNext() returned false!");
                }

                SimpleFeature result = next;
                next = null;
                return result;
            }

            private double getRasterValue(double value, double noData) {
                return SSUtils.compareDouble(noData, value) ? Double.NaN : value;
            }

            private Double getFeatureValue(double value) {
                return Double.isNaN(value) || Double.isInfinite(value) ? null : value;
            }
        }
    }

}
