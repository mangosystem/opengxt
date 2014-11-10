/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.internal.ui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.Parameter;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.process.ProcessExecutor;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.process.Progress;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.catalog.util.GeoToolsAdapters;
import org.locationtech.udig.processingtoolbox.MapUtils;
import org.locationtech.udig.processingtoolbox.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.MapUtils.VectorLayerType;
import org.locationtech.udig.processingtoolbox.ProcessUtils;
import org.locationtech.udig.processingtoolbox.ProcessUtils.OutputType;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.common.DataStoreFactory;
import org.locationtech.udig.processingtoolbox.common.FeatureTypes;
import org.locationtech.udig.processingtoolbox.common.ForceCRSFeatureCollection;
import org.locationtech.udig.processingtoolbox.common.FormatUtils;
import org.locationtech.udig.processingtoolbox.common.RasterSaveAsOp;
import org.locationtech.udig.processingtoolbox.common.ShapeExportOp;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataViewer.FielDataType;
import org.locationtech.udig.project.IMap;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Process Execution Dialog
 * 
 * @author MapPlus
 */
public class ProcessExecutionDialog extends TitleAreaDialog implements IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(ProcessExecutionDialog.class);

    private IMap map;

    private org.geotools.process.ProcessFactory factory;

    private org.opengis.feature.type.Name processName;

    private String windowTitle;

    private Map<String, Object> processParams = new HashMap<String, Object>();

    private Map<Widget, String> uiParams = new HashMap<Widget, String>();

    private boolean outputLocationRequired = false;

    private int height = 100;

    private OutputDataViewer locationView;

    private Text txtOutput;

    private CTabItem inputTab, outputTab;

    private File outputFile = null;

    private final Color warningColor = new Color(Display.getCurrent(), 255, 255, 200);

    public ProcessExecutionDialog(Shell parentShell, IMap map, ProcessFactory factory,
            Name processName) {
        super(parentShell);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.map = map;
        this.factory = factory;
        this.processName = processName;

        windowTitle = factory.getTitle(processName).toString();
    }

    @Override
    public void create() {
        super.create();

        setTitle(windowTitle);
        setMessage(factory.getDescription(processName).toString());
    }

    @Override
    protected Point getInitialSize() {
        return new Point(650, height);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);

        newShell.setText(windowTitle);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        outputLocationRequired = ProcessUtils.outputLocationRequired(factory, processName);

        // 0. Tab Folder
        final CTabFolder parentTabFolder = new CTabFolder(area, SWT.BOTTOM);
        parentTabFolder.setUnselectedCloseVisible(false);
        parentTabFolder.setLayout(new FillLayout());
        parentTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // 1. Process execution
        inputTab = makeInputTab(parentTabFolder);

        // 2. Simple output
        if (!outputLocationRequired) {
            outputTab = makeOutputTab(parentTabFolder);
        }

        // 3. Help
        makeHelpTab(parentTabFolder);

        parentTabFolder.setSelection(inputTab);

        // set maximum height
        area.pack();
        parentTabFolder.pack();

        height = parentTabFolder.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 162;
        height = height > 650 ? 650 : height;

        return area;
    }

    private CTabItem makeOutputTab(final CTabFolder parentTabFolder) {
        CTabItem tab = new CTabItem(parentTabFolder, SWT.NONE);
        tab.setText(Messages.ProcessExecutionDialog_taboutput);

        try {
            txtOutput = new Text(parentTabFolder, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
            txtOutput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
            tab.setControl(txtOutput);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        return tab;
    }

    private CTabItem makeHelpTab(final CTabFolder parentTabFolder) {
        CTabItem tab = new CTabItem(parentTabFolder, SWT.NONE);
        tab.setText(Messages.ProcessExecutionDialog_tabhelp);

        try {
            Browser browser = new Browser(parentTabFolder, SWT.NONE);
            GridData layoutData = new GridData(GridData.FILL_BOTH);
            browser.setLayoutData(layoutData);
            File file = ProcessDescriptor.generate(factory, processName);
            browser.setUrl(DataUtilities.fileToURL(file).toExternalForm());
            tab.setControl(browser);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        return tab;
    }

    private CTabItem makeInputTab(final CTabFolder parentTabFolder) {
        CTabItem tab = new CTabItem(parentTabFolder, SWT.NONE);
        tab.setText(Messages.ProcessExecutionDialog_tabparameters);

        ScrolledComposite scroller = new ScrolledComposite(parentTabFolder, SWT.NONE | SWT.V_SCROLL
                | SWT.H_SCROLL);
        scroller.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite container = new Composite(scroller, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        // input parameters
        Map<String, Parameter<?>> paramInfo = factory.getParameterInfo(processName);

        // build ui
        for (Entry<String, Parameter<?>> entrySet : paramInfo.entrySet()) {
            processParams.put(entrySet.getValue().key, entrySet.getValue().sample);
            insertControl(container, entrySet.getValue());
        }

        // output location
        OutputType outputType = ProcessUtils.getOutputType(factory, processName);
        String layerName = ProcessUtils.getLayerName(processName);

        switch (outputType) {
        case FEATURES:
        case GEOMETRY:
        case ENVELOPE:
            locationView = new OutputDataViewer(FielDataType.SHAPEFILE, SWT.SAVE);
            locationView.create(container, SWT.BORDER, 1);
            locationView.setFile(new File(ToolboxView.getWorkspace(), layerName + ".shp")); //$NON-NLS-1$
            break;
        case RASTER:
            locationView = new OutputDataViewer(FielDataType.RASTER, SWT.SAVE);
            locationView.create(container, SWT.BORDER, 1);
            locationView.setFile(new File(ToolboxView.getWorkspace(), layerName + ".tif")); //$NON-NLS-1$
            break;
        default:
            break;
        }

        scroller.setContent(container);
        tab.setControl(scroller);

        scroller.pack();
        container.pack();

        scroller.setMinSize(450, container.getSize().y - 2);
        height = container.getSize().y + 162;
        scroller.setExpandVertical(true);
        scroller.setExpandHorizontal(true);

        return tab;
    }

    private void insertParamTitle(Composite container, final Parameter<?> param) {
        String postfix = ""; //$NON-NLS-1$
        if (param.type.isAssignableFrom(Geometry.class)) {
            postfix = "(WKT)"; //$NON-NLS-1$
        }

        String title = param.title.toString();
        // capitalize title
        title = Character.toUpperCase(title.charAt(0)) + title.substring(1);

        if (param.required) {
            Image image = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage(); //$NON-NLS-1$
            CLabel lblName = new CLabel(container, SWT.NONE);
            lblName.setImage(image);
            lblName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

            FontData[] fontData = lblName.getFont().getFontData();
            for (int i = 0; i < fontData.length; ++i) {
                fontData[i].setStyle(SWT.NORMAL);
            }
            lblName.setFont(new Font(container.getDisplay(), fontData));
            lblName.setText(title + postfix);
            lblName.setToolTipText(param.description.toString());
        } else {
            Text txtName = new Text(container, SWT.NONE);
            txtName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
            txtName.setBackground(container.getBackground());
            txtName.setText(title + Messages.ProcessExecutionDialog_optional + postfix);
            txtName.setToolTipText(param.description.toString());
        }
    }

    private void insertControl(Composite container, final Parameter<?> param) {
        // process name
        insertParamTitle(container, param);

        // process value
        GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        if (param.type.isAssignableFrom(SimpleFeatureCollection.class)) {
            // vector layer parameters
            final Combo cboSfLayer = new Combo(container, SWT.NONE | SWT.DROP_DOWN | SWT.READ_ONLY);
            cboSfLayer.setLayoutData(layoutData);

            Map<String, Object> metadata = param.metadata;
            if (metadata == null || metadata.size() == 0) {
                MapUtils.fillLayers(map, cboSfLayer, VectorLayerType.ALL);
            } else {
                if (metadata.containsKey(Parameter.FEATURE_TYPE)) {
                    String val = metadata.get(Parameter.FEATURE_TYPE).toString();
                    if (val.equalsIgnoreCase(VectorLayerType.ALL.toString())) {
                        MapUtils.fillLayers(map, cboSfLayer, VectorLayerType.ALL);
                    } else if (val.equalsIgnoreCase(VectorLayerType.POINT.toString())) {
                        MapUtils.fillLayers(map, cboSfLayer, VectorLayerType.POINT);
                    } else if (val.equalsIgnoreCase(VectorLayerType.LINESTRING.toString())) {
                        MapUtils.fillLayers(map, cboSfLayer, VectorLayerType.LINESTRING);
                    } else if (val.equalsIgnoreCase(VectorLayerType.POLYGON.toString())) {
                        MapUtils.fillLayers(map, cboSfLayer, VectorLayerType.POLYGON);
                    }
                } else {
                    MapUtils.fillLayers(map, cboSfLayer, VectorLayerType.ALL);
                }
            }
            cboSfLayer.setData(param.key);

            cboSfLayer.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    SimpleFeatureCollection sfc = MapUtils.getFeatures(map, cboSfLayer.getText());
                    if (sfc.getSchema().getCoordinateReferenceSystem() == null) {
                        sfc = new ForceCRSFeatureCollection(sfc, map.getViewportModel().getCRS());
                    }
                    // TODO: Important!!!!!!!!!!!!!!!
                    // if this layer's crs is different from map's crs

                    processParams.put(param.key, sfc);

                    // related field selection "파라미터명.필드유형"
                    SimpleFeatureType schema = sfc.getSchema();
                    for (Entry<Widget, String> entrySet : uiParams.entrySet()) {
                        String paramValue = entrySet.getValue().split("\\.")[0]; //$NON-NLS-1$
                        if (paramValue.equals(param.key)) {
                            // FieldType = ALL, String, Number, Integer, Double
                            String fieldType = entrySet.getValue().split("\\.")[1]; //$NON-NLS-1$
                            Combo cboField = (Combo) entrySet.getKey();
                            if (fieldType.equalsIgnoreCase(FieldType.ALL.toString())) {
                                MapUtils.fillFields(cboField, schema, FieldType.ALL);
                            } else if (fieldType.equalsIgnoreCase(FieldType.String.toString())) {
                                MapUtils.fillFields(cboField, schema, FieldType.String);
                            } else if (fieldType.equalsIgnoreCase(FieldType.Number.toString())) {
                                MapUtils.fillFields(cboField, schema, FieldType.Number);
                            } else if (fieldType.equalsIgnoreCase(FieldType.Integer.toString())) {
                                MapUtils.fillFields(cboField, schema, FieldType.Integer);
                            } else if (fieldType.equalsIgnoreCase(FieldType.Double.toString())) {
                                MapUtils.fillFields(cboField, schema, FieldType.Double);
                            }
                        }
                    }
                }
            });
        } else if (param.type.isAssignableFrom(GridCoverage2D.class)) {
            // raster layer parameters
            final Combo cboGcLayer = new Combo(container, SWT.NONE | SWT.DROP_DOWN | SWT.READ_ONLY);
            cboGcLayer.setLayoutData(layoutData);
            MapUtils.fillLayers(map, cboGcLayer, GridCoverageReader.class);
            cboGcLayer.setData(param.key);

            cboGcLayer.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    GridCoverage2D sfc = MapUtils.getGridCoverage(map, cboGcLayer.getText());
                    processParams.put(param.key, sfc);
                }
            });
        } else if (param.type.isAssignableFrom(Geometry.class)) {
            // wkt geometry parameters
            GeometryViewer geometryView = new GeometryViewer(map);
            geometryView.create(container, SWT.NONE, processParams, param);
        } else if (Filter.class.isAssignableFrom(param.type)) {
            // filter parameters
            FilterViewer filterView = new FilterViewer(map);
            filterView.create(container, SWT.NONE, processParams, param);
        } else if (param.type.isAssignableFrom(CoordinateReferenceSystem.class)) {
            // coordinate reference system parameters
            CrsViewer crsView = new CrsViewer(map);
            crsView.create(container, SWT.NONE, processParams, param);
        } else if (BoundingBox.class.isAssignableFrom(param.type)) {
            // bounding box parameters
            BoundingBoxViewer boundingBoxView = new BoundingBoxViewer(map);
            boundingBoxView.create(container, SWT.NONE, processParams, param);
        } else if (param.type.isEnum()) {
            // enumeration parameters
            final Combo cboEnum = new Combo(container, SWT.NONE | SWT.DROP_DOWN | SWT.READ_ONLY);
            cboEnum.setLayoutData(layoutData);
            for (Object enumVal : param.type.getEnumConstants()) {
                cboEnum.add(enumVal.toString());
            }

            cboEnum.setData(param.key);
            if (param.sample != null) {
                cboEnum.setText(param.sample.toString());
            }

            cboEnum.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    for (Object enumVal : param.type.getEnumConstants()) {
                        if (enumVal.toString().equalsIgnoreCase(cboEnum.getText())) {
                            processParams.put(param.key, enumVal);
                            break;
                        }
                    }
                }
            });
        } else if (Number.class.isAssignableFrom(param.type)) {
            // number parameters = Byte, Double, Float, Integer, Long, Short
            if (Double.class.isAssignableFrom(param.type)
                    || Float.class.isAssignableFrom(param.type)) {
                NumberDataViewer numberView = new NumberDataViewer(map);
                numberView.create(container, SWT.NONE, processParams, param);
            } else {
                final Spinner spinner = new Spinner(container, SWT.LEFT_TO_RIGHT | SWT.BORDER);
                spinner.setLayoutData(layoutData);
                spinner.setData(param.key);
                spinner.setValues(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, 10);
                if (param.sample != null) {
                    spinner.setSelection((Integer) param.sample);
                }

                final Color oldBackColor = spinner.getBackground();
                spinner.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(ModifyEvent e) {
                        Object obj = Converters.convert(spinner.getSelection(), param.type);
                        if (obj == null) {
                            spinner.setBackground(warningColor);
                        } else {
                            processParams.put(param.key, obj);
                            spinner.setBackground(oldBackColor);
                        }
                    }
                });
            }
        } else if (Boolean.class.isAssignableFrom(param.type)
                || boolean.class.isAssignableFrom(param.type)) {
            // boolean parameters
            final Combo cboBoolean = new Combo(container, SWT.NONE | SWT.DROP_DOWN | SWT.READ_ONLY);
            cboBoolean.setLayoutData(layoutData);
            cboBoolean.setData(param.key);
            
            cboBoolean.add(Messages.ProcessExecutionDialog_Yes);
            cboBoolean.add(Messages.ProcessExecutionDialog_No);

            if (param.sample != null) {
                cboBoolean.select((Boolean) param.sample == Boolean.TRUE ? 0 : 1);
            }
            cboBoolean.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    Boolean value = cboBoolean.getSelectionIndex() == 0 ? Boolean.TRUE : Boolean.FALSE;
                    processParams.put(param.key, value);
                }
            });
        } else {
            Map<String, Object> metadata = param.metadata;
            if (metadata == null || metadata.size() == 0) {
                // other literal parameters 
                LiteralDataViewer literalView = new LiteralDataViewer(map);
                literalView.create(container, SWT.NONE, processParams, param);
            } else {
                if (metadata.containsKey(Parameter.OPTIONS)) {
                    // layer's field ...
                    // TODO: new KVP(Parameter.OPTIONS, "파라미터명.필드유형")
                    final Combo cboField = new Combo(container, SWT.NONE | SWT.DROP_DOWN);

                    uiParams.put(cboField, metadata.get(Parameter.OPTIONS).toString());

                    cboField.setLayoutData(layoutData);
                    cboField.setData(param.key);
                    if (param.sample != null) {
                        cboField.add(param.sample.toString());
                        cboField.setText(param.sample.toString());
                    }

                    cboField.addModifyListener(new ModifyListener() {
                        @Override
                        public void modifyText(ModifyEvent e) {
                            processParams.put(param.key, cboField.getText());
                        }
                    });

                } else {
                    // other literal parameters
                    LiteralDataViewer filterView = new LiteralDataViewer(map);
                    filterView.create(container, SWT.NONE, processParams, param);
                }
            }
        }
    }

    @SuppressWarnings("nls")
    private boolean invalidOutput(File outputFile) {
        if (!outputFile.exists()) {
            return false;
        }

        String msg = Messages.ProcessExecutionDialog_overwriteconfirm;
        if (MessageDialog.openConfirm(getParentShell(), windowTitle, msg)) {
            OutputType outputType = ProcessUtils.getOutputType(factory, processName);
            if (outputType == OutputType.RASTER) {
                if (!outputFile.delete()) {
                    msg = Messages.ProcessExecutionDialog_deletefailed;
                    MessageDialog.openInformation(getParentShell(), windowTitle, msg);
                    return true;
                }
            } else {
                String[] extensions = new String[] { "shp", "shx", "dbf", "prj", "sbn", "sbx", 
                        "qix", "fix", "lyr" };
                String fileName = outputFile.getName();
                int pos = outputFile.getName().lastIndexOf(".");
                if (pos > 0) {
                    fileName = outputFile.getName().substring(0, pos);
                }

                for (String ext : extensions) {
                    File file = new File(outputFile.getParent(), fileName + "." + ext);
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }
        } else {
            return true;
        }

        return false;
    }

    @Override
    protected void okPressed() {
        // check required parameters
        Map<String, Parameter<?>> paramInfo = factory.getParameterInfo(processName);
        for (Entry<String, Parameter<?>> entrySet : paramInfo.entrySet()) {
            String key = entrySet.getValue().key;
            if (entrySet.getValue().required && processParams.get(key) == null) {
                String msg = Messages.ProcessExecutionDialog_requiredparam
                        + System.getProperty("line.separator") + " - " //$NON-NLS-1$ //$NON-NLS-2$
                        + entrySet.getValue().getTitle();
                MessageDialog.openInformation(getParentShell(), windowTitle, msg);
                return;
            }
        }

        if (outputLocationRequired) {
            outputFile = new File(locationView.getFile());

            if (invalidOutput(outputFile)) {
                return;
            }

            try {
                new ProgressMonitorDialog(getParentShell()).run(true, true, this);
            } catch (InvocationTargetException e) {
                ToolboxPlugin.log("Error occured " + e.getMessage()); //$NON-NLS-1$
                MessageDialog.openError(getParentShell(), "Error", e.getMessage()); //$NON-NLS-1$
            } catch (InterruptedException e) {
                ToolboxPlugin.log("Cancelled " + e.getMessage()); //$NON-NLS-1$
                MessageDialog.openInformation(getParentShell(), "Cancelled", e.getMessage()); //$NON-NLS-1$
            } finally {
                if (outputLocationRequired) {
                    super.okPressed();
                }
            }
        } else {
            ToolboxPlugin.log("Executing " + windowTitle + "..."); //$NON-NLS-1$ //$NON-NLS-2$
            org.geotools.process.Process process = factory.create(processName);
            // Map<String, Object> result = process.execute(processParams, null);

            /*************************************************************/
            // This is a great way to use todays multi-core processors in your application.
            // A good idea is the number of cores you have plus 1.:
            int cores = Runtime.getRuntime().availableProcessors();
            ProcessExecutor executor = Processors.newProcessExecutor(cores, null);
            Progress workTicket = executor.submit(process, processParams);
            while (!workTicket.isDone()) {
                try {
                    Thread.sleep(250);
                    //System.out.println("Progress:" + workTicket.getProgress() + "percent complete");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Map<String, Object> result = null;
            try {
                result = workTicket.get(); // get is BLOCKING
                ToolboxPlugin.log("Completed " + windowTitle + "..."); //$NON-NLS-1$ //$NON-NLS-2$
                if (result != null) {
                    for (Entry<String, Object> entrySet : result.entrySet()) {
                        final Object val = entrySet.getValue();
                        if (val == null) {
                            continue;
                        }

                        if (Number.class.isAssignableFrom(val.getClass())) {
                            displayWindow(FormatUtils.format(Double.parseDouble(val.toString())));
                        } else {
                            displayWindow(val);
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            /**************************************************************/

        }
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        int increment = 10;
        monitor.beginTask("Running operation...", 100); //$NON-NLS-1$
        monitor.worked(increment);

        try {
            monitor.setTaskName("Running operation..."); //$NON-NLS-1$
            ToolboxPlugin.log("Executing " + windowTitle + "..."); //$NON-NLS-1$ //$NON-NLS-2$

            org.geotools.process.Process process = factory.create(processName);
            ProgressListener progessListener = GeoToolsAdapters.progress(SubMonitor.convert(monitor, "processing ", 60));  //$NON-NLS-1$
            final Map<String, Object> result = process.execute(processParams, progessListener);
            monitor.worked(increment);

            monitor.setTaskName("Adding layer..."); //$NON-NLS-1$
            if (result != null) {
                for (Entry<String, Object> entrySet : result.entrySet()) {
                    final Object val = entrySet.getValue();
                    if (val == null) {
                        continue;
                    }

                    ToolboxPlugin.log("Writing result..."); //$NON-NLS-1$
                    if (val instanceof SimpleFeatureCollection) {
                        SimpleFeatureCollection sfc = (SimpleFeatureCollection) val;
                        if (sfc.getSchema().getGeometryDescriptor() == null) {
                            displayWindow(ProcessUtils.getFeatureInformation(sfc));
                        } else {
                            if (exportToShapefile(sfc, outputFile)) {
                                ToolboxPlugin.log("Adding layer..."); //$NON-NLS-1$
                                MapUtils.addFeaturesToMap(map, outputFile);
                            }
                        }
                    } else if (val instanceof GridCoverage2D) {
                        RasterSaveAsOp saveAs = new RasterSaveAsOp();
                        GridCoverage2D output = saveAs.saveAsGeoTiff((GridCoverage2D) val,
                                outputFile.getAbsolutePath());
                        ToolboxPlugin.log("Adding layer..."); //$NON-NLS-1$
                        MapUtils.addGridCoverageToMap(map, output, outputFile, null);
                    } else if (val instanceof Geometry) {
                        if (exportToShapefile(
                                geometryToFeatures((Geometry) val, processName.toString()),
                                outputFile)) {
                            ToolboxPlugin.log("Adding layer..."); //$NON-NLS-1$
                            MapUtils.addFeaturesToMap(map, outputFile);
                        }
                    } else if (val instanceof BoundingBox) {
                        if (exportToShapefile(
                                geometryToFeatures(JTS.toGeometry((BoundingBox) val),
                                        processName.toString()), outputFile)) {
                            ToolboxPlugin.log("Adding layer..."); //$NON-NLS-1$
                            MapUtils.addFeaturesToMap(map, outputFile);
                        }
                    }
                    monitor.worked(increment);
                }
            }
            monitor.worked(increment);
        } catch (Exception e) {
            ToolboxPlugin.log(e.getMessage());
        } finally {
            ToolboxPlugin.log("Completed " + windowTitle + "..."); //$NON-NLS-1$ //$NON-NLS-2$
            monitor.done();
        }
    }

    private SimpleFeatureCollection geometryToFeatures(Geometry source, String layerName) {
        CoordinateReferenceSystem crs = map.getViewportModel().getCRS();
        SimpleFeatureType schema = FeatureTypes.getDefaultType(layerName, source.getClass(), crs);

        ListFeatureCollection features = new ListFeatureCollection(schema);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);

        SimpleFeature feature = builder.buildFeature(null);
        feature.setDefaultGeometry(source);
        features.add(feature);

        return features;
    }

    private boolean exportToShapefile(SimpleFeatureCollection features, File filePath) {
        String typeName = FilenameUtils.removeExtension(FilenameUtils.getName(filePath.getPath()));
        DataStore dataStore = DataStoreFactory.getShapefileDataStore(filePath.getParent(), false);
        
        ShapeExportOp exportOp = ShapeExportOp.getDefault();
        exportOp.setOutputDataStore(dataStore);
        exportOp.setOutputTypeName(typeName);
        try {
            SimpleFeatureSource output = exportOp.execute(features);
            return output != null;
        } catch (IOException e) {
            ToolboxPlugin.log(e.getMessage());
        }
        return false;
    }

    private void displayWindow(final Object source) {
        outputTab.getParent().setSelection(outputTab);

        String pattern = "[,()]"; //$NON-NLS-1$
        String message = source.toString()
                .replaceAll(pattern, System.getProperty("line.separator")); //$NON-NLS-1$
        txtOutput.setText(message);
    }
}
