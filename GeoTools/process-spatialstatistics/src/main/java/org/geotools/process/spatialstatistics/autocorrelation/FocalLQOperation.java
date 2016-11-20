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
import org.geotools.process.spatialstatistics.core.SpatialEvent;
import org.geotools.process.spatialstatistics.core.SpatialWeightMatrix2;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Calculates a Focal Location Quotients (Focal LQ).
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FocalLQOperation extends AbstractStatisticsOperation {
    protected static final Logger LOGGER = Logging.getLogger(FocalLQOperation.class);

    // Wong(2005) Location Quotient

    private SpatialWeightMatrix2 swMatrix = null;

    private double locationQuotient = 0.0;

    public double getLQ() {
        return locationQuotient;
    }

    double[] dcLocalLQ;

    double[] localLQ;

    double[] dcZValue;

    public FocalLQOperation() {
        this.setSpatialConceptType(SpatialConcept.FIXEDDISTANCEBAND);
        this.setStandardizationType(StandardizationMethod.NONE);
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setDistanceBand(0.0);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String fieldName1,
            String fieldName2) throws IOException {
        swMatrix = new SpatialWeightMatrix2(getSpatialConceptType(), getStandardizationType());
        swMatrix.distanceBandWidth = this.getDistanceBand();
        swMatrix.buildWeightMatrix(inputFeatures, fieldName1, fieldName2, this.getDistanceType());

        int featureCount = swMatrix.Events.size();

        // Calculate a spatial LQ for each feature in the data set.
        dcLocalLQ = new double[featureCount];
        localLQ = new double[featureCount];
        dcZValue = new double[featureCount];
        locationQuotient = 0.0;

        // Y / X
        final double dXY = swMatrix.dZSum / swMatrix.dPopSum;

        // # Calculate LQ for each feature i.
        for (int i = 0; i < featureCount; i++) {
            SpatialEvent curE = swMatrix.Events.get(i);

            // # Initialize working variables.
            double dLocalObsSum = 0.0; // All Count
            double dLocalPopSum = 0.0; // Count

            // # Look for local neighbors
            for (int j = 0; j < featureCount; j++) {
                SpatialEvent destE = swMatrix.Events.get(j);

                if (swMatrix.distanceBandWidth > 0) {
                    // apply search radius
                    double dDist = factory.getDistance(curE, destE, getDistanceType());
                    if (dDist <= swMatrix.distanceBandWidth) {
                        dLocalObsSum += destE.weight;
                        dLocalPopSum += destE.population;
                    }
                } else {
                    // apply all features
                    dLocalObsSum += destE.weight;
                    dLocalPopSum += destE.population;
                }
            }

            double dxy = dLocalPopSum == 0.0 ? 0.0 : dLocalObsSum / dLocalPopSum; // y / x
            double tmpval2 = dLocalObsSum * dXY; // x * Y/X
            double tmpval4 = 0.0;
            if (curE.weight != 0.0) {
                tmpval4 = curE.population / curE.weight; // y / x
            }

            localLQ[i] = validateDouble(tmpval4 / dXY);
            dcLocalLQ[i] = validateDouble(dxy / dXY);
            dcZValue[i] = validateDouble((dLocalPopSum - tmpval2) / Math.sqrt(tmpval2));

            // global LQ += ABS(local lq)
            locationQuotient += Math.abs(dcLocalLQ[i]);
        }

        return buildFeatureCollection(inputFeatures);
    }

    private double validateDouble(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) {
            return 0.0;
        }
        return val;
    }

    private SimpleFeatureCollection buildFeatureCollection(SimpleFeatureCollection inputFeatures)
            throws IOException {
        // prepare feature type
        SimpleFeatureType featureType = FeatureTypes.build(inputFeatures, getOutputTypeName());

        // FLQ(Double), FLQD(Double), FZ(Double)
        String[] fieldList = new String[] { "flq", "flqd", "fz" };
        for (int k = 0; k < fieldList.length; k++) {
            featureType = FeatureTypes.add(featureType, fieldList[k], Double.class);
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(featureType);

        // insert features
        int idx = 0;
        SimpleFeatureIterator featureIter = inputFeatures.features();
        try {
            while (featureIter.hasNext()) {
                final SimpleFeature feature = featureIter.next();

                // create feature and set geometry
                SimpleFeature newFeature = featureWriter.buildFeature(null);
                featureWriter.copyAttributes(feature, newFeature, true);

                // FLQ(Double), FLQD(Double), FZ(Double)
                newFeature.setAttribute(fieldList[0], FormatUtils.round(localLQ[idx]));
                newFeature.setAttribute(fieldList[1], FormatUtils.round(dcLocalLQ[idx]));
                newFeature.setAttribute(fieldList[2], FormatUtils.round(dcZValue[idx]));

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
