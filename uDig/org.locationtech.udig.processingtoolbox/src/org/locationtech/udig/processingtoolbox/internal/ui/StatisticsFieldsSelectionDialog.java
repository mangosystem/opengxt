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

import java.util.logging.Logger;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

/**
 * Statistics Fields Selection Dialog
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class StatisticsFieldsSelectionDialog extends Dialog {
    protected static final Logger LOGGER = Logging.getLogger(StatisticsFieldsSelectionDialog.class);

    private IMap map = null;

    private SimpleFeatureCollection source;
    
    private String summaryFields, targetFields;

    private Combo cboLayer, cboField, cboType;

    private Button btnAdd, btnDelete;

    private Table sumFields;
    
    public StatisticsFieldsSelectionDialog(Shell parentShell, IMap map) {
        super(parentShell);

        this.map = map;
        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);

        newShell.setText(Messages.StatisticsFieldsSelectionDialog_title);
    }

    public String getSelectedValues() {
        return this.summaryFields;
    }

    public String getTargetValues() {
        return this.targetFields;
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
        widget.createLabel(grpMap, Messages.StatisticsFieldsSelectionDialog_Layer, null, 1);
        cboLayer = widget.createCombo(grpMap, 1, true);
        for (ILayer layer : map.getMapLayers()) {
            if (layer.hasResource(FeatureSource.class)) {
                cboLayer.add(layer.getName());
            }
        }
        cboLayer.addModifyListener(modifyListener);
        
        // 1. FeatureType's Schema
        // statistics
        Group grpTable = widget.createGroup(container, Messages.StatisticsFieldsSelectionDialog_Fields, false, 2);
        grpTable.setLayout(new GridLayout(4, false));

        cboField = widget.createCombo(grpTable, 1);
        cboField.addModifyListener(modifyListener);

        cboType = widget.createCombo(grpTable, 1);

        btnAdd = widget.createButton(grpTable, Messages.StatisticsFieldsSelectionDialog_Add, null, 1);
        btnAdd.addSelectionListener(selectionListener);
        btnDelete = widget.createButton(grpTable, Messages.StatisticsFieldsSelectionDialog_Delete, null, 1);
        btnDelete.addSelectionListener(selectionListener);

        sumFields = widget.createEditableTable(grpTable, new String[] {
                Messages.StatisticsFieldsSelectionDialog_TargetField,
                Messages.StatisticsFieldsSelectionDialog_StatisticsType }, 4, 1);

        cboLayer.select(0);

        area.pack();

        return area;
    }

    private boolean invalidWidgetValue(Widget... widgets) {
        for (Widget widget : widgets) {
            if (widget instanceof Combo && ((Combo) widget).getText().isEmpty()) {
                return true;
            } else if (widget instanceof Text && ((Text) widget).getText().isEmpty()) {
                return true;
            } else if (widget instanceof Table && ((Table) widget).getItemCount() == 0) {
                return true;
            }
        }
        return false;
    }
    
    ModifyListener modifyListener = new ModifyListener() {
        @SuppressWarnings("nls")
        @Override
        public void modifyText(ModifyEvent e) {
            Widget widget = e.widget;
            if (invalidWidgetValue(widget)) {
                return;
            }

            if (widget.equals(cboLayer)) {
                cboField.removeAll();
                source = MapUtils.getFeatures(map, cboLayer.getText());
                SimpleFeatureType schema = source.getSchema();
                for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
                    if (descriptor instanceof GeometryDescriptor) {
                        continue;
                    } else {
                        cboField.add(descriptor.getLocalName());
                    }
                }
            } else if (widget.equals(cboField)) {
                if (FeatureTypes.isNumeric(source.getSchema(), cboField.getText())) {
                    cboType.setItems(new String[] { "sum", "mean", "minimum", "maximum", "range",
                            "standard deviation", "variance", "count" });
                } else {
                    cboType.setItems(new String[] { "first", "last" });
                }
            }
        }
    };

    SelectionListener selectionListener = new SelectionAdapter() {
        @SuppressWarnings("nls")
        @Override
        public void widgetSelected(SelectionEvent event) {
            Widget widget = event.widget;
            if (widget.equals(btnAdd)) {
                // Function.PropertyName
                if (cboField.getSelectionIndex() != -1 && cboType.getSelectionIndex() != -1) {
                    final String property = cboField.getText();
                    final String function = cboType.getText();
                    final String dataKey = buildField(property, function, ".");

                    if (existItem(sumFields, dataKey)) {
                        MessageDialog.openInformation(getShell(), getShell().getText(), Messages.StatisticsFieldsSelectionDialog_WarningDuplicate);
                        return;
                    }

                    TableItem item = new TableItem(sumFields, SWT.NONE);
                    item.setData(dataKey);
                    item.setText(new String[] { buildField(property, function, "_"), function });
                }
                updateFields();
            } else if (widget.equals(btnDelete)) {
                for (int index = sumFields.getItems().length - 1; index >= 0; index--) {
                    TableItem item = sumFields.getItems()[index];
                    if (item.getChecked()) {
                        sumFields.remove(index);
                    }
                }
                updateFields();
            }
        }
    };
    
    private void updateFields() {
        StringBuffer summaryBuffer = new StringBuffer();
        StringBuffer targetBuffer = new StringBuffer();
        for (int index = 0; index < sumFields.getItems().length; index++) {
            TableItem item = sumFields.getItem(index);
            if (summaryBuffer.length() > 0) {
                summaryBuffer.append(",").append(item.getData().toString()); //$NON-NLS-1$
                targetBuffer.append(",").append(item.getText().toString()); //$NON-NLS-1$
            } else {
                summaryBuffer.append(item.getData().toString());
                targetBuffer.append(item.getText().toString());
            }
        }

        // [함수명.필드명], [함수명.필드명]
        summaryFields = summaryBuffer.toString();

        // [함수명_필드명], [함수명_필드명]
        targetFields = targetBuffer.toString();
    }
    
    private boolean existItem(Table table, String dataKey) {
        for (TableItem item : table.getItems()) {
            if (item.getData().toString().equalsIgnoreCase(dataKey)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("nls")
    private String buildField(String propertyName, String functionName, String splitter) {
        String function = "cnt"; // Function_PropertyName

        functionName = functionName.toLowerCase();
        if (functionName.startsWith("sum")) {
            function = "sum";
        } else if (functionName.startsWith("mean")) {
            function = "avg";
        } else if (functionName.startsWith("ave")) {
            function = "avg";
        } else if (functionName.startsWith("min")) {
            function = "min";
        } else if (functionName.startsWith("max")) {
            function = "max";
        } else if (functionName.startsWith("r")) {
            function = "rng";
        } else if (functionName.startsWith("st")) {
            function = "std";
        } else if (functionName.startsWith("v")) {
            function = "var";
        } else if (functionName.startsWith("c")) {
            function = "cnt";
        } else if (functionName.startsWith("f")) {
            function = "fst";
        } else if (functionName.startsWith("l")) {
            function = "lst";
        }

        // [함수명.필드명]
        return function + splitter + propertyName;
    }
    
}
