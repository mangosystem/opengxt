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
import org.geotools.grid.hexagon.HexagonOrientation;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.HexagonOperation;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates hexagon grids from extent or bounds source features
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HexagonProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(HexagonProcess.class);

    private boolean started = false;

    public HexagonProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(ReferencedEnvelope extent,
            SimpleFeatureCollection boundsSource, Double sideLen, HexagonOrientation orientation,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(HexagonProcessFactory.extent.key, extent);
        map.put(HexagonProcessFactory.boundsSource.key, boundsSource);
        map.put(HexagonProcessFactory.sideLen.key, sideLen);
        map.put(HexagonProcessFactory.orientation.key, orientation);

        Process process = new HexagonProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(HexagonProcessFactory.RESULT.key);
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

        if (monitor == null)
            monitor = new NullProgressListener();
        try {
            monitor.started();
            monitor.setTask(Text.text("Grabbing arguments"));
            monitor.progress(10.0f);

            ReferencedEnvelope gridBounds = (ReferencedEnvelope) Params.getValue(input,
                    HexagonProcessFactory.extent, null);
            SimpleFeatureCollection boundsSource = (SimpleFeatureCollection) Params.getValue(input,
                    HexagonProcessFactory.boundsSource, null);
            if (gridBounds == null) {
                throw new NullPointerException("extent parameters required");
            }

            Double sideLen = (Double) Params.getValue(input, HexagonProcessFactory.sideLen, null);
            HexagonOrientation orientation = (HexagonOrientation) Params.getValue(input,
                    HexagonProcessFactory.orientation, HexagonProcessFactory.orientation.sample);
            if (sideLen == null || sideLen == 0) {
                throw new NullPointerException("sideLen parameter should be grater than 0");
            }

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            HexagonOperation operation = new HexagonOperation();
            operation.setBoundsSource(boundsSource);
            operation.setOrientation(orientation);
            SimpleFeatureCollection resultFc = operation.execute(gridBounds, sideLen);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(HexagonProcessFactory.RESULT.key, resultFc);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            return null;
        } finally {
            monitor.dispose();
            started = false;
        }
    }
}
