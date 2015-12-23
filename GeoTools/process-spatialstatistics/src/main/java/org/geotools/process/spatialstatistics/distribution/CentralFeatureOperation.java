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
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Identifies the most centrally located feature in a point, line, or polygon feature class.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class CentralFeatureOperation extends AbstractDisributionOperator {
    protected static final Logger LOGGER = Logging.getLogger(CentralFeatureOperation.class);

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

                // geometry's true centroid
                Coordinate coordinate = getTrueCentroid(geometry);

                // #### Case Field ####
                Object caseVal = idxCase == -1 ? ALL : feature.getAttribute(idxCase);

                // #### Weight Field ####
                double weightVal = 1.0;
                if (idxWeight != -1) {
                    weightVal = this.getValue(feature, weightExpr, 0.0);
                }

                // #### Potential Field ####
                double potVal = 0.0;
                if (idxPot != -1) {
                    potVal = this.getValue(feature, potentialExpr, potVal);
                }

                visitor.visit(coordinate, caseVal, weightVal, potVal);
            }
        } finally {
            featureIter.close();
        }

        SimpleFeatureType featureType = FeatureTypes.build(schema, getOutputTypeName());
        IFeatureInserter featureWriter = getFeatureWriter(featureType);
        String the_geom = schema.getGeometryDescriptor().getLocalName();

        @SuppressWarnings("unchecked")
        HashMap<Object, CentralFeature> resultMap = visitor.getResult();
        Iterator<Object> iter = resultMap.keySet().iterator();
        try {
            while (iter.hasNext()) {
                Object caseVal = iter.next();
                CentralFeature cf = resultMap.get(caseVal);
                Point cenPt = cf.getCentralEvent();

                Filter finalFilter = null;
                Filter filterIntersect = ff.intersects(ff.property(the_geom), ff.literal(cenPt));

                if (idxCase == -1) {
                    finalFilter = filterIntersect;
                } else {
                    Filter equalFilter = ff.equals(ff.property(caseField), ff.literal(caseVal));
                    finalFilter = ff.and(equalFilter, filterIntersect);
                }

                featureIter = features.subCollection(finalFilter).features();
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    featureWriter.write(feature);
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
