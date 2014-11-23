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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.geotools.data.Parameter;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;

/**
 * Integer Data control
 * 
 * @author MapPlus
 * 
 */
public class IntegerDataWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(IntegerDataWidget.class);

    public IntegerDataWidget() {
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

        final Spinner spinner = new Spinner(composite, SWT.LEFT_TO_RIGHT | SWT.BORDER);
        spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        spinner.setData(param.key);
        spinner.setValues(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, 10);
        if (param.sample != null) {
            spinner.setSelection((Integer) param.sample);
        }

        final Color oldBackColor = spinner.getBackground();
        spinner.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Object obj = Converters.convert(spinner.getSelection(), param.type);
                if (obj == null) {
                    spinner.setBackground(warningColor);
                } else {
                    processParams.put(param.key, obj);
                    spinner.setBackground(oldBackColor);
                }
            }
        });

        composite.pack();
    }
}
