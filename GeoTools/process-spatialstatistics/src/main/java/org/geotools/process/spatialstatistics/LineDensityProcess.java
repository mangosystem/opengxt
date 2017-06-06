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
import org.geotools.process.spatialstatistics.gridcoverage.RasterLineDensityOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Calculates the density of linear features in the neighborhood of each output raster cell. <br>
 * Density is calculated in units of length per unit of area.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class LineDensityProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(LineDensityProcess.class);

    public LineDensityProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static GridCoverage2D process(SimpleFeatureCollection inputFeatures,
            KernelType kernelType, String populationField, Double searchRadius, Double cellSize,
            ReferencedEnvelope extent, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(LineDensityProcessFactory.inputFeatures.key, inputFeatures);
        map.put(LineDensityProcessFactory.populationField.key, populationField);
        map.put(LineDensityProcessFactory.searchRadius.key, searchRadius);
        map.put(LineDensityProcessFactory.cellSize.key, cellSize);
        map.put(LineDensityProcessFactory.extent.key, extent);

        Process process = new LineDensityProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (GridCoverage2D) resultMap.get(LineDensityProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                LineDensityProcessFactory.inputFeatures, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        String populationField = (String) Params.getValue(input,
                LineDensityProcessFactory.populationField, null);
        Double searchRadius = (Double) Params.getValue(input,
                LineDensityProcessFactory.searchRadius,
                LineDensityProcessFactory.searchRadius.sample);
        Double cellSize = (Double) Params.getValue(input, LineDensityProcessFactory.cellSize,
                LineDensityProcessFactory.cellSize.sample);
        ReferencedEnvelope extent = (ReferencedEnvelope) Params.getValue(input,
                LineDensityProcessFactory.extent, null);

        // start process
        ReferencedEnvelope boundingBox = extent == null ? inputFeatures.getBounds() : extent;

        // get default cell size from extent
        if (cellSize == null || Double.isNaN(cellSize) || cellSize <= 0.0) {
            cellSize = Math.min(boundingBox.getWidth(), boundingBox.getHeight()) / 250.0;
            LOGGER.warning("default cell size = " + cellSize);
        }

        // Neighborhood: Circle + Radius, Rectangle + width + height
        if (Double.isNaN(searchRadius) || searchRadius <= 0.0) {
            searchRadius = Math.min(boundingBox.getWidth(), boundingBox.getHeight()) / 30.0;
            LOGGER.warning("default Radius = " + searchRadius);
        }

        GridCoverage2D resultGc = null;
        RasterLineDensityOperation process = new RasterLineDensityOperation();
        process.getRasterEnvironment().setExtent(boundingBox);
        process.getRasterEnvironment().setCellSize(cellSize);

        resultGc = process.execute(inputFeatures, populationField, searchRadius);
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(LineDensityProcessFactory.RESULT.key, resultGc);
        return resultMap;
    }
}
