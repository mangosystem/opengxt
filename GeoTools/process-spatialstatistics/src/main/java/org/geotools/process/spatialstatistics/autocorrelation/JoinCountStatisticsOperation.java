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
package org.geotools.process.spatialstatistics.autocorrelation;

import java.io.IOException;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.enumeration.ContiguityType;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

/**
 * Spatial autocorrelation for binary attributes.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class JoinCountStatisticsOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(JoinCountStatisticsOperation.class);

    public JoinCountStatisticsOperation() {
    }

    public JoinCountResult execute(SimpleFeatureCollection features, Filter trueExpression,
            ContiguityType contiguityType) throws IOException {
        String typeName = features.getSchema().getTypeName();
        String the_geom = features.getSchema().getGeometryDescriptor().getLocalName();

        JoinCountResult joinCounts = new JoinCountResult(typeName, contiguityType);
        int featureCount = 0;

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature pFeature = featureIter.next();
                Geometry pGeometry = (Geometry) pFeature.getDefaultGeometry();
                boolean primary = trueExpression.evaluate(pFeature);
                featureCount++;

                Filter filter = ff.intersects(ff.property(the_geom), ff.literal(pGeometry));
                SimpleFeatureIterator subIter = features.subCollection(filter).features();
                try {
                    while (subIter.hasNext()) {
                        SimpleFeature sFeature = subIter.next();
                        if (pFeature.getID().equals(sFeature.getID())) {
                            continue;
                        }

                        Geometry sGeometry = (Geometry) sFeature.getDefaultGeometry();
                        Geometry intersects = pGeometry.intersection(sGeometry);
                        if (contiguityType != ContiguityType.Queen) {
                            if (intersects instanceof Point || intersects instanceof MultiPoint) {
                                if (contiguityType == ContiguityType.Rook) {
                                    continue;
                                }
                            } else {
                                if (contiguityType == ContiguityType.Bishops) {
                                    continue;
                                }
                            }
                        }

                        // evaluate
                        boolean secondary = trueExpression.evaluate(sFeature);
                        joinCounts.visit(primary, secondary);
                    }
                } finally {
                    subIter.close();
                }
            }
        } finally {
            featureIter.close();
        }

        joinCounts.setFeatureCount(featureCount);
        return joinCounts;
    }

    public static class JoinCountResult {

        private String typeName;

        private ContiguityType contiguityType = ContiguityType.Queen;

        private int featureCount = 0;

        private int BBJoins = 0;

        private int WWJoins = 0;

        private int BWJoins = 0;

        public JoinCountResult(String typeName, ContiguityType contiguityType) {
            this.typeName = typeName;
            this.contiguityType = contiguityType;
        }

        public void visit(boolean primary, boolean secondary) {
            if (primary && secondary) {
                BBJoins++;
            } else if (!primary && !secondary) {
                WWJoins++;
            } else {
                BWJoins++;
            }
        }

        public String getTypeName() {
            return typeName;
        }

        public int getFeatureCount() {
            return featureCount;
        }

        public void setFeatureCount(int featureCount) {
            this.featureCount = featureCount;
        }

        public int getBBJoins() {
            return BBJoins / 2;
        }

        public int getWWJoins() {
            return WWJoins / 2;
        }

        public int getBWJoins() {
            return BWJoins / 2;
        }

        public int getNumberOfJoins() {
            return getBBJoins() + getWWJoins() + getBWJoins();
        }

        public ContiguityType getContiguityType() {
            return contiguityType;
        }

        public void setContiguityType(ContiguityType contiguityType) {
            this.contiguityType = contiguityType;
        }

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");

            // 1. The total number of areas,
            // 2. The total number of black areas,
            // 3. The total number of white areas,
            // 4. The observed number of BB, BW and WW joins,
            // 5. The expected number of BB, BW, and WW joins,
            // 6. The variance of BB, BW joins,
            // 7. The z-statistics of BB, BW joins.

            StringBuffer sb = new StringBuffer();
            sb.append("Type Name: ").append(getTypeName()).append(separator);
            sb.append("Number of Features: ").append(getFeatureCount()).append(separator);
            sb.append("Contiguity Type: ").append(getContiguityType().toString()).append(separator);
            sb.append("Number of Joins: ").append(getNumberOfJoins()).append(separator);

            sb.append("The observed number of BB, BW and WW joins").append(separator);
            sb.append("BB Joins: ").append(getBBJoins()).append(separator);
            sb.append("WW Joins: ").append(getWWJoins()).append(separator);
            sb.append("BW Joins: ").append(getBWJoins()).append(separator);

            sb.append("The expected number of BB, BW and WW joins").append(separator);
            sb.append("BB Joins: ").append(getBBJoins()).append(separator);
            sb.append("WW Joins: ").append(getWWJoins()).append(separator);
            sb.append("BW Joins: ").append(getBWJoins()).append(separator);

            sb.append("The variance of BB, BW joins").append(separator);
            sb.append("BB Joins: ").append(getBBJoins()).append(separator);
            sb.append("WW Joins: ").append(getWWJoins()).append(separator);

            sb.append("The z-statistics of BB, BW joins").append(separator);
            sb.append("BB Joins: ").append(getBBJoins()).append(separator);
            sb.append("WW Joins: ").append(getWWJoins()).append(separator);

            return sb.toString();
        }
    }
}
