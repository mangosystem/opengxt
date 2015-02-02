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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;

/**
 * Output Styler Factory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class OutputStylerFactory {

    public static OutputStyler getStyler(Object source) {
        if (source instanceof SimpleFeatureCollection) {
            return new DefaultFeatureStyler(source);
        } else if (source instanceof GridCoverage2D) {
            return new DefaultGridCoverageStyler(source);
        } else {
            return new DefaultFeatureStyler(source);
        }
    }

    public static OutputStyler getStyler(Object source, String styleName) {
        // new KVP(Parameter.OPTIONS, "렌더러유형.필드명")
        // 렌더러유형 = LISA, Density, Distance, Interpolation
        // COType, GiZScore

        if (source instanceof SimpleFeatureCollection) {
            return new DefaultFeatureStyler(source);
        } else if (source instanceof GridCoverage2D) {
            return new DefaultGridCoverageStyler(source);
        } else {
            return new DefaultFeatureStyler(source);
        }
    }

    public static OutputStyler getStyler(Object source, String styleName, String fieldName) {
        // unique, lisa, cotype...... predefined style
        return null;
    }

    public static OutputStyler getStyler(Object source, String styleName, String fieldName,
            int numClasses) {
        return getStyler(source, styleName, fieldName, numClasses, "Blues");
    }

    public static OutputStyler getStyler(Object source, String styleName, String fieldName,
            int numClasses, String brewerPaletteName) {
        return null;
    }

}
