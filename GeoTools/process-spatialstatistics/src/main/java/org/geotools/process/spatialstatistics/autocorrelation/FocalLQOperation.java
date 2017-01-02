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
import org.geotools.process.spatialstatistics.core.WeightMatrixBuilder;
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

    private double globalLQ = 0.0;

    private double[] localLQ; // LQ

    private double[] lqD; // LQD

    private double[] lqZ; // LQZ

    public FocalLQOperation() {
        // Default Setting
        this.setSpatialConceptType(SpatialConcept.FixedDistance);
        this.setStandardizationType(StandardizationMethod.None);
        this.setDistanceType(DistanceMethod.Euclidean);
        this.setDistanceBand(0.0);
    }

    public double getLQ() {
        return globalLQ;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection inputFeatures, String xField,
            String yField) throws IOException {
        swMatrix = new WeightMatrixBuilder(getSpatialConceptType(), getStandardizationType());
        swMatrix.setDistanceMethod(getDistanceType());
        swMatrix.setDistanceBandWidth(getDistanceBand());
        swMatrix.buildWeightMatrix(inputFeatures, xField, yField);

        int featureCount = swMatrix.getEvents().size();

        // Calculate a spatial LQ for each feature in the data set.
        globalLQ = 0.0;
        localLQ = new double[featureCount];
        lqD = new double[featureCount];
        lqZ = new double[featureCount];

        // Y / X
        final double dXY = swMatrix.sumX / swMatrix.sumY;

        // Calculate LQ for each feature i.
        for (int i = 0; i < featureCount; i++) {
            SpatialEvent source = swMatrix.getEvents().get(i);

            // Initialize working variables.
            double sumX = 0.0;
            double sumY = 0.0;

            // Look for local neighbors
            for (int j = 0; j < featureCount; j++) {
                SpatialEvent target = swMatrix.getEvents().get(j);
                double wij = swMatrix.getWeight(source, target);
                if (wij == 0) {
                    continue;
                }

                sumX += target.xVal;
                sumY += target.yVal;
            }

            double dxy = sumY == 0.0 ? 0.0 : sumX / sumY; // y / x
            double tmpval2 = sumX * dXY; // x * Y/X
            double tmpval4 = 0.0;
            if (source.xVal != 0.0) {
                tmpval4 = source.yVal / source.xVal; // y / x
            }

            localLQ[i] = validateDouble(tmpval4 / dXY);
            lqD[i] = validateDouble(dxy / dXY);
            lqZ[i] = validateDouble((sumY - tmpval2) / Math.sqrt(tmpval2));

            globalLQ += Math.abs(lqD[i]);
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
        String typeName = inputFeatures.getSchema().getTypeName();
        SimpleFeatureType featureType = FeatureTypes.build(inputFeatures, typeName);

        // flq(Double), flqd(Double), fz(Double)
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
                SimpleFeature newFeature = featureWriter.buildFeature();
                featureWriter.copyAttributes(feature, newFeature, true);

                newFeature.setAttribute(fieldList[0], FormatUtils.round(localLQ[idx]));
                newFeature.setAttribute(fieldList[1], FormatUtils.round(lqD[idx]));
                newFeature.setAttribute(fieldList[2], FormatUtils.round(lqZ[idx]));

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
