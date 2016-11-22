/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics.core;

import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory2;

/**
 * SpatialWeightMatrix
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractSpatialWeightMatrix {
    protected static final Logger LOGGER = Logging.getLogger(AbstractSpatialWeightMatrix.class);
    
    public enum SpatialWeightMatrixType {
        Distance, Contiguity
    }

    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
    
    protected boolean selfContains = false;

    public boolean isSelfContains() {
        return selfContains;
    }

    public void setSelfContains(boolean selfContains) {
        this.selfContains = selfContains;
    }

    public abstract SpatialWeightMatrixResult execute(SimpleFeatureCollection features,
            String uniqueField);

}
