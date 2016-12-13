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
package org.geotools.process.spatialstatistics.pattern;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.SpatialEvent;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Calculates a Standardized Score of Dissimilarity.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StandardizedScoresOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(StandardizedScoresOperation.class);

    private int count = 0;

    private double X = 0.0;

    private double Y = 0.0;

    private List<SpatialEvent> events = new ArrayList<SpatialEvent>();

    private void preCalculate(SimpleFeatureCollection features, Expression xExpression,
            Expression yExpression) {
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = geometry.getCentroid().getCoordinate();

                Double xVal = xExpression.evaluate(feature, Double.class);
                Double yVal = yExpression.evaluate(feature, Double.class);
                if (xVal == null || yVal == null) {
                    continue;
                }

                count += 1;
                X += xVal;
                Y += yVal;

                events.add(new SpatialEvent(feature.getID(), coordinate, xVal, yVal));
            }
        } finally {
            featureIter.close();
        }
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, String xField,
            String yField, String targetField) throws IOException {
        xField = FeatureTypes.validateProperty(features.getSchema(), xField);
        yField = FeatureTypes.validateProperty(features.getSchema(), yField);

        Expression xExpression = ff.property(xField);
        Expression yExpression = ff.property(yField);
        return execute(features, xExpression, yExpression, targetField);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features,
            Expression xExpression, Expression yExpression, String targetField) throws IOException {
        SimpleFeatureType schema = FeatureTypes
                .add(features.getSchema(), targetField, Double.class);

        // 1. pre calculation
        preCalculate(features, xExpression, yExpression);

        // 2. calculate standardized scores
        IFeatureInserter featureWriter = getFeatureWriter(schema);
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Double xVal = xExpression.evaluate(feature, Double.class);
                Double yVal = yExpression.evaluate(feature, Double.class);

                Double stdscr = null;
                if (xVal != null && yVal != null) {
                    double XX = xVal / X;
                    double YY = yVal / Y;

                    double dZSum = 0.0d;
                    for (SpatialEvent curE : events) {
                        double xx = curE.xVal / X;
                        double yy = curE.yVal / Y;
                        double dZ = Math.pow(xx - yy, 2) / count;
                        dZSum += dZ;
                    }
                    stdscr = (XX - YY) / Math.sqrt(dZSum);
                }

                // create & insert feature
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);
                newFeature.setAttribute(targetField, stdscr);
                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }
}
