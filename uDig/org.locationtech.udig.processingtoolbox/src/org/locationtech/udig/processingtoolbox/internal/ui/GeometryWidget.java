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

import java.util.Map;
import java.util.logging.Logger;

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
import org.locationtech.udig.project.IMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Geometry control
 * 
 * @author MapPlus
 * 
 */
public class GeometryWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(GeometryWidget.class);

    private GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

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
                    WKTReader reader = new WKTReader(geometryFactory);
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

                // 1. GeometryViewer_MapCenter = Point From Map's Center
                MenuItem mnuPoint = new MenuItem(popupMenu, SWT.PUSH);
                mnuPoint.setText(Messages.GeometryViewer_MapCenter);
                mnuPoint.addSelectionListener(new SelectionAdapter() {
                    @SuppressWarnings("nls")
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Coordinate center = map.getViewportModel().getCenter();
                        txtGeometry.setText("POINT(" + center.x + " " + center.y + ")");
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
                    }
                });

                // 3. GeometryViewer_GeometryFromFeatures = Geometry From Layers...
                MenuItem mnuFeature = new MenuItem(popupMenu, SWT.PUSH);
                mnuFeature.setText(Messages.GeometryViewer_GeometryFromFeatures);
                mnuFeature.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        // Geometry picker dialog
                        GeometryPickerDialog dialog = new GeometryPickerDialog(parent.getShell(), map);
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
                popupMenu.setLocation(parent.getShell().getDisplay().map(btnOpen.getParent(), null, pos));
                popupMenu.setVisible(true);
            }
        });

        composite.pack();
    }
}
