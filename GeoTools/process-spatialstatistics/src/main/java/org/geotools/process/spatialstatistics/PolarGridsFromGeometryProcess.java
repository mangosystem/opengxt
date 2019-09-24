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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.DistanceUnit;
import org.geotools.process.spatialstatistics.enumeration.RadialType;
import org.geotools.process.spatialstatistics.operations.PolarGridsOperation;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Creates a radial polar grids from geometry(centroid).
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PolarGridsFromGeometryProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(PolarGridsFromGeometryProcess.class);

    public PolarGridsFromGeometryProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(Geometry origin, String radius,
            RadialType radialType, Integer sides, ProgressListener monitor) {
        return process(origin, null, radius, DistanceUnit.Default, radialType, sides, monitor);
    }

    public static SimpleFeatureCollection process(Geometry origin,
            CoordinateReferenceSystem forcedCRS, String radius, DistanceUnit radiusUnit,
            RadialType radialType, Integer sides, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PolarGridsFromGeometryProcessFactory.origin.key, origin);
        map.put(PolarGridsFromGeometryProcessFactory.forcedCRS.key, forcedCRS);
        map.put(PolarGridsFromGeometryProcessFactory.radius.key, radius);
        map.put(PolarGridsFromGeometryProcessFactory.radiusUnit.key, radiusUnit);
        map.put(PolarGridsFromGeometryProcessFactory.radialType.key, radialType);
        map.put(PolarGridsFromGeometryProcessFactory.sides.key, sides);

        Process process = new PolarGridsFromGeometryProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap
                    .get(PolarGridsFromGeometryProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        Geometry origin = (Geometry) Params.getValue(input,
                PolarGridsFromGeometryProcessFactory.origin, null);
        String radius = (String) Params.getValue(input,
                PolarGridsFromGeometryProcessFactory.radius, null);
        if (origin == null || radius == null || radius.length() == 0) {
            throw new NullPointerException("origin, radius parameters required");
        }
        CoordinateReferenceSystem forcedCRS = (CoordinateReferenceSystem) Params.getValue(input,
                PolarGridsFromGeometryProcessFactory.forcedCRS, null);
        DistanceUnit radiusUnit = (DistanceUnit) Params.getValue(input,
                PolarGridsFromGeometryProcessFactory.radiusUnit,
                PolarGridsFromGeometryProcessFactory.radiusUnit.sample);

        RadialType radialType = (RadialType) Params.getValue(input,
                PolarGridsFromGeometryProcessFactory.radialType,
                PolarGridsFromGeometryProcessFactory.radialType.sample);
        Integer sides = (Integer) Params.getValue(input,
                PolarGridsFromGeometryProcessFactory.sides,
                PolarGridsFromGeometryProcessFactory.sides.sample);

        // start process
        String[] arrDistance = radius.split(",");
        double[] bufferRadius = new double[arrDistance.length];
        for (int k = 0; k < arrDistance.length; k++) {
            try {
                bufferRadius[k] = Double.parseDouble(arrDistance[k].trim());
            } catch (NumberFormatException nfe) {
                throw new ProcessException(nfe);
            }
        }

        SimpleFeatureCollection resultFc = null;
        try {
            PolarGridsOperation operation = new PolarGridsOperation();
            resultFc = operation.execute(origin, forcedCRS, bufferRadius, radiusUnit, sides,
                    radialType);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(PolarGridsFromGeometryProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
