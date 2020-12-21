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
package org.geotools.process.spatialstatistics.gridcoverage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.ProcessException;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Abstract Raster CutFill Operation.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class AbstractRasterCutFillOperation extends RasterProcessingOperation {
    protected static final Logger LOGGER = Logging.getLogger(AbstractRasterCutFillOperation.class);

    protected GridCoverage2D cutFillRaster = null;

    public AbstractRasterCutFillOperation() {

    }

    public GridCoverage2D getCutFillRaster() {
        return cutFillRaster;
    }

    protected SimpleFeatureCollection buildFeatures(GridCoverage2D cutFillGc, CutFillResult result)
            throws ProcessException, IOException {
        RasterToPolygonOperation converter = new RasterToPolygonOperation();
        SimpleFeatureCollection features = converter.execute(cutFillGc, 0, false, "category");

        Map<Integer, List<Geometry>> map = buildMap(features);

        // create schema
        SimpleFeatureType schema = FeatureTypes.build(features.getSchema(), "CutFill");
        schema = FeatureTypes.add(schema, "count", Integer.class);
        schema = FeatureTypes.add(schema, "area", Double.class);
        schema = FeatureTypes.add(schema, "volume", Double.class);

        // create features
        ListFeatureCollection outputFc = new ListFeatureCollection(schema);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);

        int serialID = 0;
        for (Entry<Integer, List<Geometry>> entry : map.entrySet()) {
            Integer category = entry.getKey();
            List<Geometry> geometries = entry.getValue();
            if (geometries.size() == 0) {
                continue;
            }

            Geometry unionGeometry = null;
            try {
                CascadedPolygonUnion unionOp = new CascadedPolygonUnion(geometries);
                unionGeometry = unionOp.union();
            } catch (IllegalStateException e) {
                LOGGER.log(Level.WARNING, e.getMessage());
            }

            if (unionGeometry == null || unionGeometry.isEmpty()) {
                continue;
            }

            double area = result.getArea(category);
            double volume = result.getVolume(category);
            int count = result.getCount(category);

            String fid = schema.getTypeName() + "." + (++serialID);
            SimpleFeature newFeature = builder.buildFeature(fid);
            newFeature.setDefaultGeometry(unionGeometry);

            newFeature.setAttribute("category", category);
            newFeature.setAttribute("count", count);
            newFeature.setAttribute("area", area);
            newFeature.setAttribute("volume", volume);

            outputFc.add(newFeature);
        }

        return outputFc;
    }

    private Map<Integer, List<Geometry>> buildMap(SimpleFeatureCollection features) {
        Map<Integer, List<Geometry>> map = new TreeMap<Integer, List<Geometry>>();
        map.put(Integer.valueOf(-1), new ArrayList<Geometry>());
        map.put(Integer.valueOf(0), new ArrayList<Geometry>());
        map.put(Integer.valueOf(1), new ArrayList<Geometry>());

        if (features == null) {
            return map;
        }

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Object val = feature.getAttribute("category");
                Integer category = Converters.convert(val, Integer.class);

                if (val != null) {
                    map.get(category).add((Geometry) feature.getDefaultGeometry());
                }
            }
        } finally {
            featureIter.close();
        }

        return map;
    }

    protected static final class CutFillResult {
        public Double baseHeight = Double.NaN;

        public Double cutArea = Double.valueOf(0);

        public Double fillArea = Double.valueOf(0);

        public Double unChangedArea = Double.valueOf(0);

        public Double cutVolume = Double.valueOf(0);

        public Double fillVolume = Double.valueOf(0);

        public Integer cutCount = Integer.valueOf(0);

        public Integer fillCount = Integer.valueOf(0);

        public Integer unChangedCount = Integer.valueOf(0);

        public CutFillResult(Double baseHeight) {
            this.baseHeight = baseHeight;
        }

        public double getArea(int category) {
            if (category == -1) { // fill
                return fillArea;
            } else if (category == 1) { // cut
                return cutArea;
            } else { // unchanged
                return unChangedArea;
            }
        }

        public double getVolume(int category) {
            if (category == -1) { // fill
                return fillVolume;
            } else if (category == 1) { // cut
                return cutVolume;
            } else { // unchanged
                return 0d;
            }
        }

        public int getCount(int category) {
            if (category == -1) { // fill
                return fillCount;
            } else if (category == 1) { // cut
                return cutCount;
            } else { // unchanged
                return unChangedCount;
            }
        }
    }
}