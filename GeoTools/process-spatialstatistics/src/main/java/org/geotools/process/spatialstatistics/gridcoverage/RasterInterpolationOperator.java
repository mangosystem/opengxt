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
package org.geotools.process.spatialstatistics.gridcoverage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.Geometry;

/**
 * Abstract Interpolation Operator
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class RasterInterpolationOperator extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(RasterInterpolationOperator.class);

    public enum RadiusType {
        Variable, Fixed
    }

    /**
     * Extract the input observation points
     * 
     * @param pointFeatures features to be extracted
     * @param valueField weight field
     * @return coordinate array
     */
    protected Coordinate[] extractPoints(SimpleFeatureCollection pointFeatures, String valueField) {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        final Expression valueExp = ff.property(valueField);

        List<Coordinate> coordinates = new ArrayList<Coordinate>();
        SimpleFeatureIterator featureIter = pointFeatures.features();
        try {
            while (featureIter.hasNext()) {
                final SimpleFeature feature = featureIter.next();
                Double objVal = valueExp.evaluate(feature, Double.class);

                // skip null or nan value
                if (objVal == null || Double.isNaN(objVal)) {
                    continue;
                } else {
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    final Coordinate obs = geometry.getCoordinate();
                    obs.z = objVal.doubleValue();
                    coordinates.add(obs);
                }
            }
        } finally {
            featureIter.close();
        }

        return CoordinateArrays.toCoordinateArray(coordinates);
    }

}
