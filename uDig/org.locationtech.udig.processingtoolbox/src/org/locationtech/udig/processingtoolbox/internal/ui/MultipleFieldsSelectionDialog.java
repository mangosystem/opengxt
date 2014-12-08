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

import java.util.logging.Logger;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

/**
 * Multiple Fields Selection Dialog
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class MultipleFieldsSelectionDialog extends Dialog {
    protected static final Logger LOGGER = Logging.getLogger(MultipleFieldsSelectionDialog.class);

    private IMap map = null;

    private String selectedFields;

    private Table schemaTable;

    private Button btnAll, btnSwitch, btnClear;

    private Combo cboLayer;

    private Text txtFields;

    public MultipleFieldsSelectionDialog(Shell parentShell, IMap map) {
        super(parentShell);

        this.map = map;
        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);

        newShell.setText(Messages.MultipleFieldsSelectionDialog_title);
    }

    public String getSelectedValues() {
        return this.selectedFields;
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
        widget.createLabel(grpMap, Messages.MultipleFieldsSelectionDialog_Layer, null, 1);
        cboLayer = widget.createCombo(grpMap, 1, true);
        for (ILayer layer : map.getMapLayers()) {
            if (layer.hasResource(FeatureSource.class)) {
                cboLayer.add(layer.getName());
            }
        }
        cboLayer.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                schemaTable.removeAll();
                SimpleFeatureCollection source = MapUtils.getFeatures(map, cboLayer.getText());
                SimpleFeatureType schema = source.getSchema();
                for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
                    if (descriptor instanceof GeometryDescriptor) {
                        continue;
                    } else {
                        TableItem item = new TableItem(schemaTable, SWT.NULL);
                        item.setText(descriptor.getLocalName());
                    }
                }
            }
        });

        // 1. FeatureType's Schema
        Group grpLayer = widget.createGroup(container, null, false, 1);

        Group grpFeatures = widget.createGroup(grpLayer,
                Messages.MultipleFieldsSelectionDialog_Fields, false, 1);
        grpFeatures.setLayout(new GridLayout(1, true));
        GridData grdList = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        grdList.widthHint = 200;
        grpFeatures.setLayoutData(grdList);

        schemaTable = widget.createTable(grpFeatures, new String[] { "name" }, 1); //$NON-NLS-1$
        schemaTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                updateFields();
            }
        });

        // ========================================================
        // 2. Selected Fields
        // ========================================================
        Group grpValues = widget.createGroup(grpLayer, Messages.MultipleFieldsSelectionDialog_SelectedFields, false, 1);
        grpValues.setLayout(new GridLayout(3, true));

        txtFields = new Text(grpValues, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
        gridData.heightHint = 200;
        txtFields.setLayoutData(gridData);

        txtFields.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                selectedFields = txtFields.getText();
            }
        });

        GridData btnLayout = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        btnAll = widget.createButton(grpValues, Messages.MultipleFieldsSelectionDialog_SelectAll,
                null, btnLayout);
        btnAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                for (TableItem item : schemaTable.getItems()) {
                    item.setChecked(true);
                }
                updateFields();
            }
        });

        btnSwitch = widget.createButton(grpValues,
                Messages.MultipleFieldsSelectionDialog_SwitchSelect, null, btnLayout);
        btnSwitch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                for (TableItem item : schemaTable.getItems()) {
                    item.setChecked(! item.getChecked());
                }
                updateFields();
            }
        });

        btnClear = widget.createButton(grpValues, Messages.MultipleFieldsSelectionDialog_Clear,
                null, btnLayout);
        btnClear.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                for (TableItem item : schemaTable.getItems()) {
                    item.setChecked(false);
                }
                updateFields();
            }
        });

        cboLayer.select(0);

        area.pack();
        resizeTableColumn();

        return area;
    }

    private void resizeTableColumn() {
        schemaTable.setRedraw(false);
        for (TableColumn column : schemaTable.getColumns()) {
            column.setWidth(schemaTable.getSize().x - (2 * schemaTable.getBorderWidth()));
        }
        schemaTable.setRedraw(true);
    }
    
    private void updateFields() {
        StringBuffer buffer = new StringBuffer();
        for (TableItem item : schemaTable.getItems()) {
            if (item.getChecked()) {
                if (buffer.length() > 0) {
                    buffer.append(",").append(item.getText()); //$NON-NLS-1$
                } else {
                    buffer.append(item.getText());
                }
            }
        }
        txtFields.setText(buffer.toString());
    }
}
