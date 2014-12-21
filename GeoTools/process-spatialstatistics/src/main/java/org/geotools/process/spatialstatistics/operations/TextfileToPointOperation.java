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
package org.geotools.process.spatialstatistics.operations;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Textfile to point features operation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class TextfileToPointOperation extends GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(TextfileToPointOperation.class);

    private StringBuffer errorBuffer = new StringBuffer();

    private WKTReader wktReader;

    public String getError() {
        return errorBuffer.toString();
    }

    private SimpleFeatureType buildSchema(List<TextColumn> columns, CoordinateReferenceSystem crs)
            throws Exception {
        // check x, y or geometry column
        TextColumn xColumn = null, yColumn = null, geomColumn = null;
        for (TextColumn col : columns) {
            if (col.isX()) {
                xColumn = col;
            } else if (col.isY()) {
                yColumn = col;
            } else if (col.isGeometry()) {
                geomColumn = col;
                wktReader = new WKTReader();
            }
        }

        // build schema
        SimpleFeatureType schema = null;
        if (geomColumn == null) {
            if (xColumn == null || yColumn == null) {
                throw new Exception("X or Y Column does not exist!");
            } else {
                schema = FeatureTypes.getDefaultType(getOutputTypeName(), Point.class, crs);
            }
        } else {
            schema = FeatureTypes.getDefaultType(getOutputTypeName(), geomColumn.getBinding(), crs);
        }

        for (TextColumn col : columns) {
            if (col.isGeometry()) {
                continue;
            }

            if (col.getBinding().isAssignableFrom(String.class)) {
                schema = FeatureTypes.add(schema, col.getName(), col.getBinding(), col.getLength());
            } else {
                schema = FeatureTypes.add(schema, col.getName(), col.getBinding());
            }
        }

        return schema;
    }

    public SimpleFeatureCollection execute(File textFile, Charset charset, String splitter,
            boolean headerFirst, List<TextColumn> columns, CoordinateReferenceSystem sourceCRS)
            throws Exception {
        return execute(textFile, charset, splitter, headerFirst, columns, sourceCRS, null);
    }

    public SimpleFeatureCollection execute(File textFile, Charset charset, String splitter,
            boolean headerFirst, List<TextColumn> columns, CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS) throws Exception {

        SimpleFeatureType schema = buildSchema(columns, sourceCRS);

        MathTransform transform = null;
        if (sourceCRS != null && targetCRS != null) {
            transform = getMathTransform(sourceCRS, targetCRS);
            if (transform != null) {
                schema = buildSchema(columns, targetCRS);
            }
        }

        // prepare transactional feature store
        IFeatureInserter featureWriter = getFeatureWriter(schema);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(textFile),
                    charset));

            if (TextColumn.reservedMap.containsKey(splitter)) {
                splitter = TextColumn.reservedMap.get(splitter);
            }

            int lineNumber = 0;
            String line = reader.readLine();
            lineNumber++;

            if (headerFirst) {
                line = reader.readLine().trim();
                lineNumber++;
            }

            while (line != null) {
                if (line.isEmpty()) {
                    line = reader.readLine();
                    lineNumber++;
                    continue;
                }

                String[] values = line.split(splitter);
                int splitSize = values.length;

                Double x = null;
                Double y = null;
                Geometry geometry = null;
                SimpleFeature newFeature = featureWriter.buildFeature(null);

                for (TextColumn col : columns) {
                    Object value = null;
                    if (splitSize > col.getColumnIndex()) {
                        value = TextColumn.removeDoubleQuote(values[col.getColumnIndex()]);
                        if (col.isGeometry()) {
                            geometry = wktReader.read(value.toString());
                        } else {
                            value = Converters.convert(value, col.getBinding());
                            newFeature.setAttribute(col.getName(), value);

                            if (value != null && col.isX()) {
                                x = (Double) value;
                            } else if (value != null && col.isY()) {
                                y = (Double) value;
                            }
                        }
                    } else {
                        newFeature.setAttribute(col.getName(), null);
                    }
                }

                if (geometry == null && x != null && y != null) {
                    geometry = gf.createPoint(new Coordinate(x, y));
                    if (transform != null) {
                        try {
                            geometry = JTS.transform(geometry, transform);
                        } catch (MismatchedDimensionException e) {
                            LOGGER.log(Level.FINER, e.getMessage(), e);
                        } catch (TransformException e) {
                            LOGGER.log(Level.FINER, e.getMessage(), e);
                        }
                    }
                }

                if (geometry != null) {
                    newFeature.setDefaultGeometry(geometry);
                    try {
                        featureWriter.write(newFeature);
                    } catch (Exception e) {
                        errorBuffer.append(lineNumber + " : " + line);
                        errorBuffer.append(System.getProperty("line.separator"));
                    }
                }

                line = reader.readLine();
                lineNumber++;
            }
        } catch (Exception e) {
            featureWriter.rollback(e);
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            throw new Exception(e.getMessage());
        } finally {
            closeQuietly(reader);
            featureWriter.close();
        }

        return featureWriter.getFeatureCollection();
    }

    private MathTransform getMathTransform(CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS) {
        if (sourceCRS == null || targetCRS == null) {
            LOGGER.log(Level.WARNING,
                    "Input CoordinateReferenceSystem is Unknown Coordinate System!");
            return null;
        }

        if (CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            LOGGER.log(Level.WARNING, "Input and Output Coordinate Reference Systems are equal!");
            return null;
        }

        MathTransform transform = null;
        try {
            transform = CRS.findMathTransform(sourceCRS, targetCRS, false);
        } catch (FactoryException e1) {
            LOGGER.log(Level.WARNING, e1.getMessage(), 1);
        }

        return transform;
    }

    private void closeQuietly(Closeable io) {
        try {
            if (io != null) {
                io.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }
}
