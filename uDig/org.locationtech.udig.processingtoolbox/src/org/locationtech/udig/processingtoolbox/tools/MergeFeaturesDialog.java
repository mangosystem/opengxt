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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
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
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Combines multiple input datasets of the same data type into a single, new output dataset.
 * 
 * @author MapPlus
 */
public class MergeFeaturesDialog extends AbstractGeoProcessingDialog implements
        IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(MergeFeaturesDialog.class);

    static final String[] columns = new String[] { Messages.Feature_Name, Messages.Feature_Type,
            Messages.Feature_CRS };

    private Table inputTable;

    private Button btnPoint, btnLine, btnPolygon;

    private Combo cboTemplate;

    private SimpleFeatureCollection templateFc;

    public MergeFeaturesDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);

        this.windowTitle = Messages.MergeFeaturesDialog_title;
        this.windowDesc = Messages.MergeFeaturesDialog_description;
        this.windowSize = new Point(650, 400);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.BORDER);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        // feature types
        Group group = uiBuilder.createGroup(container, Messages.Feature_FeatureType, true, 2);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        group.setLayout(new GridLayout(3, false));
        btnPoint = uiBuilder.createRadioButton(group, Messages.Feature_Point, null, 1);
        btnLine = uiBuilder.createRadioButton(group, Messages.Feature_LineString, null, 1);
        btnPolygon = uiBuilder.createRadioButton(group, Messages.Feature_Polygon, null, 1);

        btnPoint.addSelectionListener(selectionListener);
        btnLine.addSelectionListener(selectionListener);
        btnPolygon.addSelectionListener(selectionListener);

        // template features
        uiBuilder.createLabel(container, Messages.MergeFeaturesDialog_TemplateFeatures, null, 2);
        cboTemplate = uiBuilder.createCombo(container, 2);
        cboTemplate.addModifyListener(modifyListener);

        // input features
        uiBuilder.createLabel(container, Messages.MergeFeaturesDialog_InputFeatures, null, 2);
        inputTable = uiBuilder.createTable(container, columns, 2);

        locationView = new OutputDataWidget(FileDataType.SHAPEFILE, SWT.SAVE);
        locationView.create(container, SWT.BORDER, 2, 1);
        locationView.setFile(new File(ToolboxView.getWorkspace(), "merge.shp")); //$NON-NLS-1$

        area.pack(true);
        this.windowSize = new Point(650, area.getSize().y + 160);
        return area;
    }

    ModifyListener modifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            Widget widget = e.widget;
            if (invalidWidgetValue(widget)) {
                return;
            }

            if (widget.equals(cboTemplate)) {
                templateFc = MapUtils.getFeatures(map, cboTemplate.getText());
            }
        }
    };

    SelectionListener selectionListener = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;

            VectorLayerType layerType = VectorLayerType.ALL;
            if (widget.equals(btnPoint) && btnPoint.getSelection()) {
                layerType = VectorLayerType.POINT;
            } else if (widget.equals(btnLine) && btnLine.getSelection()) {
                layerType = VectorLayerType.LINESTRING;
            } else if (widget.equals(btnPolygon) && btnPolygon.getSelection()) {
                layerType = VectorLayerType.POLYGON;
            }

            fillLayers(map, cboTemplate, layerType);
            fillLayersToTable(map, inputTable, layerType);
            templateFc = null;
            if (cboTemplate.getItemCount() > 0) {
                cboTemplate.select(0);
            }
        }
    };

    private void fillLayersToTable(IMap map, Table table, VectorLayerType layerType) {
        table.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName() != null && layer.hasResource(FeatureSource.class)) {
                GeometryDescriptor descriptor = layer.getSchema().getGeometryDescriptor();
                Class<?> geometryBinding = descriptor.getType().getBinding();
                String crs = descriptor.getCoordinateReferenceSystem().toWKT();

                switch (layerType) {
                case ALL:
                    insertTableItem(table, layer, geometryBinding.getSimpleName(), crs);
                    break;
                case LINESTRING:
                    if (geometryBinding.isAssignableFrom(LineString.class)
                            || geometryBinding.isAssignableFrom(MultiLineString.class)) {
                        insertTableItem(table, layer, geometryBinding.getSimpleName(), crs);
                    }
                    break;
                case POINT:
                    if (geometryBinding.isAssignableFrom(com.vividsolutions.jts.geom.Point.class)
                            || geometryBinding.isAssignableFrom(MultiPoint.class)) {
                        insertTableItem(table, layer, geometryBinding.getSimpleName(), crs);
                    }
                    break;
                case POLYGON:
                    if (geometryBinding.isAssignableFrom(Polygon.class)
                            || geometryBinding.isAssignableFrom(MultiPolygon.class)) {
                        insertTableItem(table, layer, geometryBinding.getSimpleName(), crs);
                    }
                    break;
                case MULTIPART:
                    if (geometryBinding.isAssignableFrom(MultiPolygon.class)
                            || geometryBinding.isAssignableFrom(MultiLineString.class)
                            || geometryBinding.isAssignableFrom(MultiPoint.class)) {
                        insertTableItem(table, layer, geometryBinding.getSimpleName(), crs);
                    }
                    break;
                case POLYLINE:
                    if (geometryBinding.isAssignableFrom(Point.class)
                            || geometryBinding.isAssignableFrom(MultiPoint.class)
                            || geometryBinding.isAssignableFrom(Polygon.class)
                            || geometryBinding.isAssignableFrom(MultiPolygon.class)) {
                        insertTableItem(table, layer, geometryBinding.getSimpleName(), crs);
                    }
                    break;
                default:
                    break;
                }
            }
        }
    }

    private void insertTableItem(Table table, ILayer layer, String layerType, String crs) {
        TableItem item = new TableItem(table, SWT.NONE);
        item.setText(new String[] { layer.getName(), layerType, crs });
        item.setData(layer);
    }

    @Override
    protected void okPressed() {
        if (!existCheckedItem(inputTable)) {
            openInformation(getShell(), Messages.MergeFeaturesDialog_Warning);
            return;
        }

        // check file
        File outputFile = new File(locationView.getFile());
        if (outputFile.exists()) {
            String msg = Messages.ProcessExecutionDialog_overwriteconfirm;
            if (MessageDialog.openConfirm(getParentShell(), getShell().getText(), msg)) {
                if (!MapUtils.confirmSpatialFile(outputFile)) {
                    msg = Messages.ProcessExecutionDialog_deletefailed;
                    MessageDialog.openInformation(getParentShell(), getShell().getText(), msg);
                    return;
                }
            } else {
                return;
            }
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
        monitor.beginTask(String.format(Messages.Task_Executing, windowTitle), 5);
        try {
            monitor.worked(increment);

            List<SimpleFeatureCollection> fcList = new ArrayList<SimpleFeatureCollection>();
            for (TableItem item : inputTable.getItems()) {
                if (item.getChecked()) {
                    fcList.add(MapUtils.getFeatures((ILayer) item.getData()));
                }
            }

            String outputName = FilenameUtils.getBaseName(locationView.getFile());

            MergeOp op = new MergeOp(outputName);
            op.setOutputDataStore(locationView.getDataStore());

            SimpleFeatureSource sfs = op.merge(fcList, templateFc, monitor);
            if (sfs != null) {
                MapUtils.addFeaturesToMap(map, new File(locationView.getFile()), outputName);
            } else {
                ToolboxPlugin.log(windowTitle + " : Failed to merge : " + outputName); //$NON-NLS-1$
            }

            monitor.worked(increment);
        } catch (Exception e) {
            ToolboxPlugin.log(e.getMessage());
            throw new InvocationTargetException(e.getCause(), e.getMessage());
        } finally {
            monitor.done();
        }
    }

    static final class MergeOp extends GeneralOperation {
        static final Logger LOGGER = Logging.getLogger(MergeOp.class);

        private String outputName = "Merge"; //$NON-NLS-1$

        public MergeOp(String outputName) {
            this.outputName = outputName;
        }

        public SimpleFeatureSource merge(List<SimpleFeatureCollection> fcList,
                SimpleFeatureCollection template, IProgressMonitor monitor) throws IOException {
            if (template == null) {
                template = fcList.get(0);
            }

            // prepare feature type
            SimpleFeatureType destSchema = FeatureTypes.build(template, outputName);

            // prepare transactional feature store
            IFeatureInserter featureWriter = getFeatureWriter(destSchema);
            try {
                for (SimpleFeatureCollection inputFeatures : fcList) {
                    monitor.worked(1);
                    SimpleFeatureType inputSchema = inputFeatures.getSchema();
                    SimpleFeatureIterator featureIter = inputFeatures.features();
                    try {
                        while (featureIter.hasNext()) {
                            SimpleFeature feature = featureIter.next();

                            // create feature and set geometry
                            SimpleFeature newFeature = featureWriter.buildFeature();
                            for (AttributeDescriptor ad : destSchema.getAttributeDescriptors()) {
                                if (ad instanceof GeometryDescriptor
                                        || inputSchema.indexOf(ad.getName()) == -1) {
                                    continue;
                                }
                                newFeature.setAttribute(ad.getName(),
                                        feature.getAttribute(ad.getName()));
                            }
                            newFeature.setDefaultGeometry(feature.getDefaultGeometry());
                            featureWriter.write(newFeature);
                        }
                    } finally {
                        featureIter.close();
                    }
                }
            } catch (IOException e) {
                featureWriter.rollback(e);
            } finally {
                featureWriter.close();
            }

            return featureWriter.getFeatureSource();
        }
    }
}