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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.geotools.data.Parameter;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.internal.Messages;

/**
 * Boolean Data control
 * 
 * @author MapPlus
 * 
 */
public class BooleanDataWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(BooleanDataWidget.class);

    public BooleanDataWidget() {
    }

    public void create(final Composite parent, final int style,
            final Map<String, Object> processParams, final Parameter<?> param) {
        composite = new Composite(parent, style);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

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

        final Combo cboBoolean = new Combo(composite, SWT.NONE | SWT.DROP_DOWN | SWT.READ_ONLY);
        cboBoolean.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        cboBoolean.setData(param.key);

        cboBoolean.add(Messages.ProcessExecutionDialog_Yes);
        cboBoolean.add(Messages.ProcessExecutionDialog_No);

        if (param.sample != null) {
            cboBoolean.select((Boolean) param.sample == Boolean.TRUE ? 0 : 1);
        }
        cboBoolean.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Boolean value = cboBoolean.getSelectionIndex() == 0 ? Boolean.TRUE : Boolean.FALSE;
                processParams.put(param.key, value);
            }
        });

        composite.pack();
    }
}
