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
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation;
import org.geotools.process.spatialstatistics.operations.DataStatisticsOperation.DataStatisticsResult;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Calculates summary statistics(Sum, Minimum, Maximum, Mean, Standard Deviation etc.) in a gridcoverage
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class StatisticsGridCoverageProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(StatisticsGridCoverageProcess.class);

    private boolean started = false;

    public StatisticsGridCoverageProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static DataStatisticsResult process(GridCoverage2D inputCoverage, Geometry cropShape,
            Integer bandIndex, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(StatisticsGridCoverageProcessFactory.inputCoverage.key, inputCoverage);
        map.put(StatisticsGridCoverageProcessFactory.cropShape.key, cropShape);
        map.put(StatisticsGridCoverageProcessFactory.bandIndex.key, bandIndex);

        Process process = new StatisticsGridCoverageProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (DataStatisticsResult) resultMap
                    .get(StatisticsGridCoverageProcessFactory.RESULT.key);
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

            GridCoverage2D inputCoverage = (GridCoverage2D) Params.getValue(input,
                    StatisticsGridCoverageProcessFactory.inputCoverage, null);
            if (inputCoverage == null) {
                throw new NullPointerException("inputCoverage parameter required");
            }

            Geometry cropShape = (Geometry) Params.getValue(input,
                    StatisticsGridCoverageProcessFactory.cropShape, null);
            Integer bandIndex = (Integer) Params.getValue(input,
                    StatisticsGridCoverageProcessFactory.bandIndex,
                    StatisticsGridCoverageProcessFactory.bandIndex.sample);

            monitor.setTask(Text.text("Processing ..."));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            DataStatisticsOperation operator = new DataStatisticsOperation();
            DataStatisticsResult result = operator.execute(inputCoverage, cropShape, bandIndex);
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(StatisticsGridCoverageProcessFactory.RESULT.key, result);
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
