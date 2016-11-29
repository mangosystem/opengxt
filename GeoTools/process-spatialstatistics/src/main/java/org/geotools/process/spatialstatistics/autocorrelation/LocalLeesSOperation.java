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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FormatUtils;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.core.SSUtils.StatEnum;
import org.geotools.process.spatialstatistics.core.SpatialEvent;
import org.geotools.process.spatialstatistics.core.SpatialWeightMatrix;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Measures spatial autocorrelation based on feature locations and attribute values using the Local Lee's S statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalLeesSOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(LocalLeesSOperation.class);

    private SpatialWeightMatrix swMatrix = null;

    private double[] dcIndex;

    private double[] dcZScore;

    public LocalLeesSOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
    }

    public double[] getZScore() {
        return dcZScore;
    }

    public SpatialWeightMatrix getSpatialWeightMatrix() {
        return swMatrix;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String inputField)
            throws IOException {
        swMatrix = new SpatialWeightMatrix(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.buildWeightMatrix(inputFeatures, inputField);

        // calculate the mean and standard deviation for this data set.
        int featureCount = swMatrix.getEvents().size();
        double n = swMatrix.getEvents().size() * 1.0;
        double dZMean = swMatrix.dZSum / n;

        double dM2 = 0.0;
        double dM4 = 0.0;

        // calculate deviation from the mean sums.
        for (SpatialEvent curE : swMatrix.getEvents()) {
            dM2 += Math.pow(curE.weight - dZMean, 2.0);
            dM4 += Math.pow(curE.weight - dZMean, 4.0);
        }

        dM2 = dM2 / (n - 1.0);
        dM4 = dM4 / (n - 1.0);
        double dB2 = dM4 / Math.pow(dM2, 2.0);

        // calculate local index for each feature i.
        dcIndex = new double[featureCount];
        dcZScore = new double[featureCount];
        for (int i = 0; i < featureCount; i++) {
            SpatialEvent curE = swMatrix.getEvents().get(i);
            double dLocalZDevSum = 0.0;
            double dWijSum = 0.0;
            double dWij2Sum = 0.0;
            double dWijWihSum = 0.0;

            // look for i's local neighbors
            for (int j = 0; j < featureCount; j++) {
                SpatialEvent destE = swMatrix.getEvents().get(j);
                if (curE.oid == destE.oid)
                    continue;

                // calculate the weight (dWij)
                double dWij = swMatrix.getWeight(curE, destE);

                if (getStandardizationType() == StandardizationMethod.Row) {
                    dWij = swMatrix.standardizeWeight(curE, dWij);
                }

                // lee's s
                dLocalZDevSum += dWij * (destE.weight - dZMean);
                dWijSum += dWij;
                dWij2Sum += Math.pow(dWij, 2.0);
            }

            dWijWihSum = Math.pow(dWijSum, 2.0) - dWij2Sum;

            // calculate local index
            dcIndex[i] = Double.NaN;
            dcZScore[i] = Double.NaN;
            try {
                // TODO correct
                dcIndex[i] = Math.pow(dLocalZDevSum, 2.0) / dM2;

                double dExpected = -1.0 * (dWijSum / (n - 1.0));
                double v1 = (dWij2Sum * (n - dB2)) / (n - 1.0);
                double v2 = Math.pow(dWijSum, 2.0) / Math.pow((n - 1.0), 2.0);
                double v3 = dWijWihSum * ((2.0 * dB2) - n);
                double v4 = (n - 1.0) * (n - 2.0);
                double dVariance = v1 + v3 / v4 - v2;
                dcZScore[i] = (dcIndex[i] - dExpected) / Math.pow(dVariance, 0.5);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
            }
        }

        return buildFeatureCollection(inputFeatures);
    }

    private SimpleFeatureCollection buildFeatureCollection(SimpleFeatureCollection inputFeatures)
            throws IOException {
        // prepare feature type
        String typeName = inputFeatures.getSchema().getTypeName();
        SimpleFeatureType featureType = FeatureTypes.build(inputFeatures.getSchema(), typeName);

        // build results field name.
        final String[] fieldList = { "LLsIndex", "LLsZScore", "LLsPValue" };
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
                SimpleFeature newFeature = featureWriter.buildFeature(feature.getID());
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
