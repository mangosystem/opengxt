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
import org.opengis.filter.Filter;

/**
 * Filter control
 * 
 * @author MapPlus
 * 
 */
public class FilterWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(FilterWidget.class);

    private IMap map;

    public FilterWidget(IMap map) {
        this.map = map;
    }

    public void create(final Composite parent, final int style,
            final Map<String, Object> processParams, final Parameter<?> param) {
        composite = new Composite(parent, style);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

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

        final Text txtFilter = widget.createText(composite, null, 1);
        txtFilter.setData(param.key);
        if (param.sample != null) {
            txtFilter.setText(param.sample.toString());
        }

        final Color oldBackColor = txtFilter.getBackground();
        txtFilter.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (txtFilter.getText().length() == 0) {
                    txtFilter.setBackground(oldBackColor);
                } else {
                    try {
                        Filter filter = ECQL.toFilter(txtFilter.getText());
                        processParams.put(param.key, filter);
                        txtFilter.setBackground(oldBackColor);
                    } catch (CQLException e1) {
                        processParams.put(param.key, Filter.INCLUDE);
                        txtFilter.setBackground(warningColor);
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
                QueryBuilderDialog dialog = new QueryBuilderDialog(parent.getShell(), map, txtFilter.getText());
                dialog.setBlockOnOpen(true);
                if (dialog.open() == Window.OK) {
                    txtFilter.setText(dialog.getFilter());
                }
            }
        });

        composite.pack();
    }
}
