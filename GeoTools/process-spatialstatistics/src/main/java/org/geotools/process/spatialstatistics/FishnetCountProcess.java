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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.FishnetType;
import org.geotools.process.spatialstatistics.operations.FishnetOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates a fishnet of rectangular cell polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FishnetCountProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(FishnetCountProcess.class);

    private boolean started = false;

    public FishnetCountProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(ReferencedEnvelope extent,
            SimpleFeatureCollection boundsSource, Boolean boundaryInside, Integer columns,
            Integer rows, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(FishnetCountProcessFactory.extent.key, extent);
        map.put(FishnetCountProcessFactory.boundsSource.key, boundsSource);
        map.put(FishnetCountProcessFactory.boundaryInside.key, boundaryInside);
        map.put(FishnetCountProcessFactory.columns.key, columns);
        map.put(FishnetCountProcessFactory.rows.key, rows);

        Process process = new FishnetCountProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(FishnetCountProcessFactory.RESULT.key);
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
            ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                    FishnetCountProcessFactory.extent, null);
            Integer columns = (Integer) Params.getValue(input, FishnetCountProcessFactory.columns,
                    FishnetCountProcessFactory.columns.sample);
            Integer rows = (Integer) Params.getValue(input, FishnetCountProcessFactory.rows,
                    FishnetCountProcessFactory.rows.sample);
            if (extent == null || columns == null || columns == 0 || rows == null || rows == 0) {
                throw new NullPointerException("extent, columns, rows parameter required");
            }

            SimpleFeatureCollection boundsSource = (SimpleFeatureCollection) Params.getValue(input,
                    FishnetCountProcessFactory.boundsSource, null);
            Boolean boundaryInside = (Boolean) Params.getValue(input,
                    FishnetCountProcessFactory.boundaryInside,
                    FishnetCountProcessFactory.boundaryInside.sample);

            // start process
            FishnetOperation operation = new FishnetOperation();
            operation.setBoundaryInside(boundaryInside);
            operation.setFishnetType(FishnetType.Rectangle);
            operation.setBoundsSource(boundsSource);
            SimpleFeatureCollection resultFc = operation.execute(extent, columns, rows);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(FishnetCountProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
