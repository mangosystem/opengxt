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

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.EnvelopeToPolygonFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates a polygon features, each of which represents the envelope of an input feature.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FeatureEnvelopeToPolygonProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(FeatureEnvelopeToPolygonProcess.class);

    public FeatureEnvelopeToPolygonProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            Boolean singleEnvelope, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(FeatureEnvelopeToPolygonProcessFactory.inputFeatures.key, inputFeatures);
        map.put(FeatureEnvelopeToPolygonProcessFactory.singleEnvelope.key, singleEnvelope);

        Process process = new FeatureEnvelopeToPolygonProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);

            return (SimpleFeatureCollection) resultMap
                    .get(FeatureEnvelopeToPolygonProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        SimpleFeatureCollection inputFeatures = (SimpleFeatureCollection) Params.getValue(input,
                FeatureEnvelopeToPolygonProcessFactory.inputFeatures, null);
        if (inputFeatures == null) {
            throw new NullPointerException("inputFeatures parameter required");
        }

        Boolean singleEnvelope = (Boolean) Params.getValue(input,
                FeatureEnvelopeToPolygonProcessFactory.singleEnvelope,
                FeatureEnvelopeToPolygonProcessFactory.singleEnvelope.sample);

        // start process
        SimpleFeatureCollection resultFc = DataUtilities
                .simple(new EnvelopeToPolygonFeatureCollection(inputFeatures, singleEnvelope));
        // end process

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(FeatureEnvelopeToPolygonProcessFactory.RESULT.key, resultFc);
        return resultMap;
    }
}
