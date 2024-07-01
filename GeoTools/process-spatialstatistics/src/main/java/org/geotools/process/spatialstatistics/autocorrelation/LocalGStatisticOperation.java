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

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
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

/**
 * Given a set of weighted features, identifies statistically significant hot spots and cold spots using the Getis-Ord Gi* statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalGStatisticOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(LocalGStatisticOperation.class);

    private double[] dcGiZScore;

    private double[] dcMean;

    private double[] dcVar;

    public LocalGStatisticOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.None);
        this.setSelfNeighbors(true);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String inputField)
            throws IOException {
        swMatrix = new WeightMatrixBuilder(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.setSelfNeighbors(isSelfNeighbors());
        swMatrix.buildWeightMatrix(inputFeatures, inputField);

        int featureCount = swMatrix.getEvents().size();
        if (featureCount < 3) {
            LOGGER.warning("inputFeatures's feature count < " + featureCount);
            return null;
        } else if (featureCount < 30) {
            LOGGER.warning("inputFeatures's feature count < " + featureCount);
        }

        // calculate the mean and standard deviation for this data set.
        double n = swMatrix.getEvents().size();
        double meanX = swMatrix.sumX / n;
        double varX = Math.pow((swMatrix.sumX2 / n) - Math.pow(meanX, 2.0), 0.5);
        if (Math.abs(varX) <= 0.0) {
            LOGGER.warning("ERROR Zero variance:  all of the values for your input field are likely the same.");
        }

        dcGiZScore = new double[featureCount];
        dcMean = new double[featureCount];
        dcVar = new double[featureCount];

        // calculate Gi* for each feature i.
        for (int i = 0; i < featureCount; i++) {
            SpatialEvent source = swMatrix.getEvents().get(i);

            // initialize working variables.
            double localSum = 0.0;
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

                localSum += wij * target.xVal;
                wijSum += wij;
                wij2Sum += Math.pow(wij, 2.0);
            }

            dcMean[i] = wijSum / (n * (n - 1.0));
            dcVar[i] = Math.pow((wij2Sum / n) - Math.pow(dcMean[i], 2), 0.5);

            // calculate Gi / Gi*
            dcGiZScore[i] = Double.NaN;
            try {
                double wijSum2 = Math.pow(wijSum, 2.0);
                double b = (varX * Math.pow((((n * wij2Sum) - wijSum2) / (n - 1.0)), 0.5));
                dcGiZScore[i] = (localSum - (wijSum * meanX)) / b;
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

        String[] fieldList = new String[] { "GiZScore", "GiPValue", "GiMean", "GiVar" };
        for (int k = 0; k < fieldList.length; k++) {
            featureType = FeatureTypes.add(featureType, fieldList[k], Double.class);
        }

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

                // "GiZScore", "GiPValue", "GiMean", "GiVar"
                double zScore = dcGiZScore[idx];
                double pValue = 0.0;

                if (Double.isNaN(zScore) || Double.isInfinite(zScore)) {
                    zScore = 0.0;
                    pValue = 1.0;
                } else {
                    pValue = SSUtils.zProb(zScore, StatEnum.BOTH);
                }

                newFeature.setAttribute(fieldList[0], FormatUtils.round(zScore));
                newFeature.setAttribute(fieldList[1], FormatUtils.round(pValue));
                newFeature.setAttribute(fieldList[2], FormatUtils.round(dcMean[idx]));
                newFeature.setAttribute(fieldList[3], FormatUtils.round(dcVar[idx]));

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
