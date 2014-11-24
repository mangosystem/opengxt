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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.Parameter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.common.FormatUtils;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * BoundingBox control
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class BoundingBoxWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(BoundingBoxWidget.class);

    private IMap map;

    private Text txtExtent;

    public BoundingBoxWidget(IMap map) {
        this.map = map;
    }

    public void create(final Composite parent, final int style,
            final Map<String, Object> processParams, final Parameter<?> param) {
        composite = new Composite(parent, style);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        GridLayout layout = new GridLayout(4, false);
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        composite.setLayout(layout);

        txtExtent = widget.createText(composite, null, 3, false);
        final Color oldBackColor = txtExtent.getBackground();
        txtExtent.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                // xmin, ymin, xmax, ymax, epsg
                String[] splits = txtExtent.getText().split(","); //$NON-NLS-1$
                if (splits.length != 5) {
                    txtExtent.setBackground(warningColor);
                    return;
                }

                try {
                    Double xmin = Converters.convert(splits[0].trim(), Double.class);
                    Double ymin = Converters.convert(splits[1].trim(), Double.class);
                    Double xmax = Converters.convert(splits[2].trim(), Double.class);
                    Double ymax = Converters.convert(splits[3].trim(), Double.class);

                    CoordinateReferenceSystem crs = CRS.decode(splits[4].trim());
                    ReferencedEnvelope extent = new ReferencedEnvelope(xmin, xmax, ymin, ymax, crs);
                    processParams.put(param.key, extent);
                    txtExtent.setBackground(oldBackColor);
                } catch (NoSuchAuthorityCodeException e1) {
                    txtExtent.setBackground(warningColor);
                    ToolboxPlugin.log(e1.getLocalizedMessage());
                } catch (FactoryException e1) {
                    txtExtent.setBackground(warningColor);
                    ToolboxPlugin.log(e1.getLocalizedMessage());
                } catch (Exception e1) {
                    txtExtent.setBackground(warningColor);
                    ToolboxPlugin.log(e1.getLocalizedMessage());
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

                // 1. Extent from current map
                MenuItem mapMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                mapMenuItem.setText(Messages.BoundingBoxViewer_CurrentMapExtent);
                mapMenuItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        updateBoundingBox(map.getViewportModel().getBounds());
                    }
                });

                // 2. Extent from current map' full extent
                MenuItem fullExtentMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                fullExtentMenuItem.setText(Messages.BoundingBoxViewer_CurrentMapFullExtent);
                fullExtentMenuItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        updateBoundingBox(map.getBounds(new NullProgressMonitor()));
                    }
                });

                // 3. Extent from current map's layers
                MenuItem layerMenuItem = new MenuItem(popupMenu, SWT.CASCADE);
                layerMenuItem.setText(Messages.BoundingBoxViewer_LayerExtent);
                Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
                layerMenuItem.setMenu(subMenu);

                for (ILayer layer : map.getMapLayers()) {
                    MenuItem mnuLayer = new MenuItem(subMenu, SWT.PUSH);
                    mnuLayer.setText(layer.getName());
                    mnuLayer.setData(layer);
                    mnuLayer.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent event) {
                            ILayer layer = (ILayer) event.widget.getData();
                            updateBoundingBox(layer.getBounds(new NullProgressMonitor(), null));
                        }
                    });
                }

                // 4. location of popup menu
                Control ctrl = (Control) event.widget;
                Point loc = ctrl.getLocation();
                Rectangle rect = ctrl.getBounds();

                Point pos = new Point(loc.x - 1, loc.y + rect.height);
                popupMenu.setLocation(shell.getDisplay().map(ctrl.getParent(), null, pos));
                popupMenu.setVisible(true);
            }
        });

        // default extent
        if (param.sample == null) {
            // updateBoundingBox(map.getViewportModel().getBounds());
        } else if (param.sample != null
                && param.sample.getClass().isAssignableFrom(ReferencedEnvelope.class)) {
            updateBoundingBox((ReferencedEnvelope) param.sample);
        }

        composite.pack();
    }

    private void updateBoundingBox(ReferencedEnvelope extent) {
        final CoordinateReferenceSystem crs = extent.getCoordinateReferenceSystem();

        String epsgCode = null;
        if (crs != null) {
            try {
                epsgCode = CRS.lookupIdentifier(crs, true);
            } catch (FactoryException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }

        StringBuffer sb = new StringBuffer();
        sb.append(FormatUtils.format(extent.getMinX(), 9)).append(", "); //$NON-NLS-1$
        sb.append(FormatUtils.format(extent.getMinY(), 9)).append(", "); //$NON-NLS-1$
        sb.append(FormatUtils.format(extent.getMaxX(), 9)).append(", "); //$NON-NLS-1$
        sb.append(FormatUtils.format(extent.getMaxY(), 9));

        if (epsgCode == null) {
            txtExtent.setText(sb.toString());
        } else {
            txtExtent.setText(sb.toString() + ", " + epsgCode); //$NON-NLS-1$
        }
    }
}
