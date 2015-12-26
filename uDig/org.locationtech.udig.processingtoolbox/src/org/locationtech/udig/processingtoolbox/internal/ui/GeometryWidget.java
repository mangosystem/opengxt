/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.internal.ui;

import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.Parameter;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Geometry control
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GeometryWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(GeometryWidget.class);

    private GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);

    private IMap map;

    public GeometryWidget(IMap map) {
        this.map = map;
    }

    public void create(final Composite parent, final int style,
            final Map<String, Object> processParams, final Parameter<?> param) {
        composite = new Composite(parent, style);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        GridLayout layout = new GridLayout(2, false);
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        composite.setLayout(layout);

        final Text txtGeometry = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP
                | SWT.V_SCROLL);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gridData.heightHint = 32;
        txtGeometry.setLayoutData(gridData);

        txtGeometry.setData(param.key);
        if (param.sample != null) {
            Geometry validGeometry = (Geometry) param.sample;
            txtGeometry.setText(validGeometry.toText());
        }

        final Color oldBackColor = txtGeometry.getBackground();
        txtGeometry.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Geometry validGeometry = null;
                if (txtGeometry.getText().length() > 0) {
                    WKTReader reader = new WKTReader(gf);
                    try {
                        validGeometry = reader.read(txtGeometry.getText());
                    } catch (ParseException e1) {
                        ToolboxPlugin.log(e1.getMessage());
                    }
                }
                processParams.put(param.key, validGeometry);
                Color backColor = validGeometry != null ? oldBackColor : warningColor;
                txtGeometry.setBackground(backColor);
            }
        });

        final Button btnOpen = widget.createButton(composite, null, null, 1);
        Image helpImage = ToolboxPlugin.getImageDescriptor("icons/help.gif").createImage(); //$NON-NLS-1$
        btnOpen.setImage(helpImage);
        btnOpen.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                // create popup menu
                Menu popupMenu = new Menu(parent.getShell(), SWT.POP_UP);

                // 1. Point
                // - Map's Center
                // - Layer's Center - layers......
                MenuItem mnuPoint = new MenuItem(popupMenu, SWT.CASCADE);
                mnuPoint.setText(Messages.GeometryViewer_Point);
                Menu subPointMenu = new Menu(parent.getShell(), SWT.DROP_DOWN);
                mnuPoint.setMenu(subPointMenu);

                // 1. map's center = Point From Map's Center
                MenuItem mnuPointMap = new MenuItem(subPointMenu, SWT.PUSH);
                mnuPointMap.setText(Messages.GeometryViewer_MapCenter);
                mnuPointMap.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Coordinate center = map.getViewportModel().getCenter();
                        txtGeometry.setText(gf.createPoint(center).toText());
                    }
                });

                for (ILayer layer : map.getMapLayers()) {
                    if (layer.getName() == null) {
                        continue;
                    }
                    MenuItem mnuLayer = new MenuItem(subPointMenu, SWT.PUSH);
                    mnuLayer.setText(String.format(Messages.GeometryViewer_PointLayer,
                            layer.getName()));
                    mnuLayer.setData(layer);
                    mnuLayer.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent event) {
                            ILayer layer = (ILayer) event.widget.getData();
                            ReferencedEnvelope extent = layer.getBounds(new NullProgressMonitor(),
                                    null);
                            Geometry point = gf.createPoint(extent.centre());
                            txtGeometry.setText(point.toText());
                        }
                    });
                }

                //
                // 2. LineString
                // - Map's Extent
                // - Layer's Extent - layers......
                MenuItem mnuLine = new MenuItem(popupMenu, SWT.CASCADE);
                mnuLine.setText(Messages.GeometryViewer_LineString);
                Menu subLineMenu = new Menu(parent.getShell(), SWT.DROP_DOWN);
                mnuLine.setMenu(subLineMenu);

                // 1. boundary of map's extent
                MenuItem mnuBoundaryMap = new MenuItem(subLineMenu, SWT.PUSH);
                mnuBoundaryMap.setText(Messages.GeometryViewer_MapBoundary);
                mnuBoundaryMap.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        ReferencedEnvelope extent = map.getViewportModel().getBounds();
                        txtGeometry.setText(toLineString(extent).toText());
                    }
                });

                for (ILayer layer : map.getMapLayers()) {
                    if (layer.getName() == null) {
                        continue;
                    }
                    MenuItem mnuLayer = new MenuItem(subLineMenu, SWT.PUSH);
                    mnuLayer.setText(String.format(Messages.GeometryViewer_LineStringLayer,
                            layer.getName()));
                    mnuLayer.setData(layer);
                    mnuLayer.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent event) {
                            ILayer layer = (ILayer) event.widget.getData();
                            ReferencedEnvelope extent = layer.getBounds(new NullProgressMonitor(),
                                    null);
                            txtGeometry.setText(toLineString(extent).toText());
                        }
                    });
                }

                // 3. Polygon
                // - Map's Extent
                // - Layer's Extent - layers......
                MenuItem mnuPolygon = new MenuItem(popupMenu, SWT.CASCADE);
                mnuPolygon.setText(Messages.GeometryViewer_Polygon);
                Menu subPolygonMenu = new Menu(parent.getShell(), SWT.DROP_DOWN);
                mnuPolygon.setMenu(subPolygonMenu);

                // 1. map's extent = Polygon From Map's Extent
                MenuItem mnuExtentMap = new MenuItem(subPolygonMenu, SWT.PUSH);
                mnuExtentMap.setText(Messages.GeometryViewer_MapExtent);
                mnuExtentMap.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        ReferencedEnvelope extent = map.getViewportModel().getBounds();
                        txtGeometry.setText(JTS.toGeometry(extent).toText());
                    }
                });

                for (ILayer layer : map.getMapLayers()) {
                    if (layer.getName() == null) {
                        continue;
                    }
                    MenuItem mnuLayer = new MenuItem(subPolygonMenu, SWT.PUSH);
                    mnuLayer.setText(String.format(Messages.GeometryViewer_PolygonLayer,
                            layer.getName()));
                    mnuLayer.setData(layer);
                    mnuLayer.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent event) {
                            ILayer layer = (ILayer) event.widget.getData();
                            ReferencedEnvelope extent = layer.getBounds(new NullProgressMonitor(),
                                    null);
                            txtGeometry.setText(JTS.toGeometry(extent).toText());
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

        composite.pack();
    }

    private Geometry toLineString(ReferencedEnvelope extent) {
        LinearRing linearRing = (LinearRing) JTS.toGeometry(extent).getBoundary();
        return gf.createLineString(linearRing.getCoordinateSequence());
    }
}
