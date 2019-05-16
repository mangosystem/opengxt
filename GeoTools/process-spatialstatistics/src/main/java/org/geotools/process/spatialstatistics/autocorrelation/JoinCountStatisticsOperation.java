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
import org.geotools.process.spatialstatistics.core.DataUtils;
import org.geotools.process.spatialstatistics.enumeration.ContiguityType;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

/**
 * Spatial autocorrelation for binary attributes.
 * 
 * @reference http://www.gis.ttu.edu/gist4302/documents/lectures/Spring%202014/lecture6.pdf
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class JoinCountStatisticsOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(JoinCountStatisticsOperation.class);

    public JoinCountStatisticsOperation() {
    }

    public JoinCount execute(SimpleFeatureCollection features, Filter blackExpression,
            ContiguityType contiguityType) throws IOException {
        String typeName = features.getSchema().getTypeName();
        String the_geom = features.getSchema().getGeometryDescriptor().getLocalName();

        int blackCount = 0;
        int whiteCount = 0;
        int m = 0;

        // using SpatialIndexFeatureCollection
        SimpleFeatureCollection indexed = DataUtils.toSpatialIndexFeatureCollection(features);

        JoinCount joinCounts = new JoinCount(typeName, contiguityType);
        SimpleFeatureIterator featureIter = indexed.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature pFeature = featureIter.next();
                Geometry pGeometry = (Geometry) pFeature.getDefaultGeometry();
                boolean primary = blackExpression.evaluate(pFeature);
                if (primary) {
                    blackCount++;
                } else {
                    whiteCount++;
                }

                Filter filter = getIntersectsFilter(the_geom, pGeometry);
                SimpleFeatureIterator subIter = indexed.subCollection(filter).features();
                int neighborCount = 0;
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
                        boolean secondary = blackExpression.evaluate(sFeature);
                        joinCounts.visit(primary, secondary);
                        neighborCount++;
                    }
                } finally {
                    subIter.close();
                }

                m += neighborCount * (neighborCount - 1);
            }
        } finally {
            featureIter.close();
        }

        // post process
        joinCounts.setFeatureCount(blackCount + whiteCount);
        joinCounts.setBlackCount(blackCount);
        joinCounts.setWhiteCount(whiteCount);
        joinCounts.postProcess(m / 2.0);
        return joinCounts;
    }

    public static class JoinCount {

        private String typeName;

        private ContiguityType contiguityType = ContiguityType.Queen;

        private int featureCount = 0;

        private int blackCount = 0;

        private int whiteCount = 0;

        private int observedBB = 0;

        private int observedWW = 0;

        private int observedBW = 0;

        private double expectedBB = 0;

        private double expectedWW = 0;

        private double expectedBW = 0;

        private double stdDevBB = 0;

        private double stdDevWW = 0;

        private double stdDevBW = 0;

        private double zScoreBB = 0;

        private double zScoreWW = 0;

        private double zScoreBW = 0;

        public JoinCount(String typeName, ContiguityType contiguityType) {
            this.typeName = typeName;
            this.contiguityType = contiguityType;
        }

        public void visit(boolean primary, boolean secondary) {
            if (primary && secondary) {
                observedBB++;
            } else if (!primary && !secondary) {
                observedWW++;
            } else {
                observedBW++;
            }
        }

        public void postProcess(double m) {
            // Expected
            final double pB = (double) blackCount / featureCount;
            final double pW = 1 - pB;
            final double k = getNumberOfJoins();

            expectedBB = k * pB * pB;
            expectedWW = k * pW * pW;
            expectedBW = 2.0f * k * pB * pW;

            // Standard Deviation of Expected (standard error)
            stdDevBB = Math.sqrt((k * Math.pow(pB, 2)) + (2 * m * Math.pow(pB, 3))
                    - ((k + (2 * m)) * Math.pow(pB, 4)));
            stdDevWW = Math.sqrt((k * Math.pow(pW, 2)) + (2 * m * Math.pow(pW, 3))
                    - ((k + (2 * m)) * Math.pow(pW, 4)));
            stdDevBW = Math.sqrt((2 * (k + m) * pB * pW)
                    - (4 * (k + 2 * m) * Math.pow(pB, 2) * Math.pow(pW, 2)));

            // Test statistic z-score
            zScoreBB = (getObservedBB() - expectedBB) / stdDevBB;
            zScoreWW = (getObservedWW() - expectedWW) / stdDevWW;
            zScoreBW = (getObservedBW() - expectedBW) / stdDevBW;
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

        public int getNumberOfJoins() {
            return getObservedBB() + getObservedWW() + getObservedBW();
        }

        public ContiguityType getContiguityType() {
            return contiguityType;
        }

        public void setContiguityType(ContiguityType contiguityType) {
            this.contiguityType = contiguityType;
        }

        public int getBlackCount() {
            return blackCount;
        }

        public void setBlackCount(int blackCount) {
            this.blackCount = blackCount;
        }

        public int getWhiteCount() {
            return whiteCount;
        }

        public void setWhiteCount(int whiteCount) {
            this.whiteCount = whiteCount;
        }

        public int getObservedBB() {
            return observedBB / 2;
        }

        public int getObservedWW() {
            return observedWW / 2;
        }

        public int getObservedBW() {
            return observedBW / 2;
        }

        public double getExpectedBB() {
            return expectedBB;
        }

        public double getExpectedWW() {
            return expectedWW;
        }

        public double getExpectedBW() {
            return expectedBW;
        }

        public double getStdDevBB() {
            return stdDevBB;
        }

        public double getStdDevWW() {
            return stdDevWW;
        }

        public double getStdDevBW() {
            return stdDevBW;
        }

        public double getzScoreBB() {
            return zScoreBB;
        }

        public double getzScoreWW() {
            return zScoreWW;
        }

        public double getzScoreBW() {
            return zScoreBW;
        }
    }
}
