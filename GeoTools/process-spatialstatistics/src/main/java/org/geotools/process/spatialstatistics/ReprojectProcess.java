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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * Reprojects features into a supplied coordinate reference system. Can also force a feature collection to have a given CRS.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ReprojectProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(ReprojectProcess.class);

    public ReprojectProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            CoordinateReferenceSystem forcedCRS, CoordinateReferenceSystem targetCRS,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ReprojectProcessFactory.inputFeatures.key, inputFeatures);
        map.put(ReprojectProcessFactory.forcedCRS.key, forcedCRS);
        map.put(ReprojectProcessFactory.targetCRS.key, targetCRS);

        Process process = new ReprojectProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(ReprojectProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                ReprojectProcessFactory.inputFeatures, null);

        CoordinateReferenceSystem forcedCRS = (CoordinateReferenceSystem) Params.getValue(input,
                ReprojectProcessFactory.forcedCRS, null);

        CoordinateReferenceSystem targetCRS = (CoordinateReferenceSystem) Params.getValue(input,
                ReprojectProcessFactory.targetCRS, null);

        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameters required");
        }

        // start process
        SimpleFeatureCollection resultFc = inputFeatures;

        if (forcedCRS != null) {
            try {
                resultFc = new ForceCoordinateSystemFeatureResults(inputFeatures, forcedCRS, false);
            } catch (IOException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            } catch (SchemaException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }

        if (targetCRS != null) {
            // note, using ReprojectFeatureResults would work. However that would
            // just work by accident... see GEOS-4072
            resultFc = new ReprojectingFeatureCollection(resultFc, targetCRS);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(ReprojectProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
