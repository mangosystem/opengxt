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

import org.geotools.api.util.ProgressListener;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.relationship.OLSOperation;
import org.geotools.process.spatialstatistics.relationship.OLSResult;
import org.geotools.util.logging.Logging;

/**
 * Performs Ordinary Least Squares (OLS) linear regression.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class OLSProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(OLSProcess.class);

    public OLSProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String dependentVariable, String explanatoryVariables, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(OLSProcessFactory.inputFeatures.key, inputFeatures);
        map.put(OLSProcessFactory.dependentVariable.key, dependentVariable);
        map.put(OLSProcessFactory.explanatoryVariables.key, explanatoryVariables);

        Process process = new OLSProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(OLSProcessFactory.olsFeatures.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                OLSProcessFactory.inputFeatures, null);
        String dependentVariable = (String) Params.getValue(input,
                OLSProcessFactory.dependentVariable, OLSProcessFactory.dependentVariable.sample);
        String explanatoryVariables = (String) Params.getValue(input,
                OLSProcessFactory.explanatoryVariables, null);
        if (inputFeatures == null || dependentVariable == null || dependentVariable.isEmpty()
                || explanatoryVariables == null || explanatoryVariables.isEmpty()) {
            throw new NullPointerException(
                    "inputFeatures, dependentVariable, explanatoryVariables parameters required");
        }

        // start process
        OLSResult report = new OLSResult();
        SimpleFeatureCollection olsFeatures = null;
        try {
            OLSOperation operation = new OLSOperation();
            report = operation.execute(inputFeatures, dependentVariable, explanatoryVariables);
            olsFeatures = operation.getResidualFeatures();
        } catch (IOException e) {
            throw new ProcessException(e);
        }
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(OLSProcessFactory.report.key, report);
        resultMap.put(OLSProcessFactory.olsFeatures.key, olsFeatures);
        return resultMap;
    }

}
