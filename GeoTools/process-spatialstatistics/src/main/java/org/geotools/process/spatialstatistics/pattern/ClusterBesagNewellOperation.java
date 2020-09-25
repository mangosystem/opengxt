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
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.process.spatialstatistics.core.KnnSearch;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Spatial Cluster Detection: Besag, Julian and Newell, James
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @reference https://github.com/ianturton/spatial-cluster-detection
 * 
 * @source $URL$
 * 
 */
public class ClusterBesagNewellOperation extends AbstractClusterOperation {
    protected static final Logger LOGGER = Logging.getLogger(ClusterBesagNewellOperation.class);

    static final int DEFAULT_NEIGHBOURS = 10;

    public ClusterBesagNewellOperation() {

    }

    public GridCoverage2D getRaster() {
        return this.outRaster;
    }

    public SimpleFeatureCollection execute(SimpleFeatureCollection popFeatures, String popField,
            SimpleFeatureCollection caseFeatures, String caseField, int neighbours)
            throws IOException {
        return execute(popFeatures, ff.property(popField), caseFeatures, ff.property(caseField),
                neighbours);
    }

    /**
     * @param popFeatures Features containing the population at risk
     * @param popField The feature attribute with the population at risk
     * @param caseFeatures Features containing the incidents
     * @param caseField The feature attribute with the incidents
     * @param neighbours The Number of neighbours to be considered
     * @return The Significant Circles
     * @throws IOException
     */
    public SimpleFeatureCollection execute(SimpleFeatureCollection popFeatures, Expression popField,
            SimpleFeatureCollection caseFeatures, Expression caseField, int neighbours)
            throws IOException {
        CoordinateReferenceSystem crs = popFeatures.getSchema().getCoordinateReferenceSystem();
        this.extent = popFeatures.getBounds();

        if (neighbours <= 0) {
            neighbours = DEFAULT_NEIGHBOURS;
            LOGGER.log(Level.WARNING,
                    "The neighbors value must be greater than 0. The default is 10.");
        }

        FitnessFunction fitness = new FitnessFunction(functionType, threshold);

        // pre calculation
        this.preEvaluate(popFeatures, popField, caseFeatures, caseField);

        List<ClusterCircle> circles = new ArrayList<ClusterCircle>();
        KnnSearch knnSearch = new KnnSearch(this.caseIndex);

        SimpleFeatureIterator featureIter = caseFeatures.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Envelope envelope = geometry.getEnvelopeInternal();

                double value = getEvaluated(popField.evaluate(feature));
                if (value <= 0) {
                    continue;
                }

                // find K nearest neighbours
                NearFeature start = new NearFeature(feature.getID(), geometry, value);
                Object[] knns = knnSearch.kNearestNeighbour(envelope, start, new ItemDistance() {
                    @Override
                    public double distance(ItemBoundable item1, ItemBoundable item2) {
                        NearFeature s1 = (NearFeature) item1.getItem();
                        NearFeature s2 = (NearFeature) item2.getItem();
                        if (s1.getId().equals(s2.getId())) {
                            return Double.MAX_VALUE;
                        }
                        return s1.distance(s2);
                    }
                }, neighbours);

                // construct circle that includes those neighbours
                double radius = Double.MIN_VALUE;
                double population = 0;
                double expected = 0;
                double cases = 0;

                for (Object object : knns) {
                    NearFeature nearest = (NearFeature) object;
                    radius = Math.max(radius, start.distance(nearest));
                    cases += nearest.getValue();
                }

                ClusterCircle circle = new ClusterCircle(start.getX(), start.getY(), radius);
                Geometry circlePolygon = circle.getPolygon();
                PreparedGeometry prepared = PreparedGeometryFactory.prepare(circlePolygon);

                // get population and expected value in circle
                for (@SuppressWarnings("unchecked")
                Iterator<NearFeature> iter = (Iterator<NearFeature>) popIndex
                        .query(circle.getBounds()).iterator(); iter.hasNext();) {
                    NearFeature sample = iter.next();
                    if (prepared.intersects(sample.getGeometry())) {
                        population += sample.getValue();
                        expected += sample.getValue() * density;
                    }
                }

                // Poisson test
                if (fitness.isWorthTesting(expected, cases)) {
                    double stats = fitness.getStat(expected, cases);
                    if (!Double.isNaN(stats)) {
                        circle.setFitness(stats);
                        circle.setPopulation(population);
                        circle.setCases(cases);
                        circle.setExpected(expected);
                        circles.add(circle);
                    }
                }

            }
        } finally {
            featureIter.close();
        }

        // finally, build result
        this.buildResult(circles, crs);
        
        
        this.buildCircleDensityRaster(circles);

        return outFeatures;
    }

}