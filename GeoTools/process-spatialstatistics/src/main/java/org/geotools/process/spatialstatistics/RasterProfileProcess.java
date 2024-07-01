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

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterFunctionalSurface;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/**
 * Creates a point features with z values interpolated from the input gridcoverage.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterProfileProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterProfileProcess.class);

    public RasterProfileProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(GridCoverage2D inputCoverage, Geometry userLine,
            Double interval, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterProfileProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterProfileProcessFactory.userLine.key, userLine);
        map.put(RasterProfileProcessFactory.interval.key, interval);

        Process process = new RasterProfileProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(RasterProfileProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterProfileProcessFactory.inputCoverage, null);
        Geometry userLine = (Geometry) Params.getValue(input, RasterProfileProcessFactory.userLine,
                RasterProfileProcessFactory.userLine.sample);
        Double interval = (Double) Params.getValue(input, RasterProfileProcessFactory.interval,
                null);

        if (inputCoverage == null || userLine == null) {
            throw new NullPointerException("inputCoverage, userLine parameters required");
        }

        // start process
        if (interval == null || interval <= 0) {
            interval = userLine.getLength() / 20; // default interval
        } else {
            interval = userLine.getLength() / interval;
        }

        CoordinateReferenceSystem crs = inputCoverage.getCoordinateReferenceSystem();
        userLine = transformGeometry(userLine, crs);

        RasterFunctionalSurface process = new RasterFunctionalSurface(inputCoverage);
        Geometry profileLine = process.getProfile(userLine, interval);

        // prepare feature type
        SimpleFeatureType featureType = FeatureTypes.getDefaultType("profile", Point.class, crs);
        featureType = FeatureTypes.add(featureType, "distance", Double.class, 38);
        featureType = FeatureTypes.add(featureType, "value", Double.class, 38);

        // create feature collection
        ListFeatureCollection resultSfc = new ListFeatureCollection(featureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

        Coordinate[] coords = profileLine.getCoordinates();
        int id = 0;
        double distance = 0;
        for (Coordinate coord : coords) {
            Point curPoint = userLine.getFactory().createPoint(coord);

            // create feature and set geometry
            String fid = featureType.getTypeName() + "." + (++id);
            SimpleFeature newFeature = builder.buildFeature(fid);
            newFeature.setDefaultGeometry(curPoint);
            newFeature.setAttribute("distance", distance);
            newFeature.setAttribute("value", coord.z);
            resultSfc.add(newFeature);
            distance += interval;
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterProfileProcessFactory.RESULT.key, resultSfc);
        return resultMap;
    }
}
