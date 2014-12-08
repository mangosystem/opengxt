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

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.FeatureSource;
import org.geotools.data.Parameter;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.process.spatialstatistics.core.FormatUtils;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Number Data control
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class NumberDataWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(NumberDataWidget.class);

    private IMap map;

    private Text txtNumber;

    public NumberDataWidget(IMap map) {
        this.map = map;
    }

    public void create(final Composite parent, final int style,
            final Map<String, Object> processParams, final Parameter<?> param) {
        composite = new Composite(parent, style);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

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

        txtNumber = new Text(composite, SWT.RIGHT | SWT.BORDER);
        txtNumber.setData(param.key);
        txtNumber.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        if (param.sample != null) {
            txtNumber.setText(param.sample.toString());
        }

        final Color oldBackColor = txtNumber.getBackground();
        txtNumber.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Object obj = Converters.convert(txtNumber.getText(), param.type);
                if (obj == null) {
                    txtNumber.setBackground(warningColor);
                } else {
                    processParams.put(param.key, obj);
                    txtNumber.setBackground(oldBackColor);
                }
            }
        });

        final Button btnOpen = widget.createButton(composite, null, null, 1);
        Image helpImage = ToolboxPlugin.getImageDescriptor("icons/help.gif").createImage(); //$NON-NLS-1$
        btnOpen.setImage(helpImage);
        btnOpen.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                // create popup menu
                Shell shell = parent.getShell();
                Menu popupMenu = new Menu(shell, SWT.POP_UP);

                // 1. Get Area
                MenuItem areaMenuItem = new MenuItem(popupMenu, SWT.CASCADE);
                areaMenuItem.setText(Messages.NumberDataViewer_GetArea);
                Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
                areaMenuItem.setMenu(subMenu);

                int layerCount = 0;
                for (ILayer layer : map.getMapLayers()) {
                    if (layer.hasResource(FeatureSource.class)) {
                        GeometryDescriptor descriptor = layer.getSchema().getGeometryDescriptor();
                        Class<?> geometryBinding = descriptor.getType().getBinding();
                        // only polygon layers
                        if (geometryBinding.isAssignableFrom(Polygon.class)
                                || geometryBinding.isAssignableFrom(MultiPolygon.class)) {
                            MenuItem mnuLayer = new MenuItem(subMenu, SWT.PUSH);
                            mnuLayer.setText(layer.getName());
                            mnuLayer.setData(layer);
                            mnuLayer.addSelectionListener(selectionListener);
                            layerCount++;
                        }
                    }
                }

                // location of popup menu
                if (layerCount > 0) {
                    Control ctrl = (Control) event.widget;
                    Point loc = ctrl.getLocation();
                    Rectangle rect = ctrl.getBounds();

                    Point pos = new Point(loc.x - 1, loc.y + rect.height);
                    popupMenu.setLocation(shell.getDisplay().map(ctrl.getParent(), null, pos));
                    popupMenu.setVisible(true);
                }
            }
        });

        composite.pack();
    }

    SelectionListener selectionListener = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
            try {
                ILayer layer = (ILayer) event.widget.getData();
                SimpleFeatureSource source = (SimpleFeatureSource) layer.getResource(
                        FeatureSource.class, new NullProgressMonitor());

                double area = 0.0;
                SimpleFeatureIterator featureIter = null;
                try {
                    featureIter = source.getFeatures(Query.ALL).features();
                    while (featureIter.hasNext()) {
                        SimpleFeature feature = featureIter.next();
                        Geometry geometry = (Geometry) feature.getDefaultGeometry();
                        if (geometry == null || geometry.isEmpty()) {
                            continue;
                        }
                        area += geometry.getArea();
                    }
                    txtNumber.setText(FormatUtils.format(area));
                } finally {
                    featureIter.close();
                }
            } catch (IOException e) {
                ToolboxPlugin.log(e.getLocalizedMessage());
            }
        }
    };
}
