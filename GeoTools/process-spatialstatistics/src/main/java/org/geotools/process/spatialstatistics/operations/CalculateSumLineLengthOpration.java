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
package org.geotools.process.spatialstatistics.operations;

import java.io.IOException;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.parameter.InvalidParameterValueException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Calculate the total sum of line lengths for each feature of a polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CalculateSumLineLengthOpration extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(CalculateSumLineLengthOpration.class);

    public SimpleFeatureCollection execute(SimpleFeatureCollection polygons, String lengthField,
            SimpleFeatureCollection lines) throws IOException {
        Class<?> binding = polygons.getSchema().getGeometryDescriptor().getType().getBinding();
        if (!binding.isAssignableFrom(Polygon.class)
                && !binding.isAssignableFrom(MultiPolygon.class)) {
            throw new InvalidParameterValueException("Invalid parameters", "polygonFeatures",
                    polygons);
        }

        binding = lines.getSchema().getGeometryDescriptor().getType().getBinding();
        if (!binding.isAssignableFrom(LineString.class)
                && !binding.isAssignableFrom(MultiLineString.class)) {
            throw new InvalidParameterValueException("Invalid parameters", "lineFeatures", lines);
        }

        // prepare feature type
        SimpleFeatureType featureType = FeatureTypes.build(polygons, getOutputTypeName());
        featureType = FeatureTypes.add(featureType, lengthField, Double.class, 38);
        
        // number, string, int, long, float, double....
        AttributeDescriptor lengthDesc = featureType.getDescriptor(lengthField);
        binding = lengthDesc.getType().getBinding();
        
        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = polygons.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry clipGeometry = (Geometry) feature.getDefaultGeometry();
                if (clipGeometry == null || clipGeometry.isEmpty()) {
                    continue;
                }

                double sumLength = getLineLength(clipGeometry, lines);

                SimpleFeature newFeature = featureWriter.buildFeature(null);
                featureWriter.copyAttributes(feature, newFeature, true);
                newFeature.setAttribute(lengthField, Converters.convert(sumLength, binding));
                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }

    private double getLineLength(Geometry clipGeometry, SimpleFeatureCollection lineFeatures) {
        double sumLength = 0d;

        String the_geom = lineFeatures.getSchema().getGeometryDescriptor().getLocalName();
        Filter filter = ff.intersects(ff.property(the_geom), ff.literal(clipGeometry));

        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = lineFeatures.subCollection(filter).features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry lineStrings = (Geometry) feature.getDefaultGeometry();
                Geometry clipedGeometry = lineStrings.intersection(clipGeometry);
                if (clipedGeometry != null) {
                    sumLength += clipedGeometry.getLength();
                }
            }
        } finally {
            featureIter.close();
        }

        return sumLength;
    }

}
