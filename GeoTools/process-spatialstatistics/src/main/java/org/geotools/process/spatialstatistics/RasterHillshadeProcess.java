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

import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHillshadeOperation;
import org.geotools.util.logging.Logging;

/**
 * Creates a shaded relief from a surface raster by considering the illumination source angle and shadows.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterHillshadeProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterHillshadeProcess.class);

    public RasterHillshadeProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(GridCoverage2D inputCoverage, Double azimuth,
            Double altitude, Double zFactor, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterHillshadeProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterHillshadeProcessFactory.azimuth.key, azimuth);
        map.put(RasterHillshadeProcessFactory.altitude.key, altitude);
        map.put(RasterHillshadeProcessFactory.zFactor.key, zFactor);

        Process process = new RasterHillshadeProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(RasterHillshadeProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                RasterHillshadeProcessFactory.inputCoverage, null);
        if (inputCoverage == null) {
            throw new NullPointerException("inputCoverage parameter required");
        }

        Double azimuth = (Double) Params.getValue(input, RasterHillshadeProcessFactory.azimuth,
                RasterHillshadeProcessFactory.azimuth.sample);

        Double altitude = (Double) Params.getValue(input, RasterHillshadeProcessFactory.altitude,
                RasterHillshadeProcessFactory.altitude.sample);

        Double zFactor = (Double) Params.getValue(input, RasterHillshadeProcessFactory.zFactor,
                RasterHillshadeProcessFactory.zFactor.sample);

        // start process
        RasterHillshadeOperation process = new RasterHillshadeOperation();
        GridCoverage2D extractedGC = process.execute(inputCoverage, azimuth, altitude, zFactor);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RasterHillshadeProcessFactory.RESULT.key, extractedGC);
        return resultMap;
    }
}
