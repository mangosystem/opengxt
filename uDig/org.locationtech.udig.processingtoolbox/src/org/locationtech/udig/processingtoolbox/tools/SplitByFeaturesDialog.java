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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.process.spatialstatistics.core.StringHelper;
import org.geotools.process.spatialstatistics.storage.NamePolicy;
import org.geotools.process.spatialstatistics.storage.ShapeExportOperation;
import org.geotools.process.spatialstatistics.transformation.ClipWithGeometryFeatureCollection;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.FieldType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.IMap;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

/**
 * Splits the features creates a subset of multiple output features.
 * 
 * @author MapPlus
 */
public class SplitByFeaturesDialog extends AbstractGeoProcessingDialog implements
        IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(SplitByFeaturesDialog.class);

    private Combo cboInputFeatures, cboSplitFeatures, cboSplitField, cboNamePolicy;

    private Button chkPrefix;

    private Table uniqueTable;

    private NamePolicy namePolicy = NamePolicy.NORMAL;

    private SimpleFeatureCollection inputFeatures, splitFeatures;

    private String prefix = null;

    public SplitByFeaturesDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);

        this.windowTitle = Messages.SplitByFeaturesDialog_title;
        this.windowDesc = Messages.SplitByFeaturesDialog_description;
        this.windowSize = new Point(650, 600);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.BORDER);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        // input features
        uiBuilder.createLabel(container, Messages.SplitByAttributesDialog_InputFeatures, null, 2);
        cboInputFeatures = uiBuilder.createCombo(container, 2);
        cboInputFeatures.addModifyListener(modifyListener);

        // split features
        uiBuilder.createLabel(container, Messages.SplitByFeaturesDialog_SplitFeatures, null, 2);
        cboSplitFeatures = uiBuilder.createCombo(container, 2);
        cboSplitFeatures.addModifyListener(modifyListener);

        // split field
        uiBuilder.createLabel(container, Messages.SplitByAttributesDialog_SplitField, null, 1);
        cboSplitField = uiBuilder.createCombo(container, 1);
        cboSplitField.addModifyListener(modifyListener);

        chkPrefix = uiBuilder.createCheckbox(container, Messages.SplitByAttributesDialog_UsePrefix,
                null, 2);
        chkPrefix.setSelection(true);
        chkPrefix.addSelectionListener(selectionListener);

        // output layers
        Group grpTable = uiBuilder.createGroup(container,
                Messages.SplitByAttributesDialog_OutputLayers, false, 2);
        grpTable.setLayout(new GridLayout(4, false));

        String[] columns = new String[] { Messages.SplitByAttributesDialog_OutputName,
                Messages.SplitByAttributesDialog_FieldValue, Messages.SplitByAttributesDialog_Count };
        uniqueTable = uiBuilder.createEditableTable(grpTable, columns, 4, 1);

        // 4. name policy
        uiBuilder.createLabel(container, Messages.SplitByAttributesDialog_NamePolicy, null, 1);
        cboNamePolicy = uiBuilder.createCombo(container, 2);
        cboNamePolicy.addModifyListener(modifyListener);
        setComboItems(cboNamePolicy, NamePolicy.class);
        cboNamePolicy.select(0);

        locationView = new OutputDataWidget(FileDataType.FOLDER, SWT.OPEN);
        locationView.create(container, SWT.BORDER, 2, 1);
        locationView.setFolder(ToolboxView.getWorkspace()); // default location

        // load layers
        fillLayers(map, cboInputFeatures, VectorLayerType.ALL);
        fillLayers(map, cboSplitFeatures, VectorLayerType.POLYGON);

        area.pack(true);
        return area;
    }

    SelectionListener selectionListener = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;
            if (widget.equals(chkPrefix)) {
                updatePrefix();
            }
        }
    };

    ModifyListener modifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            Widget widget = e.widget;
            if (invalidWidgetValue(widget)) {
                return;
            }

            if (widget.equals(cboInputFeatures)) {
                inputFeatures = MapUtils.getFeatures(map, cboInputFeatures.getText());
                prefix = cboInputFeatures.getText();
                updatePrefix();
            } else if (widget.equals(cboSplitFeatures)) {
                splitFeatures = MapUtils.getFeatures(map, cboSplitFeatures.getText());
                if (splitFeatures != null) {
                    fillFields(cboSplitField, splitFeatures.getSchema(), FieldType.ALL);
                    uniqueTable.removeAll();
                }
            } else if (widget.equals(cboSplitField)) {
                BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(), new Runnable() {
                    @Override
                    public void run() {
                        buildTables(cboSplitField.getText());
                    }
                });
            } else if (widget.equals(cboNamePolicy)) {
                namePolicy = NamePolicy.valueOf(cboNamePolicy.getText());
            }
        }
    };

    @SuppressWarnings("unchecked")
    private void updatePrefix() {
        for (TableItem item : uniqueTable.getItems()) {
            Entry<Object, Integer> entrySet = (Entry<Object, Integer>) item.getData();
            String fieldName = entrySet.getKey().toString();
            if (chkPrefix.getSelection() && !StringHelper.isNullOrEmpty(prefix)) {
                fieldName = prefix + "_" + entrySet.getKey().toString(); //$NON-NLS-1$
            }
            item.setText(new String[] { fieldName, entrySet.getKey().toString(),
                    entrySet.getValue().toString() });
        }
    }

    private void buildTables(String attributeName) {
        SortedMap<Object, Integer> valueCountsMap = new TreeMap<Object, Integer>();
        SimpleFeatureIterator featureIter = splitFeatures.features();
        try {
            while (featureIter.hasNext()) {
                final SimpleFeature feature = featureIter.next();
                Object value = feature.getAttribute(attributeName);
                if (value == null) {
                    value = "Null_Value"; //$NON-NLS-1$
                }

                if (valueCountsMap.containsKey(value)) {
                    final int cnt = valueCountsMap.get(value);
                    valueCountsMap.put(value, Integer.valueOf(cnt + 1));
                } else {
                    valueCountsMap.put(value, Integer.valueOf(1));
                }
            }
        } finally {
            featureIter.close();
        }

        uniqueTable.removeAll();
        for (Entry<Object, Integer> entrySet : valueCountsMap.entrySet()) {
            TableItem item = new TableItem(uniqueTable, SWT.NONE);
            String fieldName = entrySet.getKey().toString();
            if (this.chkPrefix.getSelection() && !StringHelper.isNullOrEmpty(prefix)) {
                fieldName = prefix + "_" + entrySet.getKey().toString(); //$NON-NLS-1$
            }

            item.setText(new String[] { fieldName, entrySet.getKey().toString(),
                    entrySet.getValue().toString() });
            item.setData(entrySet);
            item.setChecked(true);
        }
    }

    @Override
    protected void okPressed() {
        if (inputFeatures == null || splitFeatures == null || !existCheckedItem(uniqueTable)) {
            openInformation(getShell(), Messages.SplitByAttributesDialog_Warning);
            return;
        }

        try {
            PlatformUI.getWorkbench().getProgressService().run(false, true, this);
            openInformation(getShell(), Messages.General_Completed);
            super.okPressed();
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), Messages.General_Error, e.getMessage());
        } catch (InterruptedException e) {
            MessageDialog.openInformation(getShell(), Messages.General_Cancelled, e.getMessage());
        }
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        monitor.beginTask(String.format(Messages.Task_Executing, windowTitle),
                uniqueTable.getItems().length + 1);
        try {
            monitor.worked(increment);

            final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
            PropertyName uniqueField = ff.property(cboSplitField.getText());

            ShapeExportOperation process = new ShapeExportOperation();
            process.setOutputDataStore(locationView.getDataStore());
            process.setNamePolicy(namePolicy);

            for (TableItem item : uniqueTable.getItems()) {
                monitor.subTask(String.format(Messages.Task_Executing, item.getText()));
                if (monitor.isCanceled()) {
                    break;
                }

                if (item.getChecked()) {
                    String layerName = item.getText();
                    final String value = item.getText(1);
                    Filter filter = ff.equal(uniqueField, ff.literal(value), true);
                    if (value.equals("Null_Value")) { //$NON-NLS-1$
                        filter = ff.isNull(uniqueField);
                    }

                    Geometry clipGeometry = unionGeometry(splitFeatures.subCollection(filter));
                    process.setOutputTypeName(layerName);
                    SimpleFeatureSource outputSfs = process
                            .execute(new ClipWithGeometryFeatureCollection(inputFeatures,
                                    clipGeometry));
                    if (outputSfs == null) {
                        ToolboxPlugin.log(windowTitle + " : Failed to export : " + layerName); //$NON-NLS-1$
                    }
                }

                monitor.worked(increment);
                Thread.sleep(100);
            }
        } catch (Exception e) {
            ToolboxPlugin.log(e.getMessage());
            throw new InvocationTargetException(e.getCause(), e.getMessage());
        } finally {
            monitor.done();
        }
    }

    private Geometry unionGeometry(SimpleFeatureCollection inutFeatures) {
        List<Geometry> geometries = new ArrayList<Geometry>();
        SimpleFeatureIterator featureIter = inutFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }
                geometries.add(geometry);
            }
        } finally {
            featureIter.close();
        }

        CascadedPolygonUnion unionOp = new CascadedPolygonUnion(geometries);
        return unionOp.union();
    }
}