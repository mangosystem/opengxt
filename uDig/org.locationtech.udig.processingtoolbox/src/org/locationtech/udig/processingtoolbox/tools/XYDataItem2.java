/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import org.jfree.data.xy.XYDataItem;
import org.opengis.feature.simple.SimpleFeature;

/**
 * XYDataItem extends JFreeChart
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
class XYDataItem2 extends XYDataItem {
    private static final long serialVersionUID = 2567264473390904488L;

    private SimpleFeature feature;

    public XYDataItem2(SimpleFeature feature, double x, double y) {
        super(x, y);
        this.feature = feature;
    }

    public SimpleFeature getFeature() {
        return this.feature;
    }
}
