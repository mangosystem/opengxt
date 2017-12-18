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
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FormatUtils;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.core.SSUtils.StatEnum;
import org.geotools.process.spatialstatistics.core.SpatialEvent;
import org.geotools.process.spatialstatistics.core.WeightMatrixBuilder;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Detect spatial clusters based on feature locations and attribute values using the Local Rogerson's R statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalRogersonROperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(LocalRogersonROperation.class);

    private double[] dcIndex;

    private double[] dcZScore;

    private double kappa = 1.0; // default

    public double getKappa() {
        return kappa;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }

    public LocalRogersonROperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.Row);
        this.setSelfNeighbors(true);
        this.setKappa(1.0);
    }

    public double[] getZScore() {
        return dcZScore;
    }

    public WeightMatrixBuilder getSpatialWeightMatrix() {
        return swMatrix;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String caseField,
            String popField) throws IOException {
        swMatrix = new WeightMatrixBuilder(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.setSelfNeighbors(isSelfNeighbors());
        swMatrix.buildWeightMatrix(inputFeatures, caseField, popField);

        // calculate the mean and standard deviation for this data set.
        int featureCount = swMatrix.getEvents().size();
        // final double sqrtTwo = Math.sqrt(2.0);

        // calculate local index for each feature i.
        dcIndex = new double[featureCount];
        dcZScore = new double[featureCount];
        for (int i = 0; i < featureCount; i++) {
            SpatialEvent source = swMatrix.getEvents().get(i);

            double ri = source.xVal / swMatrix.sumX;
            double pi = source.yVal / swMatrix.sumY;
            double ripi = ri - pi;

            double rjpjWSum = 0.0;

            // look for i's local neighbors
            for (int j = 0; j < featureCount; j++) {
                SpatialEvent target = swMatrix.getEvents().get(j);

                double rj = target.xVal / swMatrix.sumX;
                double pj = target.yVal / swMatrix.sumY;
                double rjpj = rj - pj;

                double aij = 0.0;
                if (source.id == target.id) {
                    aij = 1.0;
                } else {
                    double dij = factory.getDistance(source, target);
                    aij = dij <= 1.0 ? 1.0 : 1.0 / Math.pow(dij, kappa);
                }

                rjpjWSum += (aij * rjpj) / Math.sqrt(pj);
            }

            // calculate local index Ri
            dcIndex[i] = (ripi / Math.sqrt(pi)) * rjpjWSum;

            // E(Ri) = (aii * (1 - pi)) / N
            double expected = (1.0 - pi) / swMatrix.sumX;

            // Var(Ri) = sqrt(2) * E(Ri)
            // double variance = sqrtTwo * expected;

            // Z = 1.0 + ((dcIndex[i] - expected) / variance) * sqrtTwo = Ri / E(Ri)
            dcZScore[i] = dcIndex[i] / expected;
        }

        return buildFeatureCollection(inputFeatures);
    }

    private SimpleFeatureCollection buildFeatureCollection(SimpleFeatureCollection inputFeatures)
            throws IOException {
        // prepare feature type
        String typeName = inputFeatures.getSchema().getTypeName();
        SimpleFeatureType featureType = FeatureTypes.build(inputFeatures.getSchema(), typeName);

        // build results field name.
        final String[] fieldList = { "LRrIndex", "LRrZScore", "LRrPValue" };
        for (int k = 0; k < fieldList.length; k++) {
            featureType = FeatureTypes.add(featureType, fieldList[k], Double.class);
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        // insert features
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            int idx = 0;
            while (featureIter.hasNext()) {
                final SimpleFeature feature = featureIter.next();

                // create feature and set geometry
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                double localIndex = this.dcIndex[idx];
                double zScore = this.dcZScore[idx];
                double pValue = 0.0;

                if (Double.isNaN(zScore) || Double.isInfinite(zScore)) {
                    localIndex = 0.0;
                    zScore = 0.0;
                    pValue = 1.0;
                } else {
                    pValue = SSUtils.zProb(zScore, StatEnum.BOTH);
                }

                newFeature.setAttribute(fieldList[0], FormatUtils.round(localIndex));
                newFeature.setAttribute(fieldList[1], FormatUtils.round(zScore));
                newFeature.setAttribute(fieldList[2], FormatUtils.round(pValue));

                idx++;
                featureWriter.write(newFeature);
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }
}
