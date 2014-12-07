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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.logging.Logger;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Geometry Picker Dialog
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class GeometryPickerDialog extends Dialog {
    protected static final Logger LOGGER = Logging.getLogger(GeometryPickerDialog.class);
    
    private IMap map = null;

    private GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        
    private SimpleFeature feature;
    
    private String wktGeometry;

    private Table featureTable;

    private Button btnClipboard, btnAddLayer;

    private Combo cboLayer;

    private Text txtGeometry;

    public GeometryPickerDialog(Shell parentShell, IMap map) {
        super(parentShell);

        this.map = map;
        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);

        newShell.setText(Messages.GeometryPickerDialog_title);
    }

    public String getWKT() {
        return this.wktGeometry;
    }

    @Override
    protected Point getInitialSize() {
        return new Point(650, 450);
    }

    /**
     * Create contents of the dialog.
     * 
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = layout.marginHeight = layout.marginRight = layout.marginBottom = 2;
        area.setLayout(layout);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        WidgetBuilder widget = WidgetBuilder.newInstance();
        
        // 1. Layer Selection
        Group grpMap = new Group(container, SWT.SHADOW_ETCHED_IN);
        grpMap.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        grpMap.setLayout(new GridLayout(2, false));
        widget.createLabel(grpMap, Messages.GeometryPickerDialog_Layer, null, 1);
        cboLayer = widget.createCombo(grpMap, 1, true);
        for (ILayer layer : map.getMapLayers()) {
            if (layer.hasResource(FeatureSource.class)) {
                cboLayer.add(layer.getName());
            }
        }
        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                SimpleFeatureCollection source = MapUtils.getFeatures(map, cboLayer.getText());
                updateFeatures(source);
            }
        });
        
        // 1. Features
        Group grpLayer = widget.createGroup(container, null, false, 1);
        
        Group grpFeatures = widget.createGroup(grpLayer, Messages.GeometryPickerDialog_Feature, false, 1);
        grpFeatures.setLayout(new GridLayout(1, true));
        featureTable = widget.createListTable(grpFeatures, new String[] { Messages.GeometryPickerDialog_Feature }, 1);        
        featureTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (featureTable.getSelectionCount() > 0) {
                    feature = (SimpleFeature) featureTable.getSelection()[0].getData();
                    txtGeometry.setText(((Geometry)feature.getDefaultGeometry()).toText());
                } else {
                    txtGeometry.setText(""); //$NON-NLS-1$
                }
            }
        });

        // ========================================================
        // 2. WKT & Operations
        // ========================================================
        Group grpValues = widget.createGroup(grpLayer, Messages.GeometryPickerDialog_WKT, false, 1);
        grpValues.setLayout(new GridLayout(2, true));

        txtGeometry = new Text(grpValues, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gridData.heightHint = 200;
        txtGeometry.setLayoutData(gridData);

        final Color warningColor = new Color(Display.getCurrent(), 255, 255, 200);
        final Color oldBackColor = txtGeometry.getBackground();
        txtGeometry.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Geometry validGeometry = null;
                if (txtGeometry.getText().length() > 0) {
                    WKTReader reader = new WKTReader(geometryFactory);
                    try {
                        validGeometry = reader.read(txtGeometry.getText());
                        wktGeometry = txtGeometry.getText();
                    } catch (ParseException e1) {
                        ToolboxPlugin.log(e1.getMessage());
                    }
                }

                btnClipboard.setEnabled(validGeometry != null);
                btnAddLayer.setEnabled(validGeometry != null);
                Color backColor = validGeometry != null ? oldBackColor : warningColor;
                txtGeometry.setBackground(backColor);
            }
        });

        GridData btnLayout = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        btnClipboard = widget.createButton(grpValues, Messages.GeometryPickerDialog_Clipboard, null, btnLayout);
        btnClipboard.setEnabled(false);
        btnClipboard.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                // copy to clipboard
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(txtGeometry.getText()), null );
            }
        });

        btnAddLayer = widget.createButton(grpValues, Messages.GeometryPickerDialog_AddLayer, null, btnLayout);
        btnAddLayer.setEnabled(false);
        btnAddLayer.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                // add geometry to map layer
                Runnable runnable = new Runnable() {
                    @Override
                    @SuppressWarnings({})
                    public void run() {
                        String typeName = featureTable.getSelection()[0].getText();
                        MapUtils.addFeatureToMap(map, feature, typeName);
                    }
                };

                try {
                    BusyIndicator.showWhile(Display.getCurrent(), runnable);
                } catch (Exception e) {
                    ToolboxPlugin.log(e);
                }
            }
        });

        area.pack();
        resizeTableColumn();

        return area;
    }

    private void resizeTableColumn() {
        featureTable.setRedraw(false);
        for (TableColumn column : featureTable.getColumns()) {
            column.setWidth(featureTable.getSize().x - (2 * featureTable.getBorderWidth()));
        }
        featureTable.setRedraw(true);
    }

    private void updateFeatures(final SimpleFeatureCollection source) {
        featureTable.removeAll();
        Runnable runnable = new Runnable() {
            @Override
            @SuppressWarnings({ })
            public void run() {
                SimpleFeatureIterator featureIter = null;
                try {
                    featureIter = source.features();
                    while (featureIter.hasNext()) {
                        SimpleFeature feature = featureIter.next();
                        TableItem item = new TableItem(featureTable, SWT.NULL);
                        item.setText(feature.getID());
                        item.setData(feature);
                    }
                } finally {
                    featureIter.close();
                }
            }
        };

        try {
            BusyIndicator.showWhile(Display.getCurrent(), runnable);
        } catch (Exception e) {
            ToolboxPlugin.log(e);
        }
    }

}
