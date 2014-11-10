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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.geotools.util.logging.Logging;

/**
 * Abstract viewer control
 * 
 * @author MapPlus
 * 
 */
public abstract class AbstractViewer {
    protected static final Logger LOGGER = Logging.getLogger(AbstractViewer.class);

    protected final String EMPTY = ""; //$NON-NLS-1$
    
    protected final String DOT3 = " ? "; //$NON-NLS-1$
    
    protected WidgetBuilder widget = WidgetBuilder.newInstance();

    protected final Color warningColor = new Color(Display.getCurrent(), 255, 255, 200);

    protected Composite composite;

    public Control getControl() {
        return this.composite;
    }
}
