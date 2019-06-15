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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.spatialstatistics.gridcoverage.RasterReprojectOperation;
import org.geotools.process.spatialstatistics.storage.RasterExportOperation;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.ToolboxView;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget;
import org.locationtech.udig.processingtoolbox.internal.ui.OutputDataWidget.FileDataType;
import org.locationtech.udig.processingtoolbox.internal.ui.TableSelectionWidget;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.ui.CRSChooserDialog;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.SampleDimensionType;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Reproject the coordinate system of a set of input features to a common coordinate system.
 * 
 * @author MapPlus
 */
public class BatchReprojectRastersDialog extends AbstractGeoProcessingDialog implements
        IRunnableWithProgress {
    protected static final Logger LOGGER = Logging.getLogger(BatchReprojectRastersDialog.class);

    private Table inputTable;

    private CoordinateReferenceSystem targetCRS = null;

    private Text txtCrs;

    public BatchReprojectRastersDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);

        this.windowTitle = Messages.BatchReprojectRastersDialog_title;
        this.windowDesc = Messages.BatchReprojectRastersDialog_description;
        this.windowSize = ToolboxPlugin.rescaleSize(parentShell, 650, 500);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.BORDER);
        container.setLayout(new GridLayout(3, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group group = uiBuilder.createGroup(container,
                Messages.BatchReprojectFeaturesDialog_SelectLayers, false, 3);
        inputTable = uiBuilder.createTable(group, new String[] {
                Messages.BatchReprojectFeaturesDialog_Name,
                Messages.BatchReprojectFeaturesDialog_Type,
                Messages.BatchReprojectFeaturesDialog_CRS }, 3);

        TableSelectionWidget tblSelection = new TableSelectionWidget(inputTable);
        tblSelection.create(group, SWT.NONE, 3, 1);

        uiBuilder.createLabel(container, Messages.BatchReprojectFeaturesDialog_OutputCRS, null, 1);
        txtCrs = uiBuilder.createText(container, null, 1);

        final Button btnCRS = uiBuilder.createButton(container, null, null, 1);
        Image helpImage = ToolboxPlugin.getImageDescriptor("icons/help.gif").createImage(); //$NON-NLS-1$
        btnCRS.setImage(helpImage);
        btnCRS.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                // create popup menu
                Shell shell = parent.getShell();
                Menu popupMenu = new Menu(shell, SWT.POP_UP);

                // 1. CRS from current map
                MenuItem mapMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                mapMenuItem.setText(Messages.CrsViewer_MapCRS);
                mapMenuItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        CoordinateReferenceSystem selectedCrs = map.getViewportModel().getCRS();
                        updateCRS(selectedCrs);
                    }
                });

                // 2. CRS from layers
                MenuItem layerMenuItem = new MenuItem(popupMenu, SWT.CASCADE);
                layerMenuItem.setText(Messages.CrsViewer_LayerCRS);
                Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
                layerMenuItem.setMenu(subMenu);

                for (ILayer layer : map.getMapLayers()) {
                    if (layer.getName() == null) {
                        continue;
                    }
                    MenuItem mnuLayer = new MenuItem(subMenu, SWT.PUSH);
                    mnuLayer.setText(layer.getName());
                    mnuLayer.setData(layer);
                    mnuLayer.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent event) {
                            ILayer layer = (ILayer) event.widget.getData();
                            CoordinateReferenceSystem selectedCrs = layer.getCRS();
                            updateCRS(selectedCrs);
                        }
                    });
                }

                // 3. CRS Chooser Dialog
                MenuItem crsMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                crsMenuItem.setText(Messages.CrsViewer_CRSDialog);
                crsMenuItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        CoordinateReferenceSystem crs = null;
                        CRSChooserDialog dialog = new CRSChooserDialog(parent.getShell(), crs);
                        if (dialog.open() == Window.OK) {
                            CoordinateReferenceSystem selectedCrs = dialog.getResult();
                            updateCRS(selectedCrs);
                        }
                    }
                });

                // 4. location of popup menu
                Control ctrl = (Control) event.widget;
                Point loc = ctrl.getLocation();
                Rectangle rect = ctrl.getBounds();

                Point pos = new Point(loc.x - 1, loc.y + rect.height);
                popupMenu.setLocation(shell.getDisplay().map(ctrl.getParent(), null, pos));
                popupMenu.setVisible(true);
            }
        });

        locationView = new OutputDataWidget(FileDataType.FOLDER, SWT.OPEN);
        locationView.create(container, SWT.BORDER, 3, 1);
        locationView.setFolder(ToolboxView.getWorkspace());

        // load layers
        loadlayers(inputTable);

        area.pack(true);
        return area;
    }

    private void updateCRS(CoordinateReferenceSystem selectedCrs) {
        try {
            targetCRS = selectedCrs;
            if (selectedCrs != null) {
                txtCrs.setText(CRS.lookupIdentifier(selectedCrs, true));
            }
        } catch (FactoryException e) {
            txtCrs.setText(EMPTY);
        }
    }

    private void loadlayers(Table table) {
        table.removeAll();
        for (ILayer layer : map.getMapLayers()) {
            if (layer.getName() != null
                    && (layer.hasResource(GridCoverageReader.class) || layer.getGeoResource()
                            .canResolve(GridCoverageReader.class))) {
                TableItem item = new TableItem(table, SWT.NONE);

                GridCoverage2D coverage = MapUtils.getGridCoverage(layer);
                SampleDimension gridDim = coverage.getSampleDimension(0);
                SampleDimensionType sdType = gridDim.getSampleDimensionType();
                CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();

                item.setText(new String[] { layer.getName(), sdType.name(), crs.toString() });
                item.setData(layer);
            }
        }
    }

    @Override
    protected void okPressed() {
        if (!existCheckedItem(inputTable) || txtCrs.getText().length() == 0 || targetCRS == null) {
            openInformation(getShell(), Messages.BatchReprojectFeaturesDialog_Warning);
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

            RasterReprojectOperation reproject = new RasterReprojectOperation();
            RasterExportOperation export = new RasterExportOperation();

            for (TableItem item : inputTable.getItems()) {
                monitor.subTask(item.getText());
                if (item.getChecked()) {
                    ILayer layer = (ILayer) item.getData();
                    GridCoverage2D coverage = MapUtils.getGridCoverage(layer);

                    GridCoverage2D reprojected = null;
                    File file = new File(folder, layer.getName() + ".tif"); //$NON-NLS-1$
                    if (file.exists()) {
                        if (MessageDialog.openQuestion(getShell(), windowTitle, MessageFormat
                                .format(Messages.BatchReprojectFeaturesDialog_Overwrite,
                                        layer.getName()))) {
                            if (MapUtils.confirmSpatialFile(file)) {
                                reprojected = reproject.execute(coverage, targetCRS);
                            } else {
                                openInformation(getShell(), Messages.General_Completed);
                            }
                        }
                    } else {
                        reprojected = reproject.execute(coverage, targetCRS);
                    }

                    if (reprojected != null) {
                        export.saveAsGeoTiff(reprojected, file.getAbsolutePath());
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
