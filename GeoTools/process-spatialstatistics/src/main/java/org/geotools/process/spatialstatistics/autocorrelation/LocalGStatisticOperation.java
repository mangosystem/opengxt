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
import org.geotools.process.spatialstatistics.core.SpatialWeightMatrix;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Given a set of weighted features, identifies statistically significant hot spots and cold spots using the Getis-Ord Gi* statistic.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LocalGStatisticOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(LocalGStatisticOperation.class);

    public DistanceMethod DistanceType = DistanceMethod.Euclidean;

    SpatialWeightMatrix swMatrix = null;

    double[] dcGiValue;

    double[] dcMeanValue;

    double[] dcVarValue;

    public LocalGStatisticOperation() {
        // Gi* Default Setting
        this.setSpatialConceptType(SpatialConcept.FIXEDDISTANCEBAND);
        // #### Changed to Default to No Standardization ####
        this.setStandardizationType(StandardizationMethod.NONE);
        this.setDistanceType(DistanceMethod.Euclidean);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String inputField)
            throws IOException {
        swMatrix = new SpatialWeightMatrix(getSpatialConceptType(), getStandardizationType());
        swMatrix.distanceBandWidth = this.getDistanceBand();
        swMatrix.buildWeightMatrix(inputFeatures, inputField, this.getDistanceType());
        int featureCount = swMatrix.Events.size();
        if (featureCount < 3) {
            LOGGER.warning("inputFeatures's feature count < " + featureCount);
            return null;
        } else if (featureCount < 30) {
            LOGGER.warning("inputFeatures's feature count < " + featureCount);
        }

        // # Calculate the mean and standard deviation for this data set.
        double rN = featureCount * 1.0;
        double dZMean = swMatrix.dZSum / rN;
        double dZVar = Math.pow((swMatrix.dZ2Sum / rN) - Math.pow(dZMean, 2.0), 0.5);
        if (Math.abs(dZVar) <= 0.0) {
            LOGGER.warning("ERROR Zero variance:  all of the values for your input field are likely the same.");
        }

        // """Calculate a Gi* Z Score for each feature in the data set."""
        dcGiValue = new double[featureCount];
        dcMeanValue = new double[featureCount];
        dcVarValue = new double[featureCount];

        // # Calculate Gi* for each feature i.
        for (int i = 0; i < featureCount; i++) {
            SpatialEvent curE = swMatrix.Events.get(i);

            // # Initialize working variables.
            double dLocalZSum = 0.0;
            double dWijSum = 0.0;
            double dWij2Sum = 0.0;

            // # Look for i's local neighbors
            for (int j = 0; j < featureCount; j++) {
                SpatialEvent destE = swMatrix.Events.get(j);

                // # Calculate the weight (dWij)
                double dWeight = 0.0;
                if (this.getSpatialConceptType() == SpatialConcept.POLYGONCONTIGUITY) {
                    dWeight = 0.0;
                    // if (destE is neighbor ) dWeight = 1.0;
                } else {
                    // # calculate distance between i and j
                    dWeight = swMatrix.getWeight(curE, destE);
                }

                // #### Self Potential Adjustment ####
                // if (i == j && sSelfPotential) dWeight = dcSelf[iKey]

                if (dWeight != 0) {
                    final double dWij = dWeight;
                    dLocalZSum += dWij * destE.weight;
                    dWijSum += dWij;
                    dWij2Sum += Math.pow(dWij, 2.0);
                }
            }

            dcMeanValue[i] = dWijSum / (rN * (rN - 1.0));
            dcVarValue[i] = Math.pow((dWij2Sum / rN) - Math.pow(dcMeanValue[i], 2), 0.5);

            // # Calculate Gi*
            dcGiValue[i] = Double.NaN;
            try {
                dcGiValue[i] = ((dLocalZSum - (dWijSum * dZMean)) / (dZVar * Math.pow(
                        (((rN * dWij2Sum) - Math.pow(dWijSum, 2.0)) / (rN - 1.0)), 0.5)));
            } catch (Exception e) {
                dcGiValue[i] = Double.NaN;
            }
        }

        return buildFeatureCollection(inputFeatures);
    }

    private SimpleFeatureCollection buildFeatureCollection(SimpleFeatureCollection inputFeatures)
            throws IOException {
        // prepare feature type
        String typeName = inputFeatures.getSchema().getTypeName();
        SimpleFeatureType featureType = FeatureTypes.build(inputFeatures.getSchema(), typeName);

        String[] fieldList = new String[] { "GiZScore", "GiMean", "GiVar", "GiPValue" };
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
                SimpleFeature newFeature = featureWriter.buildFeature(null);
                featureWriter.copyAttributes(feature, newFeature, true);

                // "GiZScore", "GiMean", "GiVar", "GiPValue"
                double zScore = this.dcGiValue[idx];
                double pValue = 0.0;

                if (Double.isNaN(zScore) || Double.isInfinite(zScore)) {
                    zScore = 0.0;
                    pValue = 1.0;
                } else {
                    pValue = SSUtils.zProb(zScore, StatEnum.BOTH);
                }

                newFeature.setAttribute(fieldList[0], FormatUtils.round(zScore));
                newFeature.setAttribute(fieldList[1], FormatUtils.round(dcMeanValue[idx]));
                newFeature.setAttribute(fieldList[2], FormatUtils.round(dcVarValue[idx]));
                newFeature.setAttribute(fieldList[3], FormatUtils.round(pValue));

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
