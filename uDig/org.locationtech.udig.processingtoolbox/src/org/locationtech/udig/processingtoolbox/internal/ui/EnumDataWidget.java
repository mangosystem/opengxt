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
import org.geotools.data.Parameter;
import org.geotools.util.logging.Logging;

/**
 * Enum Data control
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
public class EnumDataWidget extends AbstractToolboxWidget {
    protected static final Logger LOGGER = Logging.getLogger(EnumDataWidget.class);

    public EnumDataWidget() {
    }

    public void create(final Composite parent, final int style,
            final Map<String, Object> processParams, final Parameter<?> param) {
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

        final Combo cboEnum = new Combo(composite, SWT.NONE | SWT.DROP_DOWN | SWT.READ_ONLY);
        cboEnum.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        for (Object enumVal : param.type.getEnumConstants()) {
            cboEnum.add(enumVal.toString());
        }

        cboEnum.setData(param.key);
        if (param.sample != null) {
            cboEnum.setText(param.sample.toString());
        }

        cboEnum.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                for (Object enumVal : param.type.getEnumConstants()) {
                    if (enumVal.toString().equalsIgnoreCase(cboEnum.getText())) {
                        processParams.put(param.key, enumVal);
                        break;
                    }
                }
            }
        });

        composite.pack();
    }
}
