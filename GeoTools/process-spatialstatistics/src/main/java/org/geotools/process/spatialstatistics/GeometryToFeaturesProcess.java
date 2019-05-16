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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * Converts a geometry to features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GeometryToFeaturesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(GeometryToFeaturesProcess.class);

    public GeometryToFeaturesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static Double process(Geometry geometry, CoordinateReferenceSystem crs, String typeName,
            ProgressListener monitor) {
        return GeometryToFeaturesProcess.process(geometry, crs, typeName, Boolean.FALSE, monitor);
    }

    public static Double process(Geometry geometry, CoordinateReferenceSystem crs, String typeName,
            Boolean singlePart, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(GeometryToFeaturesProcessFactory.geometry.key, geometry);
        map.put(GeometryToFeaturesProcessFactory.crs.key, crs);
        map.put(GeometryToFeaturesProcessFactory.typeName.key, typeName);
        map.put(GeometryToFeaturesProcessFactory.singlePart.key, singlePart);

        Process process = new GeometryToFeaturesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (Double) resultMap.get(GeometryToFeaturesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return new Double(0.0d);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        Geometry geometry = (Geometry) Params.getValue(input,
                GeometryToFeaturesProcessFactory.geometry, null);
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) Params.getValue(input,
                GeometryToFeaturesProcessFactory.crs, null);
        String typeName = (String) Params.getValue(input,
                GeometryToFeaturesProcessFactory.typeName,
                GeometryToFeaturesProcessFactory.typeName.sample);
        Boolean singlePart = (Boolean) Params.getValue(input,
                GeometryToFeaturesProcessFactory.singlePart,
                GeometryToFeaturesProcessFactory.singlePart.sample);
        if (geometry == null) {
            throw new NullPointerException("geometry parameter required");
        }

        // start process
        // get the crs
        if (crs == null && geometry.getUserData() instanceof CoordinateReferenceSystem) {
            try {
                crs = (CoordinateReferenceSystem) geometry.getUserData();
            } catch (Exception e) {
                // may not have a CRS attached
            }
        }

        if (crs == null && geometry.getSRID() > 0) {
            try {
                crs = CRS.decode("EPSG:" + geometry.getSRID());
            } catch (Exception e) {
                // may not have a CRS attached
            }
        }

        // build the feature type
        Class<?> geometryBinding = geometry.getClass();
        Class<?> targetGeometryBinding = geometry.getClass();

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setNamespaceURI(FeatureTypes.NAMESPACE_URL);
        typeBuilder.setName(typeName);
        typeBuilder.setCRS(crs);

        typeBuilder.add("gid", Integer.class);

        if (geometryBinding.isAssignableFrom(Polygon.class)) {
            targetGeometryBinding = MultiPolygon.class;
        } else if (geometryBinding.isAssignableFrom(LineString.class)) {
            targetGeometryBinding = MultiLineString.class;
        } else if (singlePart && geometryBinding.isAssignableFrom(MultiPoint.class)) {
            targetGeometryBinding = Point.class;
        }

        // create shape field
        typeBuilder.add(FeatureTypes.SHAPE_FIELD, targetGeometryBinding, crs);
        SimpleFeatureType schema = typeBuilder.buildFeatureType();

        ListFeatureCollection result = new ListFeatureCollection(schema);

        // build the feature
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);
        if (singlePart
                && (MultiPolygon.class.equals(geometryBinding)
                        || MultiLineString.class.equals(geometryBinding) || MultiPoint.class
                            .equals(geometryBinding))) {

            for (int index = 1; index <= geometry.getNumGeometries(); index++) {
                Geometry part = geometry.getGeometryN(index - 1);

                builder.addAll(new Object[] { index, part });
                result.add(builder.buildFeature(typeName + "." + index));
                builder.reset();
            }
        } else {
            builder.addAll(new Object[] { 1, geometry });
            result.add(builder.buildFeature(typeName + ".1"));
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(GeometryToFeaturesProcessFactory.RESULT.key, result);
        return resultMap;
    }
}
