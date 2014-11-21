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
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
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
import org.geotools.data.DataUtilities;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessFactory;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.common.ForceCRSFeatureCollection;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.ProcessExecutorOperation;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.IMap;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Process Execution Dialog
 * 
 * @author MapPlus
 */
public class ProcessExecutionDialog extends TitleAreaDialog {
    protected static final Logger LOGGER = Logging.getLogger(ProcessExecutionDialog.class);

    private IMap map;

    private org.geotools.process.ProcessFactory factory;

    private org.opengis.feature.type.Name processName;

    private String windowTitle;

    private Map<String, Object> inputParams = new HashMap<String, Object>();

    private Map<String, Object> outputParams = new HashMap<String, Object>();

    private Map<Widget, String> uiParams = new HashMap<Widget, String>();

    private boolean outputTabRequired = false;

    private int height = 100;

    private Text txtOutput;

    private CTabItem inputTab, outputTab;

    private final Color warningColor = new Color(Display.getCurrent(), 255, 255, 200);

    public ProcessExecutionDialog(Shell parentShell, IMap map, ProcessFactory factory,
            Name processName) {
        super(parentShell);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE);

        this.map = map;
        this.factory = factory;
        this.processName = processName;
        this.windowTitle = factory.getTitle(processName).toString();
    }

    @Override
    public void create() {
        super.create();

        setTitle(getShell().getText());
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

        // 0. Tab Folder
        final CTabFolder parentTabFolder = new CTabFolder(area, SWT.BOTTOM);
        parentTabFolder.setUnselectedCloseVisible(false);
        parentTabFolder.setLayout(new FillLayout());
        parentTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // 1. Process execution
        inputTab = createInputTab(parentTabFolder);

        // 2. Simple output
        if (outputTabRequired) {
            outputTab = createOutputTab(parentTabFolder);
        }

        // 3. Help
        createHelpTab(parentTabFolder);

        parentTabFolder.setSelection(inputTab);

        // set maximum height
        area.pack();
        parentTabFolder.pack();

        height = parentTabFolder.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 145;
        height = height > 650 ? 650 : height;

        return area;
    }

    private CTabItem createOutputTab(final CTabFolder parentTabFolder) {
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

    private CTabItem createHelpTab(final CTabFolder parentTabFolder) {
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

    private CTabItem createInputTab(final CTabFolder parentTabFolder) {
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
        for (Entry<String, Parameter<?>> entrySet : paramInfo.entrySet()) {
            inputParams.put(entrySet.getValue().key, entrySet.getValue().sample);
            insertControl(container, entrySet.getValue());
        }

        // output location
        Map<String, Parameter<?>> resultInfo = factory.getResultInfo(processName, null);
        for (Entry<String, Parameter<?>> entrySet : resultInfo.entrySet()) {
            Class<?> binding = entrySet.getValue().type;
            boolean outputWidgetRequired = false;
            FileDataType fileDataType = FileDataType.SHAPEFILE;
            if (binding.isAssignableFrom(SimpleFeatureCollection.class)) {
                outputWidgetRequired = true;
                fileDataType = FileDataType.SHAPEFILE;
            } else if (binding.isAssignableFrom(Geometry.class)) {
                outputWidgetRequired = true;
                fileDataType = FileDataType.SHAPEFILE;
            } else if (binding.isAssignableFrom(ReferencedEnvelope.class)) {
                outputWidgetRequired = true;
                fileDataType = FileDataType.SHAPEFILE;
            } else if (binding.isAssignableFrom(BoundingBox.class)) {
                outputWidgetRequired = true;
                fileDataType = FileDataType.SHAPEFILE;
            } else if (binding.isAssignableFrom(GridCoverage2D.class)) {
                outputWidgetRequired = true;
                fileDataType = FileDataType.RASTER;
            } else {
                outputTabRequired = true;
            }

            if (outputWidgetRequired) {
                outputParams.put(entrySet.getValue().key, entrySet.getValue().sample);
                OutputLocationWidget view = new OutputLocationWidget(fileDataType, SWT.SAVE);
                view.create(container, SWT.BORDER, outputParams, entrySet.getValue());
            }
        }

        scroller.setContent(container);
        tab.setControl(scroller);

        scroller.pack();
        container.pack();

        scroller.setMinSize(450, container.getSize().y - 2);
        scroller.setExpandVertical(true);
        scroller.setExpandHorizontal(true);

        return tab;
    }

    private void insertParamTitle(Composite container, final Parameter<?> param) {
        String postfix = ""; //$NON-NLS-1$
        if (param.type.isAssignableFrom(Geometry.class)) {
            postfix = "(WKT)"; //$NON-NLS-1$
        }

        // capitalize title
        String title = param.title.toString();
        title = Character.toUpperCase(title.charAt(0)) + title.substring(1);

        CLabel lblName = new CLabel(container, SWT.NONE);
        lblName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

        FontData[] fontData = lblName.getFont().getFontData();
        for (int i = 0; i < fontData.length; ++i) {
            fontData[i].setStyle(SWT.NORMAL);
        }
        lblName.setFont(new Font(container.getDisplay(), fontData));

        if (param.required) {
            Image image = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage(); //$NON-NLS-1$
            lblName.setImage(image);
            lblName.setText(title + postfix);
        } else {
            lblName.setText(title + Messages.ProcessExecutionDialog_optional + postfix);
        }
        lblName.setToolTipText(param.description.toString());
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

                    inputParams.put(param.key, sfc);

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

                            // default value
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
                    inputParams.put(param.key, sfc);
                }
            });
        } else if (param.type.isAssignableFrom(Geometry.class)) {
            // wkt geometry parameters
            GeometryWidget geometryView = new GeometryWidget(map);
            geometryView.create(container, SWT.NONE, inputParams, param);
        } else if (Filter.class.isAssignableFrom(param.type)) {
            // filter parameters
            FilterWidget filterView = new FilterWidget(map);
            filterView.create(container, SWT.NONE, inputParams, param);
        } else if (param.type.isAssignableFrom(CoordinateReferenceSystem.class)) {
            // coordinate reference system parameters
            CrsWidget crsView = new CrsWidget(map);
            crsView.create(container, SWT.NONE, inputParams, param);
        } else if (BoundingBox.class.isAssignableFrom(param.type)) {
            // bounding box parameters
            BoundingBoxWidget boundingBoxView = new BoundingBoxWidget(map);
            boundingBoxView.create(container, SWT.NONE, inputParams, param);
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
                            inputParams.put(param.key, enumVal);
                            break;
                        }
                    }
                }
            });
        } else if (Number.class.isAssignableFrom(param.type)) {
            // number parameters = Byte, Double, Float, Integer, Long, Short
            if (Double.class.isAssignableFrom(param.type)
                    || Float.class.isAssignableFrom(param.type)) {
                NumberDataWidget numberView = new NumberDataWidget(map);
                numberView.create(container, SWT.NONE, inputParams, param);
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
                            inputParams.put(param.key, obj);
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
                    Boolean value = cboBoolean.getSelectionIndex() == 0 ? Boolean.TRUE
                            : Boolean.FALSE;
                    inputParams.put(param.key, value);
                }
            });
        } else {
            Map<String, Object> metadata = param.metadata;
            if (metadata == null || metadata.size() == 0) {
                // other literal parameters
                LiteralDataWidget literalView = new LiteralDataWidget(map);
                literalView.create(container, SWT.NONE, inputParams, param);
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
                            inputParams.put(param.key, cboField.getText());
                        }
                    });

                } else {
                    // other literal parameters
                    LiteralDataWidget filterView = new LiteralDataWidget(map);
                    filterView.create(container, SWT.NONE, inputParams, param);
                }
            }
        }
    }

    @SuppressWarnings("nls")
    private boolean validOutput() {
        for (Entry<String, Object> entrySet : outputParams.entrySet()) {
            File outputFile = new File(outputParams.get(entrySet.getKey()).toString());
            if (!outputFile.exists()) {
                continue;
            }

            String msg = Messages.ProcessExecutionDialog_overwriteconfirm;
            if (MessageDialog.openConfirm(getParentShell(), getShell().getText(), msg)) {
                if (outputFile.getName().toLowerCase().endsWith(".tif")) {
                    if (!outputFile.delete()) {
                        msg = Messages.ProcessExecutionDialog_deletefailed;
                        MessageDialog.openInformation(getParentShell(), getShell().getText(), msg);
                        return false;
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
                    return true;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("nls")
    @Override
    protected void okPressed() {
        // check required parameters
        Map<String, Parameter<?>> paramInfo = factory.getParameterInfo(processName);
        for (Entry<String, Parameter<?>> entrySet : paramInfo.entrySet()) {
            String key = entrySet.getValue().key;
            if (entrySet.getValue().required && inputParams.get(key) == null) {
                StringBuffer sb = new StringBuffer(Messages.ProcessExecutionDialog_requiredparam);
                sb.append(System.getProperty("line.separator")).append(" - ");
                sb.append(entrySet.getValue().getTitle());
                MessageDialog.openInformation(getParentShell(), windowTitle, sb.toString());
                return;
            }
        }

        // check output files
        if (!validOutput()) {
            return;
        }

        ProcessExecutorOperation runnable = new ProcessExecutorOperation(map, factory, processName,
                inputParams, outputParams);
        try {
            new ProgressMonitorDialog(getParentShell()).run(true, true, runnable);
        } catch (InvocationTargetException e) {
            ToolboxPlugin.log(e.getMessage());
            MessageDialog.openError(getParentShell(), Messages.General_Error, e.getMessage());
        } catch (InterruptedException e) {
            ToolboxPlugin.log(e.getMessage());
            MessageDialog.openInformation(getParentShell(), Messages.General_Cancelled,
                    e.getMessage());
        }

        if (outputTabRequired) {
            String outputText = runnable.getOutputText();
            if (outputText.length() > 0) {
                outputTab.getParent().setSelection(outputTab);
                String pattern = "[,()]"; //$NON-NLS-1$
                String message = outputText.replaceAll(pattern,
                        System.getProperty("line.separator")); //$NON-NLS-1$
                txtOutput.setText(message);
            }
        } else {
            super.okPressed();
        }
    }
}
