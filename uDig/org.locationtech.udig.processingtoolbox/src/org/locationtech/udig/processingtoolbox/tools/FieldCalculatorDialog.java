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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.operations.TextColumn;
import org.geotools.process.spatialstatistics.storage.DataStoreFactory;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.storage.ShapeFileEditor;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ICatalog;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IResolve.Status;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceFactory;
import org.locationtech.udig.catalog.internal.shp.ShpGeoResourceImpl;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.WidgetBuilder;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.parameter.Parameter;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Field Calculator Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class FieldCalculatorDialog extends AbstractGeoProcessingDialog implements
        IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(FieldCalculatorDialog.class);

    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_hhmmss_S");

    private final String space = " ";

    private IMap map = null;

    private ILayer layer = null;

    private SimpleFeatureCollection source = null;
    
    private String geom_field = null;

    private Button btnClear, btnTest, chkSelected;

    private Table fieldTable, valueTable;

    private Combo cboLayer, cboField, cboType;

    private Spinner spnLen;

    private Text txtExpression;

    public FieldCalculatorDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        this.map = map;
        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);

        this.windowTitle = Messages.FieldCalculatorDialog_title;
        this.windowDesc = Messages.FieldCalculatorDialog_description;
        this.windowSize = new Point(650, 600);
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
        btnTest = createButton(parent, 2001, Messages.ExpressionBuilderDialog_Test, false);
        btnClear.setEnabled(false);
        btnTest.setEnabled(false);

        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

        btnClear.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                txtExpression.setText("");
            }
        });

        btnTest.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    Expression expression = ECQL.toExpression(txtExpression.getText());
                    Object evaluated = expression.evaluate(source.features().next());
                    String msg = "Evaluated value: " + evaluated;
                    MessageDialog.openInformation(getParentShell(),
                            Messages.ExpressionBuilderDialog_Test, msg);
                } catch (CQLException e) {
                    MessageDialog.openInformation(getParentShell(),
                            Messages.ExpressionBuilderDialog_Test, e.getLocalizedMessage());
                }
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

        // ========================================================
        // 1. layer
        // ========================================================
        Group grpLayer = widget.createGroup(container, Messages.FieldCalculatorDialog_Layer_Group,
                false, 1);

        Group grpCombo = new Group(grpLayer, SWT.SHADOW_ETCHED_IN);
        grpCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        grpCombo.setLayout(new GridLayout(2, false));

        widget.createLabel(grpCombo, Messages.FieldCalculatorDialog_Layer, null, 1);
        cboLayer = widget.createCombo(grpCombo, 1, true);
        for (ILayer layer : map.getMapLayers()) {
            if (layer.hasResource(FeatureSource.class)) {
                // current, only support shapefile
                if (layer.getGeoResource().canResolve(ShpGeoResourceImpl.class)) {
                    cboLayer.add(layer.getName());
                }
            }
        }
        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (cboLayer.getText().length() == 0) {
                    cboField.removeAll();
                    fieldTable.removeAll();
                    chkSelected.setSelection(false);
                } else {
                    layer = MapUtils.getLayer(map, cboLayer.getText());
                    source = MapUtils.getFeatures(layer);
                    fillFields(cboField, source.getSchema(), FieldType.ALL);
                    updateFields(source.getSchema());

                    // check selected features
                    chkSelected.setSelection(layer.getFilter() != Filter.EXCLUDE);
                }
            }
        });

        widget.createLabel(grpCombo, null, null, 1);
        chkSelected = widget.createCheckbox(grpCombo, Messages.FieldCalculatorDialog_Selected,
                null, 1);

        widget.createLabel(grpCombo, Messages.FieldCalculatorDialog_Field, null, 1);

        Composite subCon = new Composite(grpCombo, SWT.NONE);
        subCon.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        subCon.setLayout(widget.createGridLayout(5, false, 0, 0));

        cboField = widget.createCombo(subCon, 1, false);
        cboField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                String fieldName = cboField.getText();
                if (source.getSchema().indexOf(fieldName) != -1) {
                    AttributeDescriptor descriptor = source.getSchema().getDescriptor(fieldName);
                    spnLen.setSelection(FeatureTypes.getAttributeLength(descriptor));
                    cboType.setText(descriptor.getType().getBinding().getSimpleName());
                }
            }
        });

        widget.createLabel(subCon, Messages.FieldCalculatorDialog_FieldType, null, 1);
        cboType = widget.createCombo(subCon, 1, false);
        for (String fieldType : TextColumn.getFieldTypes(false)) {
            cboType.add(fieldType);
        }

        widget.createLabel(subCon, Messages.FieldCalculatorDialog_FieldLen, null, 1);
        spnLen = widget.createSpinner(subCon, 10, 1, 255, 0, 1, 2, 1);

        // ========================================================
        // 2. Fields & Functions
        // ========================================================
        final int defaultWidth = 200;
        Group grpFields = widget.createGroup(grpLayer, Messages.FieldCalculatorDialog_Fields,
                false, 1);
        GridData gridDataField = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
        gridDataField.widthHint = defaultWidth;
        grpFields.setLayoutData(gridDataField);
        grpFields.setLayout(new GridLayout(1, true));

        fieldTable = widget.createListTable(grpFields,
                new String[] { Messages.FieldCalculatorDialog_Fields }, 1, 100);
        if (source != null) {
            updateFields(source.getSchema());
        }
        fieldTable.getColumns()[0].setWidth(defaultWidth - 40);

        // double click event
        fieldTable.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                String selection = "[" + fieldTable.getSelection()[0].getText() + "]";
                updateExpression(selection);
            }
        });

        // ========================================================
        // filter functions
        // http://docs.geotools.org/latest/userguide/library/main/filter.html
        // ========================================================
        Group grpValues = widget.createGroup(grpLayer, Messages.FieldCalculatorDialog_Functions,
                false, 1);
        grpValues.setLayout(new GridLayout(1, true));

        valueTable = widget.createListTable(grpValues,
                new String[] { Messages.FieldCalculatorDialog_Functions }, 1, 100);
        updateFunctions();
        valueTable.getColumns()[0].setWidth(340);
        grpValues.setText(Messages.FieldCalculatorDialog_Functions + "("
                + valueTable.getItemCount() + ")");

        // double click event
        valueTable.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (valueTable.getSelectionCount() > 0) {
                    String selection = valueTable.getSelection()[0].getText();
                    if (selection.contains("[geom]")) {
                        selection = selection.replace("[geom]", "[" + geom_field + "]");
                    }
                    updateExpression(selection);
                }
            }
        });

        // ========================================================
        // 3. Operators & Expression
        // ========================================================
        Group grpOpr = new Group(container, SWT.SHADOW_ETCHED_IN);
        grpOpr.setText(Messages.FieldCalculatorDialog_Operators);
        grpOpr.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        grpOpr.setLayout(new GridLayout(16, true));

        // operators
        GridData btnLayout = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        final String[] oprs = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "+",
                "-", "*", "/", "(", ")" };
        final List<String> exceptions = Arrays.asList(new String[] { "+", "-", "*", "/" });
        Button[] btnOp = new Button[oprs.length];
        for (int idx = 0; idx < oprs.length; idx++) {
            btnOp[idx] = widget.createButton(grpOpr, oprs[idx], oprs[idx], btnLayout);
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
        txtExpression = new Text(grpOpr, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData txtGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 16, 1);
        txtGridData.heightHint = 50;
        txtExpression.setLayoutData(txtGridData);

        txtExpression.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                btnClear.setEnabled(txtExpression.getText().length() > 0);
                btnTest.setEnabled(btnClear.getEnabled());
            }
        });

        if (source == null && cboLayer.getItemCount() > 0) {
            cboLayer.select(0);
        }

        container.pack(true);
        area.pack(true);
        return area;
    }

    private void updateFields(SimpleFeatureType schema) {
        fieldTable.removeAll();
        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (descriptor instanceof GeometryDescriptor) {
                this.geom_field = descriptor.getLocalName();
                TableItem item = new TableItem(fieldTable, SWT.NULL);
                item.setText(descriptor.getLocalName());
                FontData fontData = item.getFont().getFontData()[0];
                fontData.setStyle(SWT.BOLD);
                item.setFont(new Font(item.getFont().getDevice(), fontData));
            } else {
                TableItem item = new TableItem(fieldTable, SWT.NULL);
                item.setText(descriptor.getLocalName());
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
                        if (functionName.getName().matches(regex)) {
                            continue;
                        }
                        TableItem item = new TableItem(valueTable, SWT.NULL);
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
                        item.setText(buffer.toString());
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
        if (source == null || cboField.getText().length() == 0 || cboType.getText().length() == 0
                || txtExpression.getText().length() == 0) {
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

        String folder = ToolboxView.getWorkspace();
        DataStore outputDataStore = DataStoreFactory.getShapefileDataStore(folder);
        String outputTypeName = null;

        try {
            // Convert the given monitor into a progress instance
            final SubMonitor progress = SubMonitor.convert(monitor, 100);

            // prepare parameters
            Expression expression = ECQL.toExpression(txtExpression.getText());
            String field = cboField.getText();
            Class<?> fieldBinding = String.class;
            if (FeatureTypes.existProeprty(source.getSchema(), field)) {
                AttributeDescriptor attr = source.getSchema().getDescriptor(field);
                fieldBinding = attr.getType().getBinding();
            } else {
                try {
                    fieldBinding = new TextColumn().findBestBinding(cboType.getText());
                } catch (Exception ee) {
                    ToolboxPlugin.log(ee.getMessage());
                }
            }
            int length = spnLen.getSelection();
            monitor.worked(increment);

            // execute process
            FieldCalculatorOperation process = new FieldCalculatorOperation();

            outputTypeName = process.getOutputTypeName();
            process.setOutputDataStore(outputDataStore);
            process.setOutputTypeName(process.getOutputTypeName());
            SimpleFeatureCollection features = process.execute(source, expression, field,
                    fieldBinding, length, progress.newChild(70));

            // post process
            if (features != null) {
                Date now = Calendar.getInstance().getTime();

                // remove service
                IService service = layer.getGeoResource().service(progress.newChild(5));
                final ID id = service.getID();
                final Map<java.lang.String, Serializable> params = service.getConnectionParams();
                service.dispose(progress.newChild(10));
                while (service.getStatus() == Status.CONNECTED) {
                    Thread.sleep(100);
                }

                // replace dbf file
                String shpPath = DataUtilities.urlToFile(id.toURL()).getPath(); // .shp
                File dbfFile = new File(FilenameUtils.removeExtension(shpPath) + ".dbf");

                File tempFile = new File(dbfFile.getParent(), "fc_" + df.format(now) + ".dbf");
                if (dbfFile.renameTo(tempFile)) {
                    File newFile = new File(folder, process.getOutputTypeName() + ".dbf");
                    org.apache.commons.io.FileUtils.copyFile(newFile, dbfFile);
                    tempFile.delete();
                    updateFields(layer.getSchema());
                    fillFields(cboField, layer.getSchema(), FieldType.ALL);
                    cboType.setText("");
                } else {
                    throw new Exception(Messages.FieldCalculatorDialog_Failed);
                }

                // reload service
                IServiceFactory serviceFactory = CatalogPlugin.getDefault().getServiceFactory();
                ICatalog catalog = CatalogPlugin.getDefault().getLocalCatalog();
                IService replacement = serviceFactory.createService(params).get(0);
                catalog.replace(id, replacement);
                layer.refresh(map.getViewportModel().getBounds());

                monitor.worked(increment);
            }
            monitor.worked(increment);
        } catch (Exception e) {
            ToolboxPlugin.log(e.getMessage());
            throw new InvocationTargetException(e.getCause(), e.getMessage());
        } finally {
            // finally delete temporary files
            new ShapeFileEditor().remove(outputDataStore, outputTypeName);
            monitor.done();
        }
    }

    static final class FieldCalculatorOperation extends GeneralOperation {
        protected static final Logger LOGGER = Logging.getLogger(FieldCalculatorOperation.class);

        public SimpleFeatureCollection execute(SimpleFeatureCollection features,
                Expression expression, String field, Class<?> fieldBinding, int length,
                IProgressMonitor monitor) throws IOException {
            String typeName = getOutputTypeName(); // features.getSchema().getTypeName();
            field = FeatureTypes.validateProperty(features.getSchema(), field);

            SimpleFeatureType schema = FeatureTypes.build(features.getSchema(), typeName);
            if (FeatureTypes.existProeprty(schema, field)) {
                AttributeDescriptor attr = schema.getDescriptor(field);
                fieldBinding = attr.getType().getBinding();
            } else {
                schema = FeatureTypes.add(schema, field, fieldBinding, length);
            }

            SubMonitor progress = SubMonitor.convert(monitor, 100);
            SubMonitor loopProgress = progress.newChild(100).setWorkRemaining(features.size());

            IFeatureInserter featureWriter = getFeatureWriter(schema);
            SimpleFeatureIterator featureIter = null;
            try {
                featureIter = features.features();
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();

                    loopProgress.newChild(1);
                    if (geometry == null || geometry.isEmpty()) {
                        continue;
                    }

                    SimpleFeature newFeature = featureWriter.buildFeature(null);
                    featureWriter.copyAttributes(feature, newFeature, true);

                    // expression
                    Object value = expression.evaluate(feature);
                    newFeature.setAttribute(field, Converters.convert(value, fieldBinding));

                    featureWriter.write(newFeature);
                }
            } catch (Exception e) {
                featureWriter.rollback(e);
            } finally {
                featureWriter.close(featureIter);
            }

            return featureWriter.getFeatureCollection();
        }
    }

}
