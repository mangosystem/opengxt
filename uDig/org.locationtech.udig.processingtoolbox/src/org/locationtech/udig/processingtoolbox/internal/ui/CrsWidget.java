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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.Parameter;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.ui.CRSChooserDialog;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Coordinate Reference System control
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class CrsWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(CrsWidget.class);

    private IMap map;

    private Text txtCrs;

    public CrsWidget(IMap map) {
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

        txtCrs = widget.createText(composite, null, 1);
        txtCrs.setData(param.key);
        if (param.sample != null) {
            txtCrs.setText(param.sample.toString());
        }

        final Color oldBackColor = txtCrs.getBackground();
        txtCrs.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                try {
                    final String epsgCode = txtCrs.getText();
                    if (epsgCode != null && epsgCode.length() > 0) {
                        CoordinateReferenceSystem crs = CRS.decode(epsgCode);
                        if (crs != null) {
                            processParams.put(param.key, crs);
                            txtCrs.setBackground(oldBackColor);
                        } else {
                            txtCrs.setBackground(warningColor);
                        }
                    } else {
                        txtCrs.setBackground(oldBackColor);
                    }
                } catch (NoSuchAuthorityCodeException e1) {
                    LOGGER.log(Level.FINER, e1.getMessage(), e1);
                    processParams.put(param.key, null);
                    txtCrs.setBackground(warningColor);
                } catch (FactoryException e1) {
                    LOGGER.log(Level.FINER, e1.getMessage(), e1);
                    processParams.put(param.key, null);
                    txtCrs.setBackground(warningColor);
                }
            }
        });

        Button btnOpen = widget.createButton(composite, null, null, 1);
        Image helpImage = ToolboxPlugin.getImageDescriptor("icons/help.gif").createImage(); //$NON-NLS-1$
        btnOpen.setImage(helpImage);
        btnOpen.addSelectionListener(new SelectionAdapter() {
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
                        try {
                            if (selectedCrs != null) {
                                txtCrs.setText(CRS.lookupIdentifier(selectedCrs, true));
                                processParams.put(param.key, selectedCrs);
                            }
                        } catch (FactoryException e) {
                            txtCrs.setText(EMPTY);
                        }
                    }
                });

                // 2. CRS from layers
                MenuItem layerMenuItem = new MenuItem(popupMenu, SWT.CASCADE);
                layerMenuItem.setText(Messages.CrsViewer_LayerCRS);
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
                            CoordinateReferenceSystem selectedCrs = layer.getCRS();
                            try {
                                if (selectedCrs != null) {
                                    txtCrs.setText(CRS.lookupIdentifier(selectedCrs, true));
                                    processParams.put(param.key, selectedCrs);
                                }
                            } catch (FactoryException e) {
                                txtCrs.setText(EMPTY);
                            }
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
                        if (param.key != null) {
                            crs = (CoordinateReferenceSystem) processParams.get(param.key);
                        }

                        CRSChooserDialog dialog = new CRSChooserDialog(parent.getShell(), crs);
                        if (dialog.open() == Window.OK) {
                            CoordinateReferenceSystem selectedCrs = dialog.getResult();
                            try {
                                if (selectedCrs != null) {
                                    txtCrs.setText(CRS.lookupIdentifier(selectedCrs, true));
                                    processParams.put(param.key, selectedCrs);
                                }
                            } catch (FactoryException e) {
                                txtCrs.setText(EMPTY);
                            }
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

        composite.pack();
    }
}
