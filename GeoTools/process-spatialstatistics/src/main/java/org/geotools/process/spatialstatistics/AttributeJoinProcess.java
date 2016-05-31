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
import org.geotools.data.Join;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.transformation.JoinAttributeFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Joins features to another features or attribute table based on a common field.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class AttributeJoinProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(AttributeJoinProcess.class);

    private boolean started = false;

    public AttributeJoinProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(SimpleFeatureCollection inputFeatures,
            String primaryKey, SimpleFeatureCollection joinFeatures, String foreignKey,
            Join.Type joinType, ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(AttributeJoinProcessFactory.inputFeatures.key, inputFeatures);
        map.put(AttributeJoinProcessFactory.primaryKey.key, primaryKey);
        map.put(AttributeJoinProcessFactory.joinFeatures.key, joinFeatures);
        map.put(AttributeJoinProcessFactory.foreignKey.key, foreignKey);
        map.put(AttributeJoinProcessFactory.joinType.key, joinType);

        Process process = new AttributeJoinProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(AttributeJoinProcessFactory.RESULT.key);
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
                    input, AttributeJoinProcessFactory.inputFeatures, null);
            String primaryKey = (String) Params.getValue(input,
                    AttributeJoinProcessFactory.primaryKey, null);
            SimpleFeatureCollection joinFeatures = (SimpleFeatureCollection) Params.getValue(input,
                    AttributeJoinProcessFactory.joinFeatures, null);
            String foreignKey = (String) Params.getValue(input,
                    AttributeJoinProcessFactory.foreignKey, null);
            if (inputFeatures == null || primaryKey == null || joinFeatures == null
                    || foreignKey == null) {
                throw new NullPointerException(
                        "inputFeatures, primaryKey, joinFeatures, foreignKey parameters required");
            }
            Join.Type joinType = (Join.Type) Params.getValue(input,
                    AttributeJoinProcessFactory.joinType,
                    AttributeJoinProcessFactory.joinType.sample);

            // start process
            SimpleFeatureCollection resultFc = DataUtilities
                    .simple(new JoinAttributeFeatureCollection(inputFeatures, primaryKey,
                            joinFeatures, foreignKey, joinType));
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(AttributeJoinProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
