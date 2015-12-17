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
 * Given a set of weighted features, identifies statistically significant hot spots, cold spots, and spatial outliers using the Anselin Local Moran's
 * I statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalMoranIStatisticOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(LocalMoranIStatisticOperation.class);

    public DistanceMethod DistanceType = DistanceMethod.Euclidean;

    SpatialWeightMatrix swMatrix = null;

    double[] dcIndex;

    double[] dcZScore;

    String[] moranBins;

    double[] dczValue;

    double[] dcwzValue;

    public LocalMoranIStatisticOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.INVERSEDISTANCE);
        this.setStandardizationType(StandardizationMethod.NONE);
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
        swMatrix.distanceBandWidth = this.getDistanceBand();
        swMatrix.buildWeightMatrix(inputFeatures, inputField, this.getDistanceType());

        // # Calculate the mean and standard deviation for this data set.
        int featureCount = swMatrix.Events.size();
        double n = featureCount * 1.0;
        double dZMean = swMatrix.dZSum / n;

        double dM2 = 0.0;
        double dM4 = 0.0;

        // calculate deviation from the mean sums.
        for (SpatialEvent curE : swMatrix.Events) {
            dM2 += Math.pow(curE.weight - dZMean, 2.0);
            dM4 += Math.pow(curE.weight - dZMean, 4.0);
        }

        dM2 = dM2 / (n - 1.0);
        dM4 = dM4 / (n - 1.0);
        double dB2 = dM4 / Math.pow(dM2, 2.0);

        // calculate Local Index for each feature i.
        dcIndex = new double[featureCount];
        dcZScore = new double[featureCount];
        moranBins = new String[featureCount];
        dczValue = new double[featureCount];
        dcwzValue = new double[featureCount];
        for (int i = 0; i < featureCount; i++) {
            SpatialEvent curE = swMatrix.Events.get(i);
            double dLocalZDevSum = 0.0;
            double dWijSum = 0.0;
            double dWij2Sum = 0.0;
            double dWijWihSum = 0.0;
            double localBinTotal = 0.0;
            int numNeighs = 0;

            // # Look for i's local neighbors
            for (int j = 0; j < featureCount; j++) {
                SpatialEvent destE = swMatrix.Events.get(j);
                if (curE.oid == destE.oid)
                    continue;

                // # Calculate the weight (dWij)
                double dWij = 0.0;
                if (this.getSpatialConceptType() == SpatialConcept.POLYGONCONTIGUITY) {
                    dWij = 0.0;
                    // if (destE is neighbor ) dWeight = 1.0;
                } else {
                    dWij = swMatrix.getWeight(curE, destE);
                }

                if (getStandardizationType() == StandardizationMethod.ROW) {
                    dWij = swMatrix.standardizeWeight(curE, dWij);
                }

                // moran's i
                dLocalZDevSum += dWij * (destE.weight - dZMean);
                if (dWij > 0) {
                    localBinTotal += dWij * destE.weight;
                    numNeighs++;
                }

                dWijSum += dWij;
                dWij2Sum += Math.pow(dWij, 2.0);
            } // next j

            dWijWihSum = Math.pow(dWijSum, 2.0) - dWij2Sum;

            // # Calculate Local I
            dcIndex[i] = Double.NaN;
            dcZScore[i] = Double.NaN;
            moranBins[i] = "";
            try {
                dcIndex[i] = ((curE.weight - dZMean) / dM2) * dLocalZDevSum;

                // gaiyong
                dczValue[i] = ((curE.weight - dZMean) / dM2);
                dcwzValue[i] = dLocalZDevSum;
                // end

                double dExpected = -1.0 * (dWijSum / (n - 1.0));
                double v1 = (dWij2Sum * (n - dB2)) / (n - 1.0);
                double v2 = Math.pow(dWijSum, 2.0) / Math.pow((n - 1.0), 2.0);
                double v3 = dWijWihSum * ((2.0 * dB2) - n);
                double v4 = (n - 1.0) * (n - 2.0);
                double dVariance = v1 + v3 / v4 - v2;
                dcZScore[i] = (dcIndex[i] - dExpected) / Math.pow(dVariance, 0.5);
                if (numNeighs > 0) {
                    double localMean = localBinTotal / (dWijSum * 1.0);
                    moranBins[i] = this.returnMoranBin(dcZScore[i], curE.weight, dZMean, localMean);
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
            }
        } // next i

        return buildFeatureCollection(inputFeatures);
    }

    private SimpleFeatureCollection buildFeatureCollection(SimpleFeatureCollection inputFeatures)
            throws IOException {
        // prepare feature type
        String typeName = inputFeatures.getSchema().getTypeName();
        SimpleFeatureType featureType = FeatureTypes.build(inputFeatures.getSchema(), typeName);

        // # Build results field name.
        String[] fieldList = { "LMiIndex", "LMiZScore", "LMiPValue", "LMizValue", "LMiwzValue",
                "COType" };
        for (int k = 0; k < fieldList.length - 1; k++) {
            featureType = FeatureTypes.add(featureType, fieldList[k], Double.class);
        }
        featureType = FeatureTypes.add(featureType, fieldList[5], String.class, 10);

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
                SimpleFeature newFeature = featureWriter.buildFeature(null);
                featureWriter.copyAttributes(feature, newFeature, true);

                // "LMiIndex", "LMiZScore", "LMiPValue", "COType"
                double localI = this.dcIndex[idx];
                double zScore = this.dcZScore[idx];
                double pValue = 0.0;
                String coType = this.moranBins[idx];

                double dczv = this.dczValue[idx];
                double dcwv = this.dcwzValue[idx];

                if (Double.isNaN(zScore) || Double.isInfinite(zScore)) {
                    localI = 0.0;
                    zScore = 0.0;
                    pValue = 1.0;
                    coType = "";
                } else {
                    pValue = SSUtils.zProb(zScore, StatEnum.BOTH);
                }

                newFeature.setAttribute(fieldList[0], FormatUtils.round(localI));
                newFeature.setAttribute(fieldList[1], FormatUtils.round(zScore));
                newFeature.setAttribute(fieldList[2], FormatUtils.round(pValue));
                newFeature.setAttribute(fieldList[3], FormatUtils.round(dczv));
                newFeature.setAttribute(fieldList[4], FormatUtils.round(dcwv));
                newFeature.setAttribute(fieldList[5], coType);

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

    private String returnMoranBin(double zScore, double featureVal, double globalMean,
            double localMean) {
        // Returns a string representation of Local Moran's I Cluster-Outlier classification bins.

        // HH = Cluster of Highs, L = Cluster of Lows, HL = High Outlier, LH = Low Outlier.
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
                } else {
                    moranBin = "";
                }
            }
        }

        return moranBin;
    }

}
