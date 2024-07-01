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
package org.geotools.process.spatialstatistics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.RegularPointsOperation;
import org.geotools.process.spatialstatistics.operations.RegularPointsOperation.SizeUnit;
import org.geotools.util.logging.Logging;

/**
 * Creates a regular point features within extent and boundary sources.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class RegularPointsProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(RegularPointsProcess.class);

    public RegularPointsProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(ReferencedEnvelope extent,
            SimpleFeatureCollection boundsSource, SizeUnit unit, Double width, Double height,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RegularPointsProcessFactory.extent.key, extent);
        map.put(RegularPointsProcessFactory.boundsSource.key, boundsSource);
        map.put(RegularPointsProcessFactory.unit.key, unit);
        map.put(RegularPointsProcessFactory.width.key, width);
        map.put(RegularPointsProcessFactory.height.key, height);

        Process process = new RegularPointsProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(RegularPointsProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                RegularPointsProcessFactory.extent, null);
        Double width = (Double) Params.getValue(input, RegularPointsProcessFactory.width,
                RegularPointsProcessFactory.width.sample);
        Double height = (Double) Params.getValue(input, RegularPointsProcessFactory.height,
                RegularPointsProcessFactory.height.sample);
        if (extent == null || width == null || height == null || width <= 0 || height <= 0) {
            throw new NullPointerException("extent, width, height parameters required");
        }

        SizeUnit unit = (SizeUnit) Params.getValue(input, RegularPointsProcessFactory.unit,
                RegularPointsProcessFactory.unit.sample);

        SimpleFeatureCollection boundsSource = (SimpleFeatureCollection) Params.getValue(input,
                RegularPointsProcessFactory.boundsSource, null);

        // start process
        SimpleFeatureCollection resultFc = null;
        try {
            RegularPointsOperation operation = new RegularPointsOperation();
            operation.setBoundsSource(boundsSource);
            resultFc = operation.execute(extent, unit, width, height);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(RegularPointsProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
