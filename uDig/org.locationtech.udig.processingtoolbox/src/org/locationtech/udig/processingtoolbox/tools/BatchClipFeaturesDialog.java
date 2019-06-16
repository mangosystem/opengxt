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
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.operations.ClipWithFeaturesOperation;
import org.geotools.process.spatialstatistics.storage.ShapeExportOperation;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.internal.ui.TableSelectionWidget;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.processingtoolbox.styler.MapUtils.VectorLayerType;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Extracts multiple input features that overlay the clip features.
 * 
 * @author MapPlus
 */
public class BatchClipFeaturesDialog extends AbstractGeoProcessingDialog implements
        IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(BatchClipFeaturesDialog.class);

    private Table inputTable;

    private Combo cboLayer;

    public BatchClipFeaturesDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);

        this.windowTitle = Messages.BatchClipFeaturesDialog_title;
        this.windowDesc = Messages.BatchClipFeaturesDialog_description;
        this.windowSize = ToolboxPlugin.rescaleSize(parentShell, 650, 500);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.BORDER);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        Image image = ToolboxPlugin.getImageDescriptor("icons/public_co.gif").createImage(); //$NON-NLS-1$

        Group group = uiBuilder.createGroup(container,
                Messages.BatchReprojectFeaturesDialog_SelectLayers, false, 2);
        inputTable = uiBuilder.createTable(group, new String[] {
                Messages.BatchReprojectFeaturesDialog_Name,
                Messages.BatchReprojectFeaturesDialog_Type,
                Messages.BatchReprojectFeaturesDialog_CRS }, 2);

        TableSelectionWidget tblSelection = new TableSelectionWidget(inputTable);
        tblSelection.create(group, SWT.NONE, 2, 1);

        // Layer
        uiBuilder.createLabel(container, Messages.BatchClipFeaturesDialog_ClipLayer, EMPTY, image,
                2);
        cboLayer = uiBuilder.createCombo(container, 2, true);
        fillLayers(map, cboLayer, VectorLayerType.POLYGON);

        locationView = new OutputDataWidget(FileDataType.FOLDER, SWT.OPEN);
        locationView.create(container, SWT.BORDER, 1, 1);
        locationView.setFolder(ToolboxView.getWorkspace());

        // load layers
        loadlayers(inputTable);

        area.pack(true);
        return area;
    }

    private void loadlayers(Table table) {
        table.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName() != null && layer.hasResource(FeatureSource.class)) {
                TableItem item = new TableItem(table, SWT.NONE);
                String type = layer.getSchema().getGeometryDescriptor().getType().getBinding()
                        .getSimpleName();
                CoordinateReferenceSystem crs = layer.getCRS();
                item.setText(new String[] { layer.getName(), type, crs.toString() });
                item.setData(layer);
            }
        }
    }

    @Override
    protected void okPressed() {
        if (!existCheckedItem(inputTable) || cboLayer.getText().length() == 0) {
            openInformation(getShell(), Messages.BatchClipFeaturesDialog_Warning);
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
                inputTable.getItems().length * increment);
        try {
            monitor.worked(increment);

            final String folder = locationView.getFolder();

            ShapeExportOperation export = new ShapeExportOperation();
            export.setOutputDataStore(locationView.getDataStore());

            SimpleFeatureCollection clipFeatures = MapUtils.getFeatures(map, cboLayer.getText());

            ClipWithFeaturesOperation clipper = new ClipWithFeaturesOperation();
            for (TableItem item : inputTable.getItems()) {
                monitor.subTask(item.getText());
                if (item.getChecked()) {
                    ILayer layer = (ILayer) item.getData();
                    SimpleFeatureCollection features = MapUtils.getFeatures(layer);

                    SimpleFeatureCollection clipped = null;
                    File file = new File(folder, layer.getName() + ".shp"); //$NON-NLS-1$
                    if (file.exists()) {
                        if (MessageDialog.openQuestion(
                                getShell(),
                                windowTitle,
                                MessageFormat.format(Messages.General_OverwriteLayer,
                                        layer.getName()))) {
                            if (MapUtils.confirmSpatialFile(file)) {
                                clipped = clipper.execute(features, clipFeatures);
                            } else {
                                openInformation(getShell(), Messages.General_Error);
                            }
                        }
                    } else {
                        clipped = clipper.execute(features, clipFeatures);
                    }

                    if (clipped != null) {
                        export.setOutputTypeName(layer.getName());
                        export.execute(clipFeatures);
                        ToolboxPlugin.log(file);
                    }
                }
                monitor.worked(increment);
            }
        } catch (Exception e) {
            ToolboxPlugin.log(e.getMessage());
            throw new InvocationTargetException(e.getCause(), e.getMessage());
        } finally {
            monitor.done();
        }
    }

}
