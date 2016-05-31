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
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

/**
 * Calculates area values for each feature in a polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class UnionPolygonProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(UnionPolygonProcess.class);

    private boolean started = false;

    public UnionPolygonProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection polygonFeatures,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(UnionPolygonProcessFactory.polygonFeatures.key, polygonFeatures);

        Process process = new UnionPolygonProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(UnionPolygonProcessFactory.RESULT.key);
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

        try {
            SimpleFeatureCollection polygonFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, UnionPolygonProcessFactory.polygonFeatures, null);
            Boolean preserveHole = (Boolean) Params.getValue(input,
                    UnionPolygonProcessFactory.preserveHole,
                    UnionPolygonProcessFactory.preserveHole.sample);
            if (polygonFeatures == null) {
                throw new NullPointerException("polygonFeatures parameters required");
            }

            // start process
            List<Geometry> geometries = new ArrayList<Geometry>();
            SimpleFeatureIterator featureIter = null;
            try {
                featureIter = polygonFeatures.features();
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

            CascadedPolygonUnion unionOp = new CascadedPolygonUnion(geometries);
            Geometry unionGeometry = unionOp.union();
            if (preserveHole == Boolean.FALSE) {
                unionGeometry = removeHoles(unionGeometry);
            }

            CoordinateReferenceSystem crs = polygonFeatures.getSchema()
                    .getCoordinateReferenceSystem();
            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.setName("UnionPolygon");
            typeBuilder.add("geom", MultiPolygon.class, crs);
            SimpleFeatureType schema = typeBuilder.buildFeatureType();

            ListFeatureCollection unionFeatures = new ListFeatureCollection(schema);
            SimpleFeature feature = SimpleFeatureBuilder.build(schema,
                    new Object[] { unionGeometry }, "UnionPolygon.1");
            unionFeatures.add(feature);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(UnionPolygonProcessFactory.RESULT.key, unionFeatures);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }

    private Geometry removeHoles(Geometry inputPolygon) {
        Class<?> geomBinding = inputPolygon.getClass();

        Geometry finalGeom = inputPolygon;
        if (Polygon.class.equals(geomBinding)) {
            finalGeom = removeHoles((Polygon) inputPolygon);
        } else if (MultiPolygon.class.equals(geomBinding)) {
            List<Polygon> polygons = new ArrayList<Polygon>();
            for (int index = 0; index < inputPolygon.getNumGeometries(); index++) {
                Polygon polygon = (Polygon) inputPolygon.getGeometryN(index);
                polygons.add((Polygon) removeHoles(polygon));
            }

            finalGeom = inputPolygon.getFactory().createMultiPolygon(
                    GeometryFactory.toPolygonArray(polygons));
        }
        finalGeom.setUserData(inputPolygon.getUserData());
        return finalGeom;
    }

    private Geometry removeHoles(Polygon polygon) {
        GeometryFactory factory = polygon.getFactory();
        LineString exteriorRing = polygon.getExteriorRing();
        Geometry finalGeom = factory.createPolygon((LinearRing) exteriorRing, null);
        finalGeom.setUserData(polygon.getUserData());
        return finalGeom;
    }
}
