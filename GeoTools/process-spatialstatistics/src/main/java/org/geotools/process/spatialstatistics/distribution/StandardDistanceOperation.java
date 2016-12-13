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
import com.vividsolutions.jts.geom.Polygon;

/**
 * Measures the degree to which features are concentrated or dispersed around the geometric mean center.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StandardDistanceOperation extends AbstractDisributionOperator {
    protected static final Logger LOGGER = Logging.getLogger(StandardDistanceOperation.class);

    static final String TYPE_NAME = "StandardDistance";

    final String[] FIELDS = { "CenterX", "CenterY", "StdDist" };

    /**
     * StdDeviation = 1, 2, 3 standard deviation
     */
    double stdDeviation = 1.0;

    public double getStdDeviation() {
        return stdDeviation;
    }

    public void setStdDeviation(double stdDeviation) {
        if (stdDeviation > 3) {
            stdDeviation = 3;
        }
        this.stdDeviation = stdDeviation;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, String weightField,
            String caseField) throws IOException {
        SimpleFeatureType schema = features.getSchema();
        weightField = FeatureTypes.validateProperty(schema, weightField);
        caseField = FeatureTypes.validateProperty(schema, caseField);

        int idxWeight = weightField == null ? -1 : schema.indexOf(weightField);
        int idxCase = caseField == null ? -1 : schema.indexOf(caseField);
        Expression weightExpr = ff.property(weightField);

        StandardDistanceVisitor visitor = new StandardDistanceVisitor();
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

                visitor.visit(coordinate, caseVal, weightVal);
            }
        } finally {
            featureIter.close();
        }

        // build feature collection
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        String geomName = schema.getGeometryDescriptor().getLocalName();

        SimpleFeatureType featureType = FeatureTypes.getDefaultType(TYPE_NAME, geomName,
                Polygon.class, crs);
        for (String field : FIELDS) {
            featureType = FeatureTypes.add(featureType, field, Double.class, 38);
        }

        if (idxCase != -1) {
            featureType = FeatureTypes.add(featureType, schema.getDescriptor(caseField));
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        @SuppressWarnings("unchecked")
        HashMap<Object, StandardDistance> resultMap = visitor.getResult();
        Iterator<Object> iter = resultMap.keySet().iterator();
        try {
            while (iter.hasNext()) {
                Object caseVal = iter.next();
                StandardDistance curSd = resultMap.get(caseVal);

                Point cenPoint = curSd.getMeanCenter();
                double stdDist = curSd.getStdDist(this.stdDeviation);
                Geometry sdCircle = cenPoint.buffer(stdDist, 90, 1);

                // create feature and set geometry
                SimpleFeature newFeature = featureWriter.buildFeature();
                newFeature.setDefaultGeometry(sdCircle);
                newFeature.setAttribute(FIELDS[0], cenPoint.getX());
                newFeature.setAttribute(FIELDS[1], cenPoint.getY());

                // standard distance
                newFeature.setAttribute(FIELDS[2], stdDist);

                if (idxCase != -1) {
                    newFeature.setAttribute(caseField, caseVal);
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
