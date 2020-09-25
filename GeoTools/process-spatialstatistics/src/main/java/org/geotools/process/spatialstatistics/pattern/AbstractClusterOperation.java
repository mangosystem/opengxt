/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.operations.GeneralOperation;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Spatial Cluster Detection: Openshaw's Geographical Analysis Machine(GAM)
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @see https://www.r-bloggers.com/mapping-hotspots-with-r-the-gam/
 * @see https://github.com/ianturton/spatial-cluster-detection
 * 
 * @source $URL$
 * 
 */
public abstract class AbstractClusterOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(AbstractClusterOperation.class);

    public enum FitnessFunctionType {
        /**
         * Poisson Probability fitness function
         */
        Poisson,

        /**
         * Relative fitness function
         */
        Relative,

        /**
         * RelativePercent fitness function
         */
        RelativePercent
    }

    // Fitness function
    protected FitnessFunctionType functionType = FitnessFunctionType.Poisson;

    // Significance threshold
    protected double threshold = 0.01; // default = 0.01

    protected ReferencedEnvelope extent = null;

    // density of overall dataset
    protected double density; // caseSum / popSum

    protected STRtree popIndex;

    protected STRtree caseIndex;

    protected SimpleFeatureCollection outFeatures;

    protected GridCoverage2D outRaster;

    public AbstractClusterOperation() {

    }

    public FitnessFunctionType getFunctionType() {
        return functionType;
    }

    public void setFunctionType(FitnessFunctionType functionType) {
        this.functionType = functionType;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public GridCoverage2D getRaster() {
        return outRaster;
    }

    protected void buildCircleDensityRaster(List<ClusterCircle> circles) {
        CirclesToDensityRaster toRaster = new CirclesToDensityRaster(extent);
        this.outRaster = toRaster.processCircles(circles);
    }

    protected void buildResult(List<ClusterCircle> circles, CoordinateReferenceSystem crs) {
        SimpleFeatureType schema = FeatureTypes.getDefaultType("gam", Polygon.class, crs);
        schema = FeatureTypes.add(schema, "radius", Double.class);
        schema = FeatureTypes.add(schema, "fitness", Double.class);
        schema = FeatureTypes.add(schema, "pop", Double.class);
        schema = FeatureTypes.add(schema, "expected", Double.class);
        schema = FeatureTypes.add(schema, "cases", Double.class);

        ListFeatureCollection outputFc = new ListFeatureCollection(schema);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);

        int serialID = 0;
        for (ClusterCircle circle : circles) {
            String fid = schema.getTypeName() + "." + (++serialID);

            SimpleFeature newFeature = builder.buildFeature(fid);
            newFeature.setDefaultGeometry(circle.getPolygon());

            newFeature.setAttribute("radius", circle.getRadius());
            newFeature.setAttribute("fitness", circle.getFitness());
            newFeature.setAttribute("pop", circle.getPopulation());
            newFeature.setAttribute("expected", circle.getExpected());
            newFeature.setAttribute("cases", circle.getCases());

            outputFc.add(newFeature);
        }

        this.outFeatures = outputFc;
    }

    protected void preEvaluate(SimpleFeatureCollection popFeatures, Expression popField,
            SimpleFeatureCollection caseFeatures, Expression caseField) {
        // 1. Population
        popIndex = new STRtree();

        double sumPop = 0d;
        SimpleFeatureIterator featureIter = popFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                double value = getEvaluated(popField.evaluate(feature));
                if (value <= 0) {
                    continue;
                }

                sumPop += value;

                NearFeature item = new NearFeature(feature.getID(), geometry, value);
                popIndex.insert(geometry.getEnvelopeInternal(), item);
            }
        } finally {
            featureIter.close();
        }

        // 2. Cases
        caseIndex = new STRtree();

        double sumCases = 0d;
        featureIter = caseFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                double value = getEvaluated(caseField.evaluate(feature));
                if (value <= 0) {
                    continue;
                }

                sumCases += value;

                NearFeature item = new NearFeature(feature.getID(), geometry, value);
                caseIndex.insert(geometry.getEnvelopeInternal(), item);
            }
        } finally {
            featureIter.close();
        }

        this.density = sumPop == 0 ? 0 : sumCases / sumPop;
    }

    protected double getEvaluated(Object evaluated) {
        double value = 0d;
        try {
            if (evaluated != null) {
                value = Double.valueOf(evaluated.toString());
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    value = 0;
                }
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
        }
        return value;
    }

    static final class NearFeature {

        private String id;

        private Geometry geometry;

        private double value = 0d;

        public String getId() {
            return id;
        }

        public double getX() {
            return geometry.getCentroid().getX();
        }

        public double getY() {
            return geometry.getCentroid().getY();
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public double getValue() {
            return value;
        }

        public NearFeature(String id, Geometry geometry, double value) {
            this.id = id;
            this.geometry = geometry;
            this.value = value;
        }

        public double distance(NearFeature other) {
            return this.geometry.distance(other.getGeometry());
        }
    }

}