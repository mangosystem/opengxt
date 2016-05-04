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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.gridcoverage.RasterInterpolationTPSOperation;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Interpolates a surface from points using a Thin Plate Spline(TPS) interpolation technique.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class TPSProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(TPSProcess.class);

    private boolean started = false;

    public TPSProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(SimpleFeatureCollection inputFeatures, String inputField,
            Double cellSize, ReferencedEnvelope extent, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(TPSProcessFactory.inputFeatures.key, inputFeatures);
        map.put(TPSProcessFactory.inputField.key, inputField);

        map.put(TPSProcessFactory.cellSize.key, cellSize);
        map.put(TPSProcessFactory.extent.key, extent);

        Process process = new TPSProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(TPSProcessFactory.RESULT.key);
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

            SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, TPSProcessFactory.inputFeatures, null);
            String inputField = (String) Params.getValue(input, TPSProcessFactory.inputField, null);
            if (inputFeatures == null || inputField == null || inputField.trim().length() == 0) {
                throw new NullPointerException("inputFeatures, inputField parameters required");
            }

            inputField = FeatureTypes.validateProperty(inputFeatures.getSchema(), inputField);
            if (inputFeatures.getSchema().indexOf(inputField) == -1) {
                throw new NullPointerException(inputField + " does not exist!");
            }

            Double cellSize = (Double) Params.getValue(input, TPSProcessFactory.cellSize,
                    TPSProcessFactory.cellSize.sample);
            ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                    TPSProcessFactory.extent, null);

            monitor.setTask(Text.text("Processing " + this.getClass().getSimpleName()));
            monitor.progress(25.0f);

            if (monitor.isCanceled()) {
                return null; // user has canceled this operation
            }

            // start process
            ReferencedEnvelope boundingBox = inputFeatures.getBounds();
            if (extent != null) {
                boundingBox = extent;
            }

            // get default cell size from extent
            if (cellSize == 0.0) {
                cellSize = Math.min(boundingBox.getWidth(), boundingBox.getHeight()) / 250.0;
                LOGGER.warning("default cell size = " + cellSize);
            }

            GridCoverage2D resultGc = null;
            RasterInterpolationTPSOperation process = new RasterInterpolationTPSOperation();
            process.getRasterEnvironment().setExtent(boundingBox);

            if (cellSize > 0) {
                double origCellSize = process.getRasterEnvironment().getCellSize();
                process.getRasterEnvironment().setCellSize(cellSize);
                resultGc = process.execute(inputFeatures, inputField);
                process.getRasterEnvironment().setCellSize(origCellSize);
            } else {
                resultGc = process.execute(inputFeatures, inputField);
            }
            // end process

            monitor.setTask(Text.text("Encoding result"));
            monitor.progress(90.0f);

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(TPSProcessFactory.RESULT.key, resultGc);
            monitor.complete(); // same as 100.0f

            return resultMap;
        } catch (Exception eek) {
            monitor.exceptionOccurred(eek);
            throw new ProcessException(eek);
        } finally {
            monitor.dispose();
            started = false;
        }
    }

}
