/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.operation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

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

    public String getError() {
        return errorBuffer.toString();
    }

    private SimpleFeatureType buildSchema(List<TextColumn> columns, CoordinateReferenceSystem crs)
            throws Exception {
        // check x, y column
        TextColumn xColumn = null, yColomn = null;
        for (TextColumn col : columns) {
            if (col.isX()) {
                xColumn = col;
            } else if (col.isY()) {
                yColomn = col;
            }
        }

        if (xColumn == null || yColomn == null) {
            throw new Exception("X or Y Column does not exist!");
        }

        // build schema
        SimpleFeatureType schema = null;
        schema = FeatureTypes.getDefaultType(getOutputTypeName(), Point.class, crs);
        for (TextColumn col : columns) {
            if (col.getBinding().isAssignableFrom(String.class)) {
                schema = FeatureTypes.add(schema, col.getName(), col.getBinding(), col.getLength());
            } else {
                schema = FeatureTypes.add(schema, col.getName(), col.getBinding());
            }
        }

        return schema;
    }

    public SimpleFeatureSource execute(File textFile, Charset charset, String splitter,
            boolean headerFirst, List<TextColumn> columns, CoordinateReferenceSystem crs)
            throws Exception {

        SimpleFeatureType schema = buildSchema(columns, crs);

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
                    continue;
                }

                String[] values = line.split(splitter);
                int splitSize = values.length;

                SimpleFeature newFeature = featureWriter.buildFeature(null);
                Double x = null;
                Double y = null;
                for (TextColumn col : columns) {
                    Object value = null;
                    if (splitSize > col.getColumnIndex()) {
                        value = TextColumn.removeDoubleQuote(values[col.getColumnIndex()]);
                        value = Converters.convert(value, col.getBinding());
                        newFeature.setAttribute(col.getName(), value);

                        if (value != null && col.isX()) {
                            x = (Double) value;
                        } else if (value != null && col.isY()) {
                            y = (Double) value;
                        }
                    } else {
                        newFeature.setAttribute(col.getName(), value);
                    }
                }

                if (x != null && y != null) {
                    newFeature.setDefaultGeometry(gf.createPoint(new Coordinate(x, y)));
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
            IOUtils.closeQuietly(reader);
            featureWriter.close();
        }

        return featureWriter.getFeatureSource();
    }
}
