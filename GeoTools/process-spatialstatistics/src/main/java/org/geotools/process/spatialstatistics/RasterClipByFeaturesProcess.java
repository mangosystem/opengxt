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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterClipOperation;
import org.geotools.process.spatialstatistics.transformation.ReprojectFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Extracts the subset of a raster based on a polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterClipByFeaturesProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterClipByFeaturesProcess.class);

    public RasterClipByFeaturesProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage,
            SimpleFeatureCollection cropFeatures, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterClipByFeaturesProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterClipByFeaturesProcessFactory.cropFeatures.key, cropFeatures);

        Process process = new RasterClipByFeaturesProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterClipByFeaturesProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterClipByFeaturesProcessFactory.inputCoverage, null);
        SimpleFeatureCollection cropFeatures = (SimpleFeatureCollection) Params.getValue(input,
                RasterClipByFeaturesProcessFactory.cropFeatures, null);
        if (inputCoverage == null || cropFeatures == null) {
            throw new NullPointerException("inputCoverage, cropFeatures parameters required");
        }

        SimpleFeatureType featureType = cropFeatures.getSchema();
        Class<?> binding = featureType.getGeometryDescriptor().getType().getBinding();
        if (!binding.isAssignableFrom(MultiPolygon.class)
                && !binding.isAssignableFrom(Polygon.class)) {
            throw new ProcessException("cropFeatures must be polygon features!");
        }

        // start process
        CoordinateReferenceSystem targetCRS = inputCoverage.getCoordinateReferenceSystem();
        Geometry cropShape = getGeometries(cropFeatures, targetCRS);

        GridCoverage2D cropedCoverage = inputCoverage;
        if (!cropShape.isEmpty()) {
            RasterClipOperation cropOperation = new RasterClipOperation();
            cropedCoverage = cropOperation.execute(inputCoverage, cropShape);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterClipByFeaturesProcessFactory.RESULT.key, cropedCoverage);
        return resultMap;
    }

    private Geometry getGeometries(SimpleFeatureCollection features,
            CoordinateReferenceSystem targetCRS) {
        CoordinateReferenceSystem sourceCRS = features.getSchema().getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            features = new ReprojectFeatureCollection(features, targetCRS);
        }

        List<Geometry> geomList = new ArrayList<Geometry>();
        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                geomList.add(geometry);
            }
        } finally {
            featureIter.close();
        }

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        Geometry coprGeometry = geometryFactory.buildGeometry(geomList);
        coprGeometry.setUserData(targetCRS);

        return coprGeometry;
    }
}
