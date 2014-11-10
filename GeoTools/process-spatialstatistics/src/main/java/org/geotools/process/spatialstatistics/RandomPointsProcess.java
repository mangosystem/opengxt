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
package org.geotools.process.spatialstatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.GeoTools;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.impl.AbstractProcess;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;
import com.vividsolutions.jts.shape.random.RandomPointsBuilder;

/**
 * Calculates area values for each feature in a polygon features.
 * 
 * @author Minpa Lee, MangoSystem RandomShapeFactory
 * 
 * @source $URL$
 */
public class RandomPointsProcess extends AbstractProcess {
    protected static final Logger LOGGER = Logging.getLogger(RandomPointsProcess.class);

    private boolean started = false;

    public RandomPointsProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(Integer pointCount, ReferencedEnvelope extent,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RandomPointsProcessFactory.pointCount.key, pointCount);
        map.put(RandomPointsProcessFactory.extent.key, extent);

        Process process = new RandomPointsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(RandomPointsProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    public static SimpleFeatureCollection process(Integer pointCount,
            SimpleFeatureCollection inputFeatures, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RandomPointsProcessFactory.pointCount.key, pointCount);
        map.put(RandomPointsProcessFactory.polygonFeatures.key, inputFeatures);

        Process process = new RandomPointsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(RandomPointsProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        if (monitor == null)
            monitor = new NullProgressListener();
        try {
            monitor.started();
            monitor.setTask(Text.text("Grabbing arguments"));
            monitor.progress(10.0f);

            int pointCount = (Integer) Params.getValue(input,
                    RandomPointsProcessFactory.pointCount,
                    RandomPointsProcessFactory.pointCount.sample);
            if (pointCount < 1) {
                throw new NullPointerException("Point count must be greater than 1");
            }

            SimpleFeatureCollection polygonFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, RandomPointsProcessFactory.polygonFeatures, null);
            ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                    RandomPointsProcessFactory.extent, null);
            if (polygonFeatures == null && extent == null) {
                throw new NullPointerException("extent or polygonFeatures parameters required");
            }

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            RandomPoints operator = new RandomPoints(extent, polygonFeatures);
            SimpleFeatureCollection randomPoints = operator.getRandomFeatures(pointCount);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(AreaProcessFactory.RESULT.key, randomPoints);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
        }
    }

    static class RandomPoints {
        final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(GeoTools.getDefaultHints());

        com.vividsolutions.jts.shape.random.RandomPointsBuilder builder;

        CoordinateReferenceSystem crs;

        public RandomPoints(ReferencedEnvelope extent, SimpleFeatureCollection inputFeatures) {
            this.builder = new RandomPointsBuilder(gf);
            if (inputFeatures == null) {
                this.crs = extent.getCoordinateReferenceSystem();
                this.builder.setExtent(extent);
            } else {
                this.crs = inputFeatures.getSchema().getCoordinateReferenceSystem();
                Geometry maskPoly = unionFeatures(inputFeatures);
                if (maskPoly == null || maskPoly.isEmpty()) {
                    this.builder.setExtent(inputFeatures.getBounds());
                    LOGGER.log(Level.WARNING,
                            "Failed to create mask polygon, random points builder will use feature's boundary");
                } else {
                    this.builder.setExtent(maskPoly);
                }
            }
        }

        public SimpleFeatureCollection getRandomFeatures(Integer pointCount) {
            this.builder.setNumPoints(pointCount);

            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.setName("RandomPoints");
            typeBuilder.add("geom", Point.class, this.crs);
            typeBuilder.add("weight", Integer.class);
            SimpleFeatureType schema = typeBuilder.buildFeatureType();

            ListFeatureCollection randomFeatures = new ListFeatureCollection(schema);

            Geometry multiPoints = this.builder.getGeometry();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);
            for (int i = 0; i < multiPoints.getNumGeometries(); i++) {
                Point point = (Point) multiPoints.getGeometryN(i);
                String featureID = String.valueOf("RandomPoints." + (i + 1));
                
                builder.reset();
                builder.addAll(new Object[] { point, 1 });
                randomFeatures.add(builder.buildFeature(featureID));
            }
            return randomFeatures;
        }

        private Geometry unionFeatures(SimpleFeatureCollection inputFeatures) {
            List<Geometry> geometries = new ArrayList<Geometry>();
            SimpleFeatureIterator featureIter = null;
            try {
                featureIter = inputFeatures.features();
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    if (geometry == null || geometry.isEmpty()) {
                        continue;
                    }
                    geometries.add(geometry);
                }
            } finally {
                featureIter.close();
            }

            if (geometries.size() == 0) {
                return null;
            } else if (geometries.size() == 1) {
                return geometries.iterator().next();
            }

            com.vividsolutions.jts.operation.union.CascadedPolygonUnion unionOp = null;
            unionOp = new CascadedPolygonUnion(geometries);
            return unionOp.union();
        }
    }
}
