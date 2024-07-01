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

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/**
 * Identifies the most centrally located feature in a point, line, or polygon feature class.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CentralFeatureOperation extends AbstractDistributionOperator {
    protected static final Logger LOGGER = Logging.getLogger(CentralFeatureOperation.class);

    static final String TYPE_NAME = "LinearDirectionalMean";

    private DistanceMethod distanceMethod = DistanceMethod.Euclidean;

    public DistanceMethod getDistanceMethod() {
        return distanceMethod;
    }

    public void setDistanceMethod(DistanceMethod distanceMethod) {
        this.distanceMethod = distanceMethod;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection features, String weightField,
            String potentialField, String caseField) throws IOException {
        SimpleFeatureType schema = features.getSchema();
        weightField = FeatureTypes.validateProperty(schema, weightField);
        caseField = FeatureTypes.validateProperty(schema, caseField);
        potentialField = FeatureTypes.validateProperty(schema, potentialField);

        int idxWeight = weightField == null ? -1 : schema.indexOf(weightField);
        int idxCase = caseField == null ? -1 : schema.indexOf(caseField);
        int idxPot = potentialField == null ? -1 : schema.indexOf(potentialField);
        Expression weightExpr = ff.property(weightField);
        Expression potentialExpr = ff.property(potentialField);

        CentralFeatureVisitor visitor = new CentralFeatureVisitor();
        visitor.setDistanceMethod(distanceMethod);

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
                    weightVal = this.getValue(feature, weightExpr, 0.0);
                }

                double potVal = 0.0;
                if (idxPot != -1) {
                    potVal = this.getValue(feature, potentialExpr, potVal);
                }

                visitor.visit(coordinate, caseVal, weightVal, potVal);
            }
        } finally {
            featureIter.close();
        }

        String the_geom = schema.getGeometryDescriptor().getLocalName();
        SimpleFeatureType featureType = FeatureTypes.build(schema, TYPE_NAME);
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        @SuppressWarnings("unchecked")
        HashMap<Object, CentralFeature> resultMap = visitor.getResult();
        Iterator<Object> iter = resultMap.keySet().iterator();
        try {
            while (iter.hasNext()) {
                Object caseVal = iter.next();
                CentralFeature cf = resultMap.get(caseVal);
                Point cenPt = cf.getCentralEvent();

                Filter filter = null;
                Filter intersects = ff.intersects(ff.property(the_geom), ff.literal(cenPt));

                if (idxCase == -1) {
                    filter = intersects;
                } else {
                    Filter equalFilter = ff.equals(ff.property(caseField), ff.literal(caseVal));
                    filter = ff.and(equalFilter, intersects);
                }

                featureIter = features.subCollection(filter).features();
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();

                    SimpleFeature newFeature = featureWriter.buildFeature();
                    newFeature.setAttributes(feature.getAttributes());
                    featureWriter.write(newFeature);
                }
                featureIter.close();
            }
        } catch (IOException e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }
}
