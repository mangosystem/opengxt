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
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.enumeration.KernelType;
import org.geotools.process.spatialstatistics.gridcoverage.RasterKernelDensityOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculates a magnitude per unit area from point features using a kernel function to fit a smoothly tapered surface to each point.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class KernelDensityProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(KernelDensityProcess.class);

    private boolean started = false;

    public KernelDensityProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(SimpleFeatureCollection inputFeatures,
            KernelType kernelType, String populationField, Double searchRadius, Double cellSize,
            ReferencedEnvelope extent, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(KernelDensityProcessFactory.inputFeatures.key, inputFeatures);
        map.put(KernelDensityProcessFactory.kernelType.key, kernelType);
        map.put(KernelDensityProcessFactory.populationField.key, populationField);
        map.put(KernelDensityProcessFactory.searchRadius.key, searchRadius);
        map.put(KernelDensityProcessFactory.cellSize.key, cellSize);
        map.put(KernelDensityProcessFactory.extent.key, extent);

        Process process = new KernelDensityProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(KernelDensityProcessFactory.RESULT.key);
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
            SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(
                    input, KernelDensityProcessFactory.inputFeatures, null);
            if (inputFeatures == null) {
                throw new NullPointerException("inputFeatures parameters required");
            }

            KernelType kernelType = (KernelType) Params.getValue(input,
                    KernelDensityProcessFactory.kernelType, KernelType.Quadratic);
            String populationField = (String) Params.getValue(input,
                    KernelDensityProcessFactory.populationField, null);
            Double searchRadius = (Double) Params.getValue(input,
                    KernelDensityProcessFactory.searchRadius, 0.0);
            Double cellSize = (Double) Params.getValue(input, KernelDensityProcessFactory.cellSize,
                    0.0);
            ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                    KernelDensityProcessFactory.extent, null);

            // start process
            ReferencedEnvelope boundingBox = inputFeatures.getBounds();
            if (extent != null) {
                boundingBox = extent;
            }

            // get default cell size from extent
            if (cellSize == null || Double.isNaN(cellSize) || cellSize == 0.0) {
                cellSize = Math.min(boundingBox.getWidth(), boundingBox.getHeight()) / 250.0;
                LOGGER.warning("default cell size = " + cellSize);
            }

            if (searchRadius == null || Double.isNaN(searchRadius) || searchRadius == 0) {
                searchRadius = Math.min(boundingBox.getWidth(), boundingBox.getHeight()) / 30.0;
                LOGGER.warning("default neighborhood = Circle + Radius(" + searchRadius + ")");
            }

            GridCoverage2D resultGc = null;
            RasterKernelDensityOperation process = new RasterKernelDensityOperation();
            process.getRasterEnvironment().setExtent(boundingBox);
            process.getRasterEnvironment().setCellSize(cellSize);
            process.setKernelType(kernelType);
            resultGc = process.execute(inputFeatures, populationField, searchRadius);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(KernelDensityProcessFactory.RESULT.key, resultGc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }

}
