/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.styler;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.styler.SSStyleBuilder;
import org.geotools.styling.Style;

/**
 * Default Vector Styler
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public final class DefaultFeatureStyler extends OutputStyler {

    protected SSStyleBuilder builder;

    public DefaultFeatureStyler(Object source) {
        super(source);
    }

    @Override
    public Style getStyle() {
        SimpleFeatureCollection features = (SimpleFeatureCollection) source;
        builder = new SSStyleBuilder(features.getSchema());
        builder.setOpacity(0.8f);
        return builder.getDefaultFeatureStyle();
    }

}