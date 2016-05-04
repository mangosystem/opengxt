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
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates a fishnet of rectangular cell polygon features.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FishnetSizeProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(FishnetSizeProcess.class);

    private boolean started = false;

    public FishnetSizeProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(ReferencedEnvelope extent,
            SimpleFeatureCollection boundsSource, Boolean boundaryInside, Double width,
            Double height, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(FishnetSizeProcessFactory.extent.key, extent);
        map.put(FishnetSizeProcessFactory.boundsSource.key, boundsSource);
        map.put(FishnetSizeProcessFactory.boundaryInside.key, boundaryInside);
        map.put(FishnetSizeProcessFactory.width.key, width);
        map.put(FishnetSizeProcessFactory.height.key, height);

        Process process = new FishnetSizeProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(FishnetSizeProcessFactory.RESULT.key);
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

            ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                    FishnetSizeProcessFactory.extent, null);
            Double width = (Double) Params.getValue(input, FishnetSizeProcessFactory.width,
                    FishnetSizeProcessFactory.width.sample);
            Double height = (Double) Params.getValue(input, FishnetSizeProcessFactory.height,
                    FishnetSizeProcessFactory.height.sample);
            if (extent == null || width == null || height == null || width == 0 || height == 0) {
                throw new NullPointerException("extent, width, height parameters required");
            }

            SimpleFeatureCollection boundsSource = (SimpleFeatureCollection) Params.getValue(input,
                    FishnetSizeProcessFactory.boundsSource, null);
            Boolean boundaryInside = (Boolean) Params.getValue(input,
                    FishnetSizeProcessFactory.boundaryInside,
                    FishnetSizeProcessFactory.boundaryInside.sample);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            FishnetOperation operation = new FishnetOperation();
            operation.setBoundaryInside(boundaryInside);
            operation.setFishnetType(FishnetType.Rectangle);
            operation.setBoundsSource(boundsSource);
            SimpleFeatureCollection resultFc = operation.execute(extent, width, height);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(FishnetSizeProcessFactory.RESULT.key, resultFc);
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
