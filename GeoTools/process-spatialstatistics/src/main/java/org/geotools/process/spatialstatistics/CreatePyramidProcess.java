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

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.MultiWindRoseOperation;
import org.geotools.process.spatialstatistics.storage.CreatePyramidOperation;
import org.geotools.text.Text;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Creates a wind roses map from features.
 * 
 * @author jyajya, MangoSystem
 * 
 * @source $URL$
 */
public class CreatePyramidProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(CreatePyramidProcess.class);

    private boolean started = false;

    public CreatePyramidProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {

        CreatePyramidOperation process = new CreatePyramidOperation();
        process.execute(((File) input.get(CreatePyramidProcessFactory.inputFile.key)).toURI(),
                ((File) input.get(CreatePyramidProcessFactory.outputFile.key)).toURI(),
                (Integer) input.get(CreatePyramidProcessFactory.level.key));

        return null;
    }

}
