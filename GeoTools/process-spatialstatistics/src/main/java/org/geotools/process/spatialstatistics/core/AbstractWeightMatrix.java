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
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;

/**
 * SpatialWeightMatrix
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class AbstractWeightMatrix {
    protected static final Logger LOGGER = Logging.getLogger(AbstractWeightMatrix.class);

    public enum SpatialWeightMatrixType {
        Distance, Contiguity
    }

    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private StandardizationMethod standardizationMethod = StandardizationMethod.None;

    private boolean selfNeighbors = false;

    protected boolean uniqueFieldIsFID = false;

    public boolean isSelfNeighbors() {
        return selfNeighbors;
    }

    public void setSelfNeighbors(boolean selfNeighbors) {
        this.selfNeighbors = selfNeighbors;
    }

    public StandardizationMethod getStandardizationMethod() {
        return standardizationMethod;
    }

    public void setStandardizationMethod(StandardizationMethod standardizationMethod) {
        this.standardizationMethod = standardizationMethod;
    }

    public abstract WeightMatrix execute(SimpleFeatureCollection features,
            String uniqueField);

    protected Object getFeatureID(SimpleFeature feature, String uniqueField) {
        if (uniqueFieldIsFID || uniqueField == null) {
            return feature.getID();
        }
        return feature.getAttribute(uniqueField);
    }
}
