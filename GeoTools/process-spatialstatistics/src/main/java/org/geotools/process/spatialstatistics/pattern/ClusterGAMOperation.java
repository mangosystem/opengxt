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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Spatial Cluster Detection: Openshaw's Geographical Analysis Machine(GAM)
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @reference https://www.r-bloggers.com/mapping-hotspots-with-r-the-gam/
 * @reference https://github.com/ianturton/spatial-cluster-detection
 * 
 * @source $URL$
 * 
 */
public class ClusterGAMOperation extends AbstractClusterOperation {
    protected static final Logger LOGGER = Logging.getLogger(ClusterGAMOperation.class);

    // Circle overlap
    private double overlapRatio = 0.5; // default

    public ClusterGAMOperation() {

    }

    public void setOverlapRatio(double overlapRatio) {
        this.overlapRatio = overlapRatio < 0 ? 0 : overlapRatio;
        this.overlapRatio = this.overlapRatio > 1 ? 1 : this.overlapRatio;
    }

    public GridCoverage2D getRaster() {
        return this.outRaster;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection popFeatures, String popField,
            SimpleFeatureCollection caseFeatures, String caseField) throws IOException {
        return execute(popFeatures, ff.property(popField), caseFeatures, ff.property(caseField));
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection popFeatures, Expression popField,
            SimpleFeatureCollection caseFeatures, Expression caseField) throws IOException {
        ReferencedEnvelope bounds = popFeatures.getBounds();

        double minRadius = Math.min(bounds.getWidth(), bounds.getHeight()) / 150.0;
        double maxRadius = minRadius * 5;

        return execute(popFeatures, popField, caseFeatures, caseField, minRadius, maxRadius);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection popFeatures, String popField,
            SimpleFeatureCollection caseFeatures, String caseField, double minRadius,
            double maxRadius) throws IOException {
        return execute(popFeatures, ff.property(popField), caseFeatures, ff.property(caseField),
                minRadius, maxRadius);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection popFeatures, Expression popField,
            SimpleFeatureCollection caseFeatures, Expression caseField, double minRadius,
            double maxRadius) throws IOException {
        double radiusIncrement = minRadius / 2.0;

        return execute(popFeatures, popField, caseFeatures, caseField, minRadius, maxRadius,
                radiusIncrement);
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection popFeatures, String popField,
            SimpleFeatureCollection caseFeatures, String caseField, double minRadius,
            double maxRadius, double radiusIncrement) throws IOException {
        return execute(popFeatures, ff.property(popField), caseFeatures, ff.property(caseField),
                minRadius, maxRadius, radiusIncrement);
    }

    /**
     * 
     * @param popFeatures The features containing the population at risk
     * @param popField The feature attribute with the population at risk
     * @param caseFeatures The features containing the incidents
     * @param caseField The feature attribute with the incidents
     * @param minRadius The radius of the smallest circle
     * @param maxRadius The radius of the largest circle
     * @param radiusIncrement How much to change the size of the circles by
     * @return The Significant Circles
     * @throws IOException
     */
    public SimpleFeatureCollection execute(SimpleFeatureCollection popFeatures, Expression popField,
            SimpleFeatureCollection caseFeatures, Expression caseField, double minRadius,
            double maxRadius, double radiusIncrement) throws IOException {
        CoordinateReferenceSystem crs = popFeatures.getSchema().getCoordinateReferenceSystem();
        this.extent = popFeatures.getBounds();

        if (minRadius <= 0) {
            minRadius = Math.min(extent.getWidth(), extent.getHeight()) / 150.0;
            maxRadius = minRadius * 5;
            LOGGER.log(Level.WARNING,
                    "The minRadius parameter must be greater than 0. Set the minRadius parameter value to the default.");
        }

        if (maxRadius <= minRadius) {
            maxRadius = minRadius * 5;
        }

        if (radiusIncrement <= 0) {
            radiusIncrement = minRadius / 2.0;
        }

        FitnessFunction fitness = new FitnessFunction(functionType, threshold);

        // pre calculation
        this.preEvaluate(popFeatures, popField, caseFeatures, caseField);

        final double halfMaxRadius = maxRadius / 2.0;

        double minX = extent.getMinX() - halfMaxRadius;
        double minY = extent.getMinY() - halfMaxRadius;
        double maxX = extent.getMaxX() + halfMaxRadius;
        double maxY = extent.getMaxY() + halfMaxRadius;

        this.extent = new ReferencedEnvelope(minX, maxX, minY, maxY, crs);

        List<ClusterCircle> circles = new ArrayList<ClusterCircle>();

        for (double radius = minRadius; radius <= maxRadius; radius += radiusIncrement) {
            double step = radius * overlapRatio;
            for (double x = extent.getMinX(); x <= extent.getMaxX(); x += step) {
                for (double y = extent.getMinY(); y <= extent.getMaxY(); y += step) {

                    ClusterCircle circle = new ClusterCircle(x, y, radius);

                    Geometry circlePolygon = circle.getPolygon();
                    PreparedGeometry prepared = PreparedGeometryFactory.prepare(circlePolygon);

                    double population = 0;
                    double expected = 0;
                    double cases = 0;

                    // pop
                    for (@SuppressWarnings("unchecked")
                    Iterator<NearFeature> iter = (Iterator<NearFeature>) popIndex
                            .query(circle.getBounds()).iterator(); iter.hasNext();) {
                        NearFeature sample = iter.next();
                        if (prepared.intersects(sample.getGeometry())) {
                            population += sample.getValue();
                            expected += sample.getValue() * density;
                        }
                    }

                    // cases
                    for (@SuppressWarnings("unchecked")
                    Iterator<NearFeature> iter = (Iterator<NearFeature>) caseIndex
                            .query(circle.getBounds()).iterator(); iter.hasNext();) {
                        NearFeature sample = iter.next();
                        if (prepared.intersects(sample.getGeometry())) {
                            cases += sample.getValue();
                        }
                    }

                    // Poisson test
                    if (fitness.isWorthTesting(expected, cases)) {
                        double stats = fitness.getStat(expected, cases);
                        if (!Double.isNaN(stats)) {
                            circle.setFitness(stats);
                            circle.setPopulation(population);
                            circle.setExpected(expected);
                            circle.setCases(cases);
                            circles.add(circle);
                        }
                    }
                } // y
            } // x
        } // radius

        // finally, build result
        this.buildResult(circles, crs);

        this.buildCircleDensityRaster(circles);

        return outFeatures;
    }
}