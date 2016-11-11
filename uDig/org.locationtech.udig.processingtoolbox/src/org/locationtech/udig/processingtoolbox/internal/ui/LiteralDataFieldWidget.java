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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.geotools.data.Parameter;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.project.IMap;

/**
 * LiteralData - Field control
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LiteralDataFieldWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(LiteralDataFieldWidget.class);

    @SuppressWarnings("unused")
    private IMap map;

    public LiteralDataFieldWidget(IMap map) {
        this.map = map;
    }

    public void create(final Composite parent, final int style,
            final Map<String, Object> processParams, final Parameter<?> param,
            final Map<Widget, Map<String, Object>> uiParams) {
        composite = new Composite(parent, style);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        GridLayout layout = new GridLayout(1, false);
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        composite.setLayout(layout);

        Map<String, Object> metadata = param.metadata;
        final Combo cboField = widget.createCombo(composite, 1, false);
        uiParams.put(cboField, metadata);

        GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        cboField.setLayoutData(layoutData);
        cboField.setData(param.key);
        if (param.sample != null) {
            cboField.add(param.sample.toString());
            cboField.setText(param.sample.toString());
        }

        cboField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (cboField.getText().length() == 0) {
                    processParams.put(param.key, null);
                } else {
                    processParams.put(param.key, cboField.getText());
                }
            }
        });

        composite.pack();
    }
}
