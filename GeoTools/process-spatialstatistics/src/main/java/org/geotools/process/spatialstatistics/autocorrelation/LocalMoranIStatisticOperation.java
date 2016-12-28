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
 * Given a set of weighted features, identifies statistically significant hot spots, cold spots, and spatial outliers using the Anselin Local Moran's
 * I statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalMoranIStatisticOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(LocalMoranIStatisticOperation.class);

    private double[] dcIndex;

    private double[] dcZScore;

    private String[] moranBins;

    public LocalMoranIStatisticOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
        this.setSelfNeighbors(false);
    }

    public double[] getIndex() {
        return dcIndex;
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

        // Calculate the mean and standard deviation for this data set.
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
        moranBins = new String[featureCount];
        for (int i = 0; i < featureCount; i++) {
            SpatialEvent source = swMatrix.getEvents().get(i);

            // initialize working variables.
            double zxjWSum = 0.0;
            double wijSum = 0.0;
            double wij2Sum = 0.0;
            double localBinSum = 0.0;
            int numNeighbors = 0;

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

                if (wij > 0) {
                    localBinSum += wij * target.xVal;
                    numNeighbors++;
                }

                zxjWSum += wij * (target.xVal - meanX);
                wijSum += wij;
                wij2Sum += Math.pow(wij, 2.0);
            }

            // calculate Local i
            dcIndex[i] = Double.NaN;
            dcZScore[i] = Double.NaN;
            moranBins[i] = "";

            try {
                dcIndex[i] = ((source.xVal - meanX) / m2) * zxjWSum;

                double dExpected = (-1.0 * wijSum) / (n - 1);
                double wijWihSum = Math.pow(wijSum, 2.0) - wij2Sum;
                double v1 = (wij2Sum * (n - b2)) / (n - 1);
                double v2 = Math.pow(wijSum, 2.0) / Math.pow(n - 1, 2.0);
                double v3 = wijWihSum * ((2.0 * b2) - n);
                double v4 = (n - 1) * (n - 2);
                double dVariance = v1 + (v3 / v4) - v2;
                dcZScore[i] = (dcIndex[i] - dExpected) / Math.pow(dVariance, 0.5);

                if (numNeighbors > 0) {
                    double localMean = localBinSum / wijSum;
                    moranBins[i] = returnMoranBin(dcZScore[i], source.xVal, meanX, localMean);
                }
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

        // Build results field name.
        String[] fields = { "LMiIndex", "LMiZScore", "LMiPValue", "COType" };
        for (int k = 0; k < fields.length - 1; k++) {
            featureType = FeatureTypes.add(featureType, fields[k], Double.class);
        }
        featureType = FeatureTypes.add(featureType, fields[fields.length - 1], String.class, 10);

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        // insert features
        int idx = 0;
        SimpleFeatureIterator featureIter = null;
        try {
            featureIter = inputFeatures.features();
            while (featureIter.hasNext()) {
                final SimpleFeature feature = featureIter.next();

                // create feature and set geometry
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                // "LMiIndex", "LMiZScore", "LMiPValue", "COType"
                double localI = this.dcIndex[idx];
                double zScore = this.dcZScore[idx];
                String coType = this.moranBins[idx];

                double pValue = 0.0;
                if (Double.isNaN(zScore) || Double.isInfinite(zScore)) {
                    localI = 0.0;
                    zScore = 0.0;
                    pValue = 1.0;
                    coType = "";
                } else {
                    pValue = SSUtils.zProb(zScore, StatEnum.BOTH);
                }

                newFeature.setAttribute(fields[0], FormatUtils.round(localI));
                newFeature.setAttribute(fields[1], FormatUtils.round(zScore));
                newFeature.setAttribute(fields[2], FormatUtils.round(pValue));
                newFeature.setAttribute(fields[3], coType);

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

    private String returnMoranBin(double zScore, double featureVal, double globalMean,
            double localMean) {
        String moranBin = "";

        if (Double.isInfinite(zScore) || Double.isNaN(zScore)) {
            return moranBin;
        }

        if (Math.abs(zScore) < 1.96) {
            return moranBin;
        } else {
            if (zScore > 1.96) {
                moranBin = localMean >= globalMean ? "HH" : "LL";
            } else {
                if (featureVal >= globalMean && localMean <= globalMean) {
                    moranBin = "HL";
                } else if (featureVal <= globalMean && localMean >= globalMean) {
                    moranBin = "LH";
                }
            }
        }

        return moranBin;
    }
}
