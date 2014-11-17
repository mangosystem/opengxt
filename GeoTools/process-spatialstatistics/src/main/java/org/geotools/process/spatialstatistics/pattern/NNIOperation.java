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
package org.geotools.process.spatialstatistics.pattern;

import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.spatialstatistics.core.DistanceFactory;
import org.geotools.process.spatialstatistics.core.FormatUtils;
import org.geotools.process.spatialstatistics.core.SSUtils;
import org.geotools.process.spatialstatistics.core.SSUtils.StatEnum;
import org.geotools.process.spatialstatistics.core.SpatialEvent;
import org.geotools.process.spatialstatistics.enumeration.DistanceMethod;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Calculates a nearest neighbor index based on the average distance from each feature to its nearest neighboring feature.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class NNIOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(NNIOperation.class);

    private final DistanceFactory factory = DistanceFactory.newInstance();

    private DistanceMethod distanceMethod = DistanceMethod.Euclidean;

    private int featureCount = 0;

    private double studyArea = 0;

    private double observedMeanDist = 0;

    private String typeName = "Average Nearest Neighbor Ratio";

    public void setDistanceType(DistanceMethod distanceMethod) {
        this.distanceMethod = distanceMethod;
    }

    public double getConvexHullArea(List<SpatialEvent> srcEvents) {
        Coordinate[] coordinates = new Coordinate[srcEvents.size()];
        for (int k = 0; k < srcEvents.size(); k++) {
            SpatialEvent srcEvent = srcEvents.get(k);
            coordinates[k] = new Coordinate(srcEvent.x, srcEvent.y);
        }

        ConvexHull cbxBuidler = new ConvexHull(coordinates, new GeometryFactory());
        Geometry convexHull = cbxBuidler.getConvexHull();

        return convexHull.getArea();
    }

    public NearestNeighborResult execute(SimpleFeatureCollection features) {
        return execute(features, features.getBounds().getArea());
    }

    public NearestNeighborResult execute(SimpleFeatureCollection features, double studyArea) {
        typeName = features.getSchema().getTypeName();
        return execute(DistanceFactory.loadEvents(features, null), studyArea);
    }

    public NearestNeighborResult execute(List<SpatialEvent> events, double studyArea) {
        observedMeanDist = 0.0;

        factory.DistanceType = distanceMethod;

        featureCount = events.size();
        if (studyArea == 0) {
            this.studyArea = getConvexHullArea(events);
        } else {
            this.studyArea = studyArea;
        }

        double minDistance = 0.0;
        double sumNearestDist = 0.0;

        for (SpatialEvent curEvent : events) {
            minDistance = factory.getMinimumDistance(events, curEvent);
            sumNearestDist += minDistance;
        }

        observedMeanDist = sumNearestDist / (featureCount * 1.0);

        return buildResult();
    }

    private NearestNeighborResult buildResult() {
        double nearestNeighborIndex = 0;
        double expectedMeanDist = 0;
        double standardError = 0;
        double zScore = 0;
        double pValue = 0;

        if (studyArea <= 0) {
            expectedMeanDist = 0.0;
            nearestNeighborIndex = 0.0;
            standardError = 0.0;
            zScore = 0.0;
            pValue = 0.0;
        } else {
            // ArcGIS & CrimeStat
            // expectedMeanDist = 1.0 / (2.0 * ((N / studyArea)**0.5))
            expectedMeanDist = 0.5 * Math.sqrt(studyArea / featureCount);
            nearestNeighborIndex = observedMeanDist / expectedMeanDist;
            // double variance = (1.0 / Math.PI - 0.25) * studyArea / Math.pow(N, 2.0);
            // standardError = 0.26136 / ((N**2.0 / studyArea)**0.5)
            standardError = Math.sqrt(((4 - Math.PI) * studyArea)
                    / (4 * Math.PI * featureCount * featureCount));
            zScore = (observedMeanDist - expectedMeanDist) / standardError;
            pValue = SSUtils.zProb(zScore, StatEnum.BOTH);
        }

        NearestNeighborResult nni = new NearestNeighborResult(typeName);
        nni.setObserved_Point_Count(featureCount);
        nni.setStudy_Area(FormatUtils.round(studyArea));
        nni.setObserved_Mean_Distance(FormatUtils.round(observedMeanDist));
        nni.setExpected_Mean_Distance(FormatUtils.round(expectedMeanDist));
        nni.setNearest_Neighbor_Ratio(FormatUtils.round(nearestNeighborIndex));
        nni.setZ_Score(FormatUtils.round(zScore));
        nni.setP_Value(FormatUtils.round(pValue));
        nni.setStandard_Error(FormatUtils.round(standardError));

        return nni;
    }

    public static final class NearestNeighborResult {

        String typeName;

        int observed_Point_Count = 0;

        double study_Area = 0;

        double observed_Mean_Distance = 0;

        double expected_Mean_Distance = 0;

        double nearest_Neighbor_Ratio = 0;

        double z_Score = 0;

        double p_Value = 0;

        double standard_Error = 0;

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public int getObserved_Point_Count() {
            return observed_Point_Count;
        }

        public void setObserved_Point_Count(int observed_Point_Count) {
            this.observed_Point_Count = observed_Point_Count;
        }

        public double getStudy_Area() {
            return study_Area;
        }

        public void setStudy_Area(double study_Area) {
            this.study_Area = study_Area;
        }

        public double getObserved_Mean_Distance() {
            return observed_Mean_Distance;
        }

        public void setObserved_Mean_Distance(double observed_Mean_Distance) {
            this.observed_Mean_Distance = observed_Mean_Distance;
        }

        public double getExpected_Mean_Distance() {
            return expected_Mean_Distance;
        }

        public void setExpected_Mean_Distance(double expected_Mean_Distance) {
            this.expected_Mean_Distance = expected_Mean_Distance;
        }

        public double getNearest_Neighbor_Ratio() {
            return nearest_Neighbor_Ratio;
        }

        public void setNearest_Neighbor_Ratio(double nearest_Neighbor_Ratio) {
            this.nearest_Neighbor_Ratio = nearest_Neighbor_Ratio;
        }

        public double getZ_Score() {
            return z_Score;
        }

        public void setZ_Score(double z_Score) {
            this.z_Score = z_Score;
        }

        public double getP_Value() {
            return p_Value;
        }

        public void setP_Value(double p_Value) {
            this.p_Value = p_Value;
        }

        public double getStandard_Error() {
            return standard_Error;
        }

        public void setStandard_Error(double standard_Error) {
            this.standard_Error = standard_Error;
        }

        public NearestNeighborResult(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public String toString() {
            final String separator = System.getProperty("line.separator");
            final DecimalFormat df = new DecimalFormat("##.######");

            StringBuffer sb = new StringBuffer();
            sb.append("|| Average Nearest Neighbor Summary").append(separator);
            sb.append("|| Observed Point Count: ").append(df.format(getObserved_Point_Count()))
                    .append(separator);
            sb.append("|| Study Area: ").append(df.format(getStudy_Area())).append(separator);
            sb.append("|| Observed Mean Distance: ").append(df.format(getObserved_Mean_Distance()))
                    .append(separator);
            sb.append("|| Expected Mean Distance: ").append(df.format(getExpected_Mean_Distance()))
                    .append(separator);
            sb.append("|| Nearest Neighbor Ratio: ").append(df.format(getNearest_Neighbor_Ratio()))
                    .append(separator);
            sb.append("|| Z-Score: ").append(df.format(getZ_Score())).append(separator);
            sb.append("|| p-value: ").append(df.format(getP_Value())).append(separator);
            sb.append("|| Standard Error: ").append(df.format(getStandard_Error()))
                    .append(separator);

            return sb.toString();
        }
    }

}
