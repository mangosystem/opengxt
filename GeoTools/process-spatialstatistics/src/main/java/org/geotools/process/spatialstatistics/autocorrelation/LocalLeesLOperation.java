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
 * Measures spatial autocorrelation based on feature locations and attribute values using the Local Lee's L statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalLeesLOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(LocalLeesLOperation.class);

    private double[] dcIndex;

    private double[] dcZScore;

    public LocalLeesLOperation() {
        // Default Setting
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setSpatialConceptType(SpatialConcept.InverseDistance);
        this.setStandardizationType(StandardizationMethod.Row);
        this.setSelfNeighbors(true);
    }

    public double[] getZScore() {
        return dcZScore;
    }

    public WeightMatrixBuilder getSpatialWeightMatrix() {
        return swMatrix;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String xField,
            String yField) throws IOException {
        swMatrix = new WeightMatrixBuilder(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.setSelfNeighbors(isSelfNeighbors());
        swMatrix.buildWeightMatrix(inputFeatures, xField, yField);

        // calculate the mean and standard deviation for this data set.
        int featureCount = swMatrix.getEvents().size();
        double n = swMatrix.getEvents().size();
        double meanX = swMatrix.sumX / n;
        double meanY = swMatrix.sumY / n;
        double mx2 = 0.0;
        double my2 = 0.0;
        double wij2Sum = 0.0;

        // calculate deviation from the mean sums.
        for (SpatialEvent source : swMatrix.getEvents()) {
            mx2 += Math.pow(source.xVal - meanX, 2.0);
            my2 += Math.pow(source.yVal - meanY, 2.0);
            double jwijSum = 0.0;
            for (SpatialEvent target : swMatrix.getEvents()) {
                if (!isSelfNeighbors() && source.id == target.id) {
                    continue;
                }
                jwijSum += swMatrix.getWeight(source, target);
            }
            wij2Sum += Math.pow(jwijSum, 2);
        }

        double mx2sqr = Math.sqrt(mx2);
        double my2sqr = Math.sqrt(my2);

        // calculate local index for each feature i.
        dcIndex = new double[featureCount];
        dcZScore = new double[featureCount];
        for (int i = 0; i < featureCount; i++) {
            SpatialEvent source = swMatrix.getEvents().get(i);

            // initialize working variables.
            double zxjWSum = 0.0;
            double zyjWSum = 0.0;

            // look for i's local neighbors
            for (int j = 0; j < featureCount; j++) {
                SpatialEvent target = swMatrix.getEvents().get(j);
                if (!isSelfNeighbors() && source.id == target.id) {
                    continue;
                }

                // calculate the weight (dWij)
                double wij = swMatrix.getWeight(source, target);
                wij = swMatrix.standardizeWeight(source, wij);
                if (wij == 0) {
                    continue;
                }

                // lee's l
                double zxj = target.xVal - meanX;
                double zyj = target.yVal - meanY;

                zxjWSum += wij * zxj;
                zyjWSum += wij * zyj;
            }

            // calculate local index
            dcIndex[i] = Double.NaN;
            dcZScore[i] = Double.NaN;
            try {
                dcIndex[i] = (Math.pow(n, 2) / wij2Sum) * ((zxjWSum * zyjWSum) / (mx2sqr * my2sqr));

                // TODO correct
                dcZScore[i] = 0.0;
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
        final String[] fieldList = { "LLlIndex", "LLlZScore", "LLlPValue" };
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
