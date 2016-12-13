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
import org.geotools.process.spatialstatistics.core.WeightMatrixBuilder;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Measures spatial autocorrelation based on feature locations and attribute values using the Local Geary's C statistic.
 * 
 * @reference http://www.passagesoftware.net/webhelp/Introduction.htm#Local_Geary_s_c.htm
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalGearysCOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(LocalGearysCOperation.class);

    private double[] dcIndex;

    private double[] dcZScore;

    public LocalGearysCOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
        this.setSelfNeighbors(false);
    }

    public double[] getZScore() {
        return dcZScore;
    }

    public WeightMatrixBuilder getSpatialWeightMatrix() {
        return swMatrix;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String inputField)
            throws IOException {
        swMatrix = new WeightMatrixBuilder(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.setSelfNeighbors(isSelfNeighbors());
        swMatrix.buildWeightMatrix(inputFeatures, inputField);

        // calculate the mean and standard deviation for this data set.
        int featureCount = swMatrix.getEvents().size();
        double n = swMatrix.getEvents().size();
        double meanX = swMatrix.sumX / n;
        double m2 = 0.0;
        double m4 = 0.0;

        // calculate deviation from the mean sums.
        for (SpatialEvent source : swMatrix.getEvents()) {
            m2 += Math.pow(source.xVal - meanX, 2.0);
            m4 += Math.pow(source.xVal - meanX, 4.0);
        }

        m2 = m2 / (n - 1.0);
        m4 = m4 / (n - 1.0);
        double b2 = m4 / Math.pow(m2, 2.0);

        // calculate local index for each feature i.
        dcIndex = new double[featureCount];
        dcZScore = new double[featureCount];
        for (int i = 0; i < featureCount; i++) {
            SpatialEvent source = swMatrix.getEvents().get(i);

            // initialize working variables.
            double localDevSum = 0.0;
            double wijSum = 0.0;
            double wij2Sum = 0.0;

            // look for i's local neighbors
            for (int j = 0; j < featureCount; j++) {
                SpatialEvent target = swMatrix.getEvents().get(j);
                if (!isSelfNeighbors() && source.id == target.id) {
                    continue;
                }

                // calculate the weight (wij)
                double wij = swMatrix.getWeight(source, target);
                wij = swMatrix.standardizeWeight(source, wij);
                if (wij == 0) {
                    continue;
                }

                // geary's c
                double ijxd = source.xVal - target.xVal;
                localDevSum += wij * Math.pow(ijxd, 2.0);
                wijSum += wij;
                wij2Sum += Math.pow(wij, 2.0);
            }

            // calculate local index
            dcIndex[i] = Double.NaN;
            dcZScore[i] = Double.NaN;
            try {
                dcIndex[i] = localDevSum / m2;

                double dExpected = (2.0 * n * wijSum) / (n - 1.0);
                double v1 = n / (n - 1.0);
                double v2 = Math.pow(wijSum, 2.0) + wij2Sum;
                double v3 = 3.0 + b2;
                double v4 = Math.pow((2.0 * n * wijSum) / (n - 1.0), 2.0);
                double dVariance = (v1 * v2 * v3) - v4;
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
        final String[] fieldList = { "LGcIndex", "LGcZScore", "LGcPValue" };
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

                featureWriter.write(newFeature);
                idx++;
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
        } finally {
            featureWriter.close(featureIter);
        }

        return featureWriter.getFeatureCollection();
    }
}
