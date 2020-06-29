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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Geometry;

/**
 * MultiWindRoseMapProcessFactory
 * 
 * @author jyajya, MangoSystem
 * 
 * @source $URL$
 */
public class CreatePyramidProcessFactory extends SpatialStatisticsProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(CreatePyramidProcessFactory.class);

    private static final String PROCESS_NAME = "CreatePyramid";

    // VA_Fishnet(extent BoundingBoxData, columns Integer, rows Integer, width Double, height
    // Double) : GML
    // VA_Fishnet(boundary GML, boundaryInside Boolean, columns Integer, rows Integer, width Double,
    // height Double) : GML

    public CreatePyramidProcessFactory() {
        super(new NameImpl(NAMESPACE, PROCESS_NAME));
    }
    // SimpleFeatureCollection inputFeatures, String weightField,
    // SimpleFeatureCollection centerFeatures, double searchRadius, String valueField, int roseCnt

    /** inputFeatures */
    public static final Parameter<File> inputFile = new Parameter<File>(
            "inputTiff", File.class,
            getResource("CreatePyramid.inputTiff.title"),
            getResource("CreatePyramid.inputTiff.description"), true, 1, 1,
            null, null);

    /** weightField */
    public static final Parameter<File> outputFile = new Parameter<File>(
            "outputTiff", File.class,
            getResource("CreatePyramid.outputTiff.title"),
            getResource("CreatePyramid.outputTiff.description"), true, 1, 1,
            null, null);

    // /** inputFeatures */
    // public static final Parameter<SimpleFeatureCollection> inputFeatures2 = new Parameter<SimpleFeatureCollection>(
    // "inputFeatures2", SimpleFeatureCollection.class, Text.text("Input Point Layer 2"),
    // Text.text("Input Vector Layer"), false, 1, 1, null, null);
    //
    // /** weightField */
    // public static final Parameter<String> weightField2 = new Parameter<String>(
    // "weightField2", String.class, Text.text("Field to apply the weight(Point Layer 2)"),
    // Text.text("Field to apply the weight"), false, 0, 1, null, null);
    //
    // /** inputFeatures */
    // public static final Parameter<SimpleFeatureCollection> inputFeatures3 = new Parameter<SimpleFeatureCollection>(
    // "inputFeatures3", SimpleFeatureCollection.class, Text.text("Input Point Layer 3"),
    // Text.text("Input Vector Layer"), false, 1, 1, null, null);
    //
    // /** weightField */
    // public static final Parameter<String> weightField3 = new Parameter<String>(
    // "weightField3", String.class, Text.text("Field to apply the weight(Point Layer 3)"),
    // Text.text("Field to apply the weight"), false, 0, 1, null, null);

    /** searchRadius */
    public static final Parameter<Integer> level = new Parameter<Integer>("level",
            Integer.class, getResource("CreatePyramid.level.title"),
            getResource("CreatePyramid.level.description"), false, 0, 1, null, null);

    @Override
    protected Map<String, Parameter<?>> getParameterInfo() {
        HashMap<String, Parameter<?>> parameterInfo = new LinkedHashMap<String, Parameter<?>>();
        parameterInfo.put(inputFile.key, inputFile);
        parameterInfo.put(outputFile.key, outputFile);
        parameterInfo.put(level.key, level);
        return parameterInfo;
    }

    public static final Parameter<URI> result = new Parameter<URI>(
            "result", URI.class,
            getResource("CreatePyramid.result.title"),
            getResource("CreatePyramid.result.description"));

    static final Map<String, Parameter<?>> resultInfo = new TreeMap<String, Parameter<?>>();
    static {
        resultInfo.put(result.key, result);
    }

    @Override
    protected Map<String, Parameter<?>> getResultInfo(Map<String, Object> parameters)
            throws IllegalArgumentException {
        return Collections.unmodifiableMap(resultInfo);
    }

    @Override
    public Process create() {
        return new MultiWindRoseMapProcess(this);
    }

    @Override
    public InternationalString getTitle() {
        return getResource("CreatePyramid.title");
    }

    @Override
    protected InternationalString getDescription() {
        return getResource("CreatePyramid.description");
    }

}
