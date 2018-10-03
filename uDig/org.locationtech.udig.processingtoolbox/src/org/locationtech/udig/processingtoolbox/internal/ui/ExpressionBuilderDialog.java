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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.processingtoolbox.internal.Messages;
import org.locationtech.udig.processingtoolbox.styler.MapUtils;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.parameter.Parameter;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Expression Builder Dialog
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class ExpressionBuilderDialog extends Dialog {
    protected static final Logger LOGGER = Logging.getLogger(ExpressionBuilderDialog.class);

    private final String space = " ";

    private IMap map = null;

    private String initSQL = null;

    private ILayer layer = null;

    private SimpleFeatureCollection features = null;

    private String geom_field = null;

    private Table fieldTable, valueTable;

    private Combo cboLayer;

    private Text txtExpression;

    private boolean isFeatureLayer;

    public ExpressionBuilderDialog(Shell parentShell, SimpleFeatureCollection features) {
        super(parentShell);

        this.features = features;
        this.isFeatureLayer = true;
        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);
    }

    public ExpressionBuilderDialog(Shell parentShell, SimpleFeatureCollection features,
            String initSQL) {
        this(parentShell, features);
        this.initSQL = initSQL;
    }

    public ExpressionBuilderDialog(Shell parentShell, IMap map) {
        super(parentShell);

        this.map = map;
        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
                | SWT.RESIZE);
    }

    public ExpressionBuilderDialog(Shell parentShell, IMap map, String initSQL) {
        this(parentShell, map);
        this.initSQL = initSQL;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);

        newShell.setText(Messages.ExpressionBuilderDialog_title);
    }

    public String getSelectedValues() {
        return this.initSQL;
    }

    /**
     * Create contents of the button bar.
     * 
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // Clear, Test, Save..., Load..., OK, Cancel
        Button btnClear = createButton(parent, 2000, Messages.ExpressionBuilderDialog_Clear, false);
        Button btnTest = createButton(parent, 2001, Messages.ExpressionBuilderDialog_Test, false);
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

        btnClear.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                txtExpression.setText("");
            }
        });

        btnTest.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    if (isFeatureLayer) {
                        Expression expression = ECQL.toExpression(txtExpression.getText());
                        Object evaluated = expression.evaluate(features.features().next());
                        String msg = "Evaluated value: " + evaluated;
                        MessageDialog.openInformation(getParentShell(),
                                Messages.ExpressionBuilderDialog_Test, msg);
                    } else {
                        MessageDialog.openInformation(getParentShell(), Messages.QueryDialog_Test,
                                "Ratser layer not yet available!");
                    }
                } catch (CQLException e) {
                    MessageDialog.openInformation(getParentShell(),
                            Messages.ExpressionBuilderDialog_Test, e.getLocalizedMessage());
                }
            }
        });
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return ToolboxPlugin.rescaleSize(getShell(), 650, 500);
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
        int scale = (int) parent.getShell().getDisplay().getDPI().x / 96;

        // ========================================================
        // 1. layer
        // ========================================================
        Group grpLayer = widget.createGroup(container,
                Messages.ExpressionBuilderDialog_Layer_Functions, false, 1);
        if (map != null) {
            Group grpCombo = new Group(grpLayer, SWT.SHADOW_ETCHED_IN);
            grpCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
            grpCombo.setLayout(new GridLayout(2, false));
            widget.createLabel(grpCombo, Messages.ExpressionBuilderDialog_Layer,
                    Messages.ExpressionBuilderDialog_Layer, 1);

            cboLayer = widget.createCombo(grpCombo, 1, true);
            for (ILayer layer : map.getMapLayers()) {
                if (layer.hasResource(FeatureSource.class)
                        || layer.hasResource(GridCoverage2D.class)
                        || layer.getGeoResource().canResolve(GridCoverageReader.class)) {
                    cboLayer.add(layer.getName());
                }
            }

            cboLayer.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    if (cboLayer.getSelectionIndex() == -1) {
                        return;
                    }

                    layer = MapUtils.getLayer(map, cboLayer.getText());
                    isFeatureLayer = MapUtils.isFeatureLayer(layer);
                    if (isFeatureLayer) {
                        features = MapUtils.getFeatures(map, cboLayer.getText());
                    }
                    updateFields();
                }
            });
        }

        // ========================================================
        // 1. fields
        // ========================================================
        final int defaultWidth = 300;
        Group grpFields = widget.createGroup(grpLayer, Messages.ExpressionBuilderDialog_Fields,
                false, 1);
        GridData gridDataField = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1);
        gridDataField.widthHint = defaultWidth * scale;
        grpFields.setLayoutData(gridDataField);
        grpFields.setLayout(new GridLayout(1, true));

        fieldTable = widget.createListTable(grpFields,
                new String[] { Messages.ExpressionBuilderDialog_Fields }, 1);
        if (features != null) {
            updateFields();
        }
        fieldTable.getColumns()[0].setWidth(gridDataField.widthHint - 40);

        // double click event
        fieldTable.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                String selection = fieldTable.getSelection()[0].getText();
                updateExpression(selection);
            }
        });

        // ========================================================
        // 2. filter functions
        // http://docs.geotools.org/latest/userguide/library/main/filter.html
        // ========================================================
        Group grpValues = widget.createGroup(grpLayer, Messages.ExpressionBuilderDialog_Functions,
                false, 1);
        grpValues.setLayout(new GridLayout(1, true));

        valueTable = widget.createListTable(grpValues,
                new String[] { Messages.ExpressionBuilderDialog_Functions }, 1);
        updateFunctions();
        valueTable.getColumns()[0].setWidth(340 * scale);
        grpValues.setText(Messages.ExpressionBuilderDialog_Functions + "("
                + valueTable.getItemCount() + ")");

        // double click event
        valueTable.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (valueTable.getSelectionCount() > 0) {
                    String selection = valueTable.getSelection()[0].getText();
                    if (selection.contains("[geom]")) {
                        selection = selection.replace("[geom]", "[" + geom_field + "]");
                    }
                    updateExpression(selection);
                }
            }
        });

        // ========================================================
        // 3. Operators
        // ========================================================
        Group grpOpr = new Group(container, SWT.SHADOW_ETCHED_IN);
        grpOpr.setText(Messages.ExpressionBuilderDialog_Operators);
        grpOpr.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        grpOpr.setLayout(new GridLayout(16, true));

        // create button
        GridData btnLayout = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        final String[] oprs = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "+",
                "-", "*", "/", "(", ")" };
        final List<String> exceptions = Arrays.asList(new String[] { "+", "-", "*", "/" });
        Button[] btnOp = new Button[oprs.length];
        for (int idx = 0; idx < oprs.length; idx++) {
            btnOp[idx] = widget.createButton(grpOpr, oprs[idx], oprs[idx], btnLayout);
            btnOp[idx].addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    Button current = (Button) event.widget;
                    if (exceptions.contains(current.getText())) {
                        updateExpression(space + current.getText() + space);
                    } else {
                        updateExpression(current.getText());
                    }
                }
            });
        }

        // ========================================================
        // 4. Expression
        // ========================================================
        Group grpSql = new Group(container, SWT.SHADOW_ETCHED_IN);
        grpSql.setText(Messages.ExpressionBuilderDialog_Expression);
        grpSql.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        grpSql.setLayout(new GridLayout(1, true));

        txtExpression = new Text(grpSql, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData txtGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        txtGridData.heightHint = 50;
        txtExpression.setLayoutData(txtGridData);

        txtExpression.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                initSQL = txtExpression.getText();
            }
        });

        if (initSQL != null && initSQL.length() > 0) {
            txtExpression.setText(initSQL);
        }

        if (features == null && cboLayer.getItemCount() > 0) {
            cboLayer.select(0);
        }

        area.pack();

        return area;
    }

    private void updateFields() {
        fieldTable.removeAll();
        if (isFeatureLayer && features != null) {
            for (AttributeDescriptor descriptor : features.getSchema().getAttributeDescriptors()) {
                if (descriptor instanceof GeometryDescriptor) {
                    this.geom_field = descriptor.getLocalName();
                    TableItem item = new TableItem(fieldTable, SWT.NULL);
                    item.setText("[" + descriptor.getLocalName() + "]");
                    FontData fontData = item.getFont().getFontData()[0];
                    fontData.setStyle(SWT.BOLD);
                    item.setFont(new Font(item.getFont().getDevice(), fontData));
                } else {
                    TableItem item = new TableItem(fieldTable, SWT.NULL);
                    item.setText("[" + descriptor.getLocalName() + "]");
                }
            }
        } else {
            TableItem item = new TableItem(fieldTable, SWT.NULL);
            item.setText("[Value]");
        }
    }

    private void updateExpression(String insert) {
        String val = txtExpression.getText();
        if (val.length() == 0) {
            txtExpression.setText(insert);
            txtExpression.setSelection(txtExpression.getText().length());
        } else {
            if (txtExpression.getSelectionCount() == 0) {
                final int pos = txtExpression.getCaretPosition();
                String sql = val.substring(0, pos) + insert + val.substring(pos);
                txtExpression.setText(sql);
                txtExpression.setSelection(pos + insert.length() + 1);
            } else {
                final Point pos = txtExpression.getSelection();
                String sql = val.substring(0, pos.x) + insert + val.substring(pos.y);
                txtExpression.setText(sql);
                txtExpression.setSelection(pos.x + insert.length());
            }
        }
        txtExpression.setFocus();
    }

    private void updateFunctions() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Set<FunctionFactory> functionFactories = CommonFactoryFinder
                        .getFunctionFactories(null);
                for (FunctionFactory factory : functionFactories) {
                    String factoryName = factory.toString();
                    if (factoryName
                            .contains("org.geotools.process.function.ProcessFunctionFactory")) {
                        continue;
                    }

                    List<FunctionName> functionNames = factory.getFunctionNames();
                    ArrayList<FunctionName> sorted = new ArrayList<FunctionName>(functionNames);
                    Collections.sort(sorted, new Comparator<FunctionName>() {
                        @Override
                        public int compare(FunctionName o1, FunctionName o2) {
                            if (o1 == null && o2 == null) {
                                return 0;
                            } else if (o1 == null && o2 != null) {
                                return 1;
                            } else if (o1 != null && o2 == null) {
                                return -1;
                            } else {
                                return o1.getName().compareTo(o2.getName());
                            }
                        }
                    });

                    // add table
                    final String regex = "^[A-Z].*";
                    for (FunctionName functionName : sorted) {
                        if (functionName.getName().matches(regex)) {
                            continue;
                        }
                        TableItem item = new TableItem(valueTable, SWT.NULL);
                        int i = 0;
                        StringBuffer buffer = new StringBuffer(functionName.getName() + "( ");
                        for (Parameter<?> argument : functionName.getArguments()) {
                            if (i++ > 0) {
                                buffer.append(", ");
                            }
                            if (Geometry.class.isAssignableFrom(argument.getType())) {
                                buffer.append("[geom]");
                            } else {
                                buffer.append(argument.getName());
                            }
                        }
                        buffer.append(" )");
                        item.setText(buffer.toString());
                    }
                }
            }
        };

        try {
            BusyIndicator.showWhile(Display.getCurrent(), runnable);
        } catch (Exception e) {
            ToolboxPlugin.log(e);
        }
    }

    final class FunctionFilter extends ViewerFilter {
        private String searchString;

        public void setSearchText(String s) {
            this.searchString = ".*" + s + ".*";
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (searchString == null || searchString.length() == 0) {
                return true;
            }
            if (element.toString().matches(searchString)) {
                return true;
            }
            return false;
        }
    }
}
