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

import java.util.logging.Logger;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.internal.ui.GeometryPickerDialog;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.ui.CRSChooserDialog;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Convert a geometry to a feature layer
 * 
 * @author MapPlus
 */
public class GeometryToFeaturesDialog extends AbstractGeoProcessingDialog {
    protected static final Logger LOGGER = Logging.getLogger(GeometryToFeaturesDialog.class);

    private final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

    private final Color warningColor = new Color(Display.getCurrent(), 255, 255, 200);

    private Text txtGeometry, txtCrs;

    private CoordinateReferenceSystem crs = null;

    public GeometryToFeaturesDialog(Shell parentShell, IMap map) {
        super(parentShell, map);

        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);

        this.windowTitle = Messages.GeometryToFeaturesDialog_title;
        this.windowDesc = Messages.GeometryToFeaturesDialog_description;
        this.windowSize = new Point(650, 400);
    }

    @SuppressWarnings("nls")
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.BORDER);
        container.setLayout(new GridLayout(3, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        // 1. Well known text geometry
        uiBuilder.createLabel(container, Messages.GeometryToFeaturesDialog_WKT, null, 3);

        // 2. wkt text & help button
        txtGeometry = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gridData.heightHint = 32;
        txtGeometry.setLayoutData(gridData);

        final Color oldBackColor = txtGeometry.getBackground();
        txtGeometry.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Geometry validGeometry = null;
                if (txtGeometry.getText().length() > 0) {
                    WKTReader reader = new WKTReader(geometryFactory);
                    try {
                        validGeometry = reader.read(txtGeometry.getText());
                    } catch (ParseException e1) {
                        ToolboxPlugin.log(e1.getMessage());
                    }
                }
                Color backColor = validGeometry != null ? oldBackColor : warningColor;
                txtGeometry.setBackground(backColor);
            }
        });

        final Button btnOpen = uiBuilder.createButton(container, null, null, 1);
        Image helpImage = ToolboxPlugin.getImageDescriptor("icons/help.gif").createImage(); //$NON-NLS-1$
        btnOpen.setImage(helpImage);
        btnOpen.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                // create popup menu
                Menu popupMenu = new Menu(parent.getShell(), SWT.POP_UP);

                // 1. GeometryViewer_MapCenter = Point From Map's Center
                MenuItem mnuPoint = new MenuItem(popupMenu, SWT.PUSH);
                mnuPoint.setText(Messages.GeometryViewer_MapCenter);
                mnuPoint.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Coordinate center = map.getViewportModel().getCenter();
                        txtGeometry.setText("POINT(" + center.x + " " + center.y + ")");
                        updateCRS(map.getViewportModel().getCRS());
                    }
                });

                // 2. GeometryViewer_MapExtent = Polygon From Map's Extent
                MenuItem mnuExtent = new MenuItem(popupMenu, SWT.PUSH);
                mnuExtent.setText(Messages.GeometryViewer_MapExtent);
                mnuExtent.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        ReferencedEnvelope extent = map.getViewportModel().getBounds();
                        Geometry polygon = JTS.toGeometry(extent);
                        txtGeometry.setText(polygon.toText());
                        updateCRS(map.getViewportModel().getCRS());
                    }
                });

                // 3. Extent from current map's layers
                MenuItem layerMenuItem = new MenuItem(popupMenu, SWT.CASCADE);
                layerMenuItem.setText(Messages.BoundingBoxViewer_LayerExtent);
                Menu subMenu = new Menu(parent.getShell(), SWT.DROP_DOWN);
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
                            ReferencedEnvelope extent = layer.getBounds(new NullProgressMonitor(),
                                    null);
                            Geometry polygon = JTS.toGeometry(extent);
                            txtGeometry.setText(polygon.toText());
                            updateCRS(layer.getCRS());
                        }
                    });
                }

                // 4. GeometryViewer_GeometryFromFeatures = Geometry From Layers...
                MenuItem mnuFeature = new MenuItem(popupMenu, SWT.PUSH);
                mnuFeature.setText(Messages.GeometryViewer_GeometryFromFeatures);
                mnuFeature.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        // Geometry picker dialog
                        GeometryPickerDialog dialog = new GeometryPickerDialog(parent.getShell(),
                                map);
                        dialog.setBlockOnOpen(true);
                        if (dialog.open() == Window.OK) {
                            txtGeometry.setText(dialog.getWKT());
                            updateCRS(dialog.getCRS());
                        }
                    }
                });

                // location of popup menu
                Point loc = btnOpen.getLocation();
                Rectangle rect = btnOpen.getBounds();

                Point pos = new Point(loc.x - 1, loc.y + rect.height);
                popupMenu.setLocation(parent.getShell().getDisplay()
                        .map(btnOpen.getParent(), null, pos));
                popupMenu.setVisible(true);
            }
        });

        // 3. epsg
        uiBuilder.createLabel(container, Messages.GeometryToFeaturesDialog_CRS, null, 3);
        txtCrs = uiBuilder.createText(container, null, 2);

        final Button btnCRS = uiBuilder.createButton(container, null, null, 1);
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

        // 4. output location
        // locationView = new OutputDataWidget(FileDataType.SHAPEFILE, SWT.SAVE);
        // locationView.create(container, SWT.BORDER, 3, 1);

        area.pack(true);
        return area;
    }

    private void updateCRS(CoordinateReferenceSystem selectedCrs) {
        try {
            crs = selectedCrs;
            if (selectedCrs != null) {
                txtCrs.setText(CRS.lookupIdentifier(selectedCrs, true));
            }
        } catch (FactoryException e) {
            txtCrs.setText(EMPTY);
        }
    }

    @Override
    protected void okPressed() {
        if (txtGeometry.getText().length() == 0) {
            openInformation(getShell(), Messages.FormatConversionDialog_Warning);
            return;
        }

        // default crs = map's crs
        if (crs == null) {
            crs = map.getViewportModel().getCRS();
        }

        // add geometry to map layer
        Runnable runnable = new Runnable() {
            @Override
            @SuppressWarnings({})
            public void run() {
                WKTReader reader = new WKTReader(geometryFactory);
                try {
                    Geometry validGeometry = reader.read(txtGeometry.getText());
                    validGeometry.setUserData(crs);
                    MapUtils.addGeometryToMap(map, validGeometry, validGeometry.getGeometryType());
                } catch (ParseException e1) {
                    ToolboxPlugin.log(e1.getMessage());
                }
            }
        };

        try {
            BusyIndicator.showWhile(Display.getCurrent(), runnable);
            this.close();
        } catch (Exception e) {
            ToolboxPlugin.log(e);
        }
    }
}
