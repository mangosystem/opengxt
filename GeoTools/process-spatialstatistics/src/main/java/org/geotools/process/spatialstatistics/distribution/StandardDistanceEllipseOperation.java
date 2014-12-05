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
 * Creates standard deviational ellipses to summarize the spatial characteristics of geographic features: central tendency, dispersion, and
 * directional trends.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StandardDistanceEllipseOperation extends AbstractDisributionOperator {
    protected static final Logger LOGGER = Logging
            .getLogger(StandardDistanceEllipseOperation.class);

    String[] FIELDS = { "CenterX", "CenterY", "XStdDist", "YStdDist", "Rotation" };

    /**
     * StdDeviation = 1, 2, 3 standard deviation
     */
    double stdDeviation = 1.0;

    public double getStdDeviation() {
        return stdDeviation;
    }

    public void setStdDeviation(double stdDeviation) {
        this.stdDeviation = stdDeviation;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, String weightField,
            String caseField) throws IOException {
        SimpleFeatureType schema = features.getSchema();
        weightField = FeatureTypes.validateProperty(schema, weightField);
        caseField = FeatureTypes.validateProperty(schema, caseField);

        int idxCase = caseField == null ? -1 : schema.indexOf(caseField);
        int idxWeight = weightField == null ? -1 : schema.indexOf(weightField);
        Expression weightExpr = ff.property(weightField);

        StandardDistanceEllipseVisitor visitor = new StandardDistanceEllipseVisitor();
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = features.features();
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }

                // geometry's true centroid
                Coordinate coordinate = getTrueCentroid(geometry);

                // #### Case Field ####
                Object caseVal = idxCase == -1 ? ALL : feature.getAttribute(idxCase);

                // #### Weight Field ####
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

        SimpleFeatureType featureType = FeatureTypes.getDefaultType(this.getOutputTypeName(),
                geomName, Polygon.class, crs);
        for (String field : FIELDS) {
            featureType = FeatureTypes.add(featureType, field, Double.class, 38);
        }

        if (idxCase != -1) {
            featureType = FeatureTypes.add(featureType, schema.getDescriptor(caseField));
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        @SuppressWarnings("unchecked")
        HashMap<Object, StandardDistanceEllipse> resultMap = visitor.getResult();
        Iterator<Object> iter = resultMap.keySet().iterator();
        try {
            while (iter.hasNext()) {
                Object caseVal = iter.next();
                StandardDistanceEllipse curSd = resultMap.get(caseVal);

                final Geometry ellipseCircle = curSd.calculateSDE(stdDeviation);
                if (ellipseCircle != null) {
                    final Point cenPoint = curSd.getMeanCenter();

                    SimpleFeature newFeature = featureWriter.buildFeature(null);
                    newFeature.setDefaultGeometry(ellipseCircle);

                    // FIELDS = {"CenterX", "CenterY", "XStdDist", "YStdDist", "Rotation"};
                    // create feature and set geometry
                    newFeature.setAttribute(FIELDS[0], cenPoint.getX());
                    newFeature.setAttribute(FIELDS[1], cenPoint.getY());

                    newFeature.setAttribute(FIELDS[2], curSd.seX);
                    newFeature.setAttribute(FIELDS[3], curSd.seY);
                    newFeature.setAttribute(FIELDS[4], curSd.radianRotation2);

                    if (idxCase != -1) {
                        newFeature.setAttribute(caseField, caseVal);
                    }

                    featureWriter.write(newFeature);
                }
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

}
