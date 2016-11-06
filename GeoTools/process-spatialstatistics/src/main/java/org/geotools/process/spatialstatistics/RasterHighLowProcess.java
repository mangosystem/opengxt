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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHighLowPointsOperation;
import org.geotools.process.spatialstatistics.gridcoverage.RasterHighLowPointsOperation.HighLowType;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Finds the highest or lowest points for a polygon geometry.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RasterHighLowProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RasterHighLowProcess.class);

    private boolean started = false;

    public RasterHighLowProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(GridCoverage2D inputCoverage, Integer bandIndex,
            Geometry cropShape, HighLowType valueType, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RasterHighLowProcessFactory.inputCoverage.key, inputCoverage);
        map.put(RasterHighLowProcessFactory.bandIndex.key, bandIndex);
        map.put(RasterHighLowProcessFactory.cropShape.key, cropShape);
        map.put(RasterHighLowProcessFactory.valueType.key, valueType);

        Process process = new RasterHighLowProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap.get(RasterHighLowProcessFactory.RESULT.key);
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
            GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                    RasterHighLowProcessFactory.inputCoverage, null);
            Integer bandIndex = (Integer) Params.getValue(input,
                    RasterHighLowProcessFactory.bandIndex,
                    RasterHighLowProcessFactory.bandIndex.sample);
            Geometry cropShape = (Geometry) Params.getValue(input,
                    RasterHighLowProcessFactory.cropShape, null);
            HighLowType valueType = (HighLowType) Params.getValue(input,
                    RasterHighLowProcessFactory.valueType,
                    RasterHighLowProcessFactory.valueType.sample);
            if (inputCoverage == null) {
                throw new NullPointerException("inputCoverage parameter required");
            }

            // start process
            RasterHighLowPointsOperation process = new RasterHighLowPointsOperation();
            SimpleFeatureCollection resultFc = process.execute(inputCoverage, bandIndex, cropShape,
                    valueType);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(RasterHighLowProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
