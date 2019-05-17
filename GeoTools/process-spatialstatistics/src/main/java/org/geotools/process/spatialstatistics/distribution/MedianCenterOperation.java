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
package org.geotools.process.spatialstatistics.distribution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Identifies the location that minimizes overall Euclidean distance to the features in a dataset.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MedianCenterOperation extends AbstractDistributionOperator {
    protected static final Logger LOGGER = Logging.getLogger(MedianCenterOperation.class);

    static final String TYPE_NAME = "MedianCenter";

    final String[] FIELDS = { "XCoord", "YCoord" };

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, String weightField,
            String caseField, String[] attFields) throws IOException {
        SimpleFeatureType schema = features.getSchema();

        weightField = FeatureTypes.validateProperty(schema, weightField);
        caseField = FeatureTypes.validateProperty(schema, caseField);
        if (attFields != null) {
            for (int k = 0; k < attFields.length; k++) {
                attFields[k] = FeatureTypes.validateProperty(schema, attFields[k]);
            }
        }

        int idxWeight = weightField == null ? -1 : schema.indexOf(weightField);
        int idxCase = caseField == null ? -1 : schema.indexOf(caseField);
        Expression weightExpr = ff.property(weightField);

        int[] idxAtts = null;
        if (attFields != null) {
            idxAtts = new int[attFields.length];
            for (int k = 0; k < attFields.length; k++) {
                idxAtts[k] = schema.indexOf(attFields[k]);
            }
        }

        MedianCenterVisitor visitor = new MedianCenterVisitor();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                Coordinate coordinate = getTrueCentroid(geometry);
                Object caseVal = idxCase == -1 ? ALL : feature.getAttribute(idxCase);

                double weightVal = 1.0;
                if (idxWeight != -1) {
                    weightVal = this.getValue(feature, weightExpr, weightVal);
                }

                // Attribute Fields
                Number[] attVals = null;
                if (attFields != null) {
                    attVals = new Number[attFields.length];
                    for (int index = 0; index < attFields.length; index++) {
                        try {
                            attVals[index] = (Number) feature.getAttribute(idxAtts[index]);
                        } catch (NumberFormatException nfe) {
                            LOGGER.log(Level.WARNING, nfe.getMessage(), nfe);
                            attVals[index] = 0.0;
                        }
                    }
                }

                visitor.visit(coordinate, caseVal, weightVal, attVals);
            }
        } finally {
            featureIter.close();
        }

        // build feature collection
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        String geomName = schema.getGeometryDescriptor().getLocalName();

        SimpleFeatureType featureType = FeatureTypes.getDefaultType(TYPE_NAME, geomName,
                Point.class, crs);
        featureType = FeatureTypes.add(featureType, FIELDS[0], Double.class, 38);
        featureType = FeatureTypes.add(featureType, FIELDS[1], Double.class, 38);

        if (idxCase != -1) {
            featureType = FeatureTypes.add(featureType, schema.getDescriptor(caseField));
        }

        if (attFields != null) {
            for (int k = 0; k < attFields.length; k++) {
                featureType = FeatureTypes.add(featureType, schema.getDescriptor(attFields[k]));
            }
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        @SuppressWarnings("unchecked")
        HashMap<Object, MedianCenter> resultMap = visitor.getResult();
        Iterator<Object> iter = resultMap.keySet().iterator();
        try {
            while (iter.hasNext()) {
                Object caseVal = iter.next();
                MedianCenter curCenter = resultMap.get(caseVal);
                Point cenPoint = curCenter.getMedianCenter();

                // Median Center
                SimpleFeature newFeature = featureWriter.buildFeature();
                newFeature.setDefaultGeometry(cenPoint);

                newFeature.setAttribute(FIELDS[0], cenPoint.getX());
                newFeature.setAttribute(FIELDS[1], cenPoint.getY());

                // Case Field
                if (idxCase != -1) {
                    newFeature.setAttribute(caseField, caseVal);
                }

                // Attribute Fields
                if (attFields != null) {
                    Number[] attVals = curCenter.getUnivariateMedian();
                    for (int k = 0; k < attFields.length; k++) {
                        Class<?> bind = featureType.getDescriptor(attFields[k]).getType()
                                .getBinding();
                        if (bind.isAssignableFrom(Short.class)) {
                            newFeature.setAttribute(attFields[k], attVals[k].shortValue());
                        } else if (bind.isAssignableFrom(Integer.class)) {
                            newFeature.setAttribute(attFields[k], attVals[k].intValue());
                        } else if (bind.isAssignableFrom(Long.class)) {
                            newFeature.setAttribute(attFields[k], attVals[k].longValue());
                        } else if (bind.isAssignableFrom(Float.class)) {
                            newFeature.setAttribute(attFields[k], attVals[k].floatValue());
                        } else if (bind.isAssignableFrom(Double.class)) {
                            newFeature.setAttribute(attFields[k], attVals[k].doubleValue());
                        } else {
                            newFeature.setAttribute(attFields[k], attVals[k]);
                        }
                    }
                }

                featureWriter.write(newFeature);
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }
}
