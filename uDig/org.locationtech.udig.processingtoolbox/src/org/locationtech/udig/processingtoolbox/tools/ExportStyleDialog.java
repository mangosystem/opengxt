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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.internal.ui.TableSelectionWidget;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Exports To SLD files
 * 
 * @author MapPlus
 */
public class ExportStyleDialog extends AbstractGeoProcessingDialog implements IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(ExportStyleDialog.class);

    private Table inputTable;

    private Button chkOverwrite;

    final StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);

    public ExportStyleDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);

        this.windowTitle = Messages.ExportStyleDialog_title;
        this.windowDesc = Messages.ExportStyleDialog_description;
        this.windowSize = new Point(650, 500);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.BORDER);
        container.setLayout(new GridLayout(2, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group group = uiBuilder.createGroup(container,
                Messages.FormatConversionDialog_SelectLayers, false, 2);
        inputTable = uiBuilder.createTable(group, new String[] {
                Messages.FormatConversionDialog_Name, Messages.FormatConversionDialog_Type,
                Messages.FormatConversionDialog_CRS }, 2);

        TableSelectionWidget tblSelection = new TableSelectionWidget(inputTable);
        tblSelection.create(group, SWT.NONE, 2, 1);

        chkOverwrite = uiBuilder.createCheckbox(container, Messages.ExportStyleDialog_overwrite, null,
                1);

        locationView = new OutputDataWidget(FileDataType.FOLDER, SWT.OPEN);
        locationView.create(container, SWT.BORDER, 2, 1);
        locationView.setFolder(ToolboxView.getWorkspace());

        // load layers
        loadlayers(inputTable);

        area.pack(true);
        return area;
    }

    private void loadlayers(Table table) {
        table.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName() != null) {
                Style style = (Style) layer.getStyleBlackboard().get(SLDContent.ID);
                if (style == null) {
                    continue;
                }

                TableItem item = new TableItem(table, SWT.NONE);

                String type = "Raster"; //$NON-NLS-1$
                if (layer.hasResource(FeatureSource.class)) {
                    type = layer.getSchema().getGeometryDescriptor().getType().getBinding()
                            .getSimpleName();
                }

                CoordinateReferenceSystem crs = layer.getCRS();
                item.setText(new String[] { layer.getName(), type, crs.toString() });
                item.setData(layer);
            }
        }
    }

    @Override
    protected void okPressed() {
        if (!existCheckedItem(inputTable)) {
            openInformation(getShell(), Messages.FormatConversionDialog_Warning);
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

            final String outputFolder = locationView.getFolder();
            final boolean overwriter = chkOverwrite.getSelection();
            final String ext = ".sld"; //$NON-NLS-1$

            for (TableItem item : inputTable.getItems()) {
                monitor.subTask(item.getText());
                if (item.getChecked()) {
                    ILayer layer = (ILayer) item.getData();
                    File sldFile = new File(outputFolder, layer.getName() + ext);

                    if (sldFile.exists()) {
                        if (overwriter) {
                            sldFile.delete();
                        } else {
                            continue;
                        }
                    }

                    Style style = (Style) layer.getStyleBlackboard().get(SLDContent.ID);
                    saveToSldFile(layer.getName(), style, sldFile);
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

    private void saveToSldFile(String styleName, Style style, File sldFile) {
        UserLayer userLayer = sf.createUserLayer();
        userLayer.setLayerFeatureConstraints(new FeatureTypeConstraint[] { null });
        userLayer.setName(styleName);
        userLayer.addUserStyle(style);

        StyledLayerDescriptor styledLayerDesc = sf.createStyledLayerDescriptor();
        styledLayerDesc.addStyledLayer(userLayer);

        try {
            SLDTransformer transformer = new SLDTransformer();
            transformer.setEncoding(java.nio.charset.Charset.forName("UTF-8")); //$NON-NLS-1$
            transformer.setIndentation(2);

            FileOutputStream fos = new FileOutputStream(sldFile);
            transformer.transform(styledLayerDesc, fos);
            fos.close();
        } catch (TransformerException e) {
            ToolboxPlugin.log(e.getMessage());
        } catch (FileNotFoundException e) {
            ToolboxPlugin.log(e.getMessage());
        } catch (IOException e) {
            ToolboxPlugin.log(e.getMessage());
        }
    }
}
