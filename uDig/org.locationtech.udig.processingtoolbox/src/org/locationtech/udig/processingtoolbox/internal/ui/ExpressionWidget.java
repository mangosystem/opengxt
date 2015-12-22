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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.Parameter;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.locationtech.udig.project.IMap;
import org.opengis.filter.expression.Expression;

/**
 * Expression control
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ExpressionWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(ExpressionWidget.class);

    private IMap map;

    public ExpressionWidget(IMap map) {
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
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);

        final Text txtExpression = widget.createText(composite, null, 1);
        txtExpression.setData(param.key);
        if (param.sample != null) {
            txtExpression.setText(param.sample.toString());
        }

        final Color oldBackColor = txtExpression.getBackground();
        txtExpression.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (txtExpression.getText().length() == 0) {
                    txtExpression.setBackground(oldBackColor);
                } else {
                    try {
                        Expression expression = ECQL.toExpression(txtExpression.getText());
                        processParams.put(param.key, expression);
                        txtExpression.setBackground(oldBackColor);
                    } catch (CQLException e1) {
                        processParams.put(param.key, null);
                        txtExpression.setBackground(warningColor);
                    }
                }
            }
        });

        Button btnOpen = widget.createButton(composite, null, null, 1);
        Image helpImage = ToolboxPlugin.getImageDescriptor("icons/help.gif").createImage(); //$NON-NLS-1$
        btnOpen.setImage(helpImage);
        btnOpen.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                ExpressionBuilderDialog dialog = new ExpressionBuilderDialog(parent.getShell(),
                        map, txtExpression.getText());
                dialog.setBlockOnOpen(true);
                if (dialog.open() == Window.OK) {
                    txtExpression.setText(dialog.getSelectedValues());
                }
            }
        });

        composite.pack();
    }
}
