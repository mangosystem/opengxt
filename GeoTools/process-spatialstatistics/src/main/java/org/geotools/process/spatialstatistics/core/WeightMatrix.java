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
package org.geotools.process.spatialstatistics.core;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.process.spatialstatistics.core.AbstractWeightMatrix.SpatialWeightMatrixType;
import org.geotools.util.logging.Logging;

/**
 * SpatialWeightMatrixResult
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class WeightMatrix {
    protected static final Logger LOGGER = Logging.getLogger(WeightMatrix.class);

    private final String newLine = System.getProperty("line.separator");

    private final String space = " ";

    private String uniqueField;

    private String typeName;

    private SpatialWeightMatrixType spatialWeightMatrixType = SpatialWeightMatrixType.Distance;

    // primaryID, <secondaryID, distance>
    private LinkedHashMap<Object, Hashtable<Object, Double>> items;

    public int getFeatureCount() {
        return this.items.size();
    }

    public String getUniqueField() {
        return uniqueField;
    }

    public void setUniqueField(String uniqueField) {
        if (uniqueField == null || uniqueField.isEmpty()) {
            this.uniqueField = "FeatureID";
        } else {
            this.uniqueField = uniqueField;
        }
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public SpatialWeightMatrixType getSpatialWeightMatrixType() {
        return spatialWeightMatrixType;
    }

    public void setSpatialWeightMatrixType(SpatialWeightMatrixType spatialWeightMatrixType) {
        this.spatialWeightMatrixType = spatialWeightMatrixType;
    }

    public LinkedHashMap<Object, Hashtable<Object, Double>> getItems() {
        return items;
    }

    public void setItems(LinkedHashMap<Object, Hashtable<Object, Double>> items) {
        this.items = items;
    }

    public WeightMatrix(SpatialWeightMatrixType spatialWeightMatrixType) {
        this.setSpatialWeightMatrixType(spatialWeightMatrixType);
        this.setItems(new LinkedHashMap<Object, Hashtable<Object, Double>>());
    }

    public void setupVariables(String typeName, String uniqueField) {
        this.setUniqueField(uniqueField);
        this.setTypeName(typeName);
    }

    public void visit(Object primaryID, Object secondaryID) {
        this.visit(primaryID, secondaryID, Double.valueOf(1.0));
    }

    public void visit(Object primaryID, Object secondaryID, Double distance) {
        if (!items.containsKey(primaryID)) {
            items.put(primaryID, new Hashtable<Object, Double>());
        }
        items.get(primaryID).put(secondaryID, distance);
    }

    public boolean isNeighbor(SpatialEvent source, SpatialEvent target) {
        return isNeighbor(source.id, target.id);
    }

    public boolean isNeighbor(Object primaryID, Object secondaryID) {
        if (items.containsKey(primaryID)) {
            return items.get(primaryID).containsKey(secondaryID);
        }
        return false;
    }

    public double getWeight(SpatialEvent source, SpatialEvent target) {
        return getWeight(source.id, target.id);
    }

    public double getWeight(Object primaryID, Object secondaryID) {
        return isNeighbor(primaryID, secondaryID) ? 1.0 : 0.0;
    }

    public void save(File outputFile, Charset charset) throws IOException {
        if (spatialWeightMatrixType == SpatialWeightMatrixType.Distance) {
            writeDistance(outputFile, charset);
        } else {
            writeContiguity(outputFile, charset);
        }
    }

    public boolean load(File swmFile, Charset charset) {
        // TODO code here

        return false;
    }

    private void writeContiguity(File outputFile, Charset charset) throws IOException {
        BufferedWriter writer = null;
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            writer = new BufferedWriter(new OutputStreamWriter(fos, charset));

            StringBuffer sb = new StringBuffer();

            // header : 0 25 seoul_series sgg_cd
            sb.append("0").append(space).append(this.getFeatureCount()).append(space);
            sb.append(this.getTypeName()).append(space).append(this.getUniqueField());
            writer.write(sb.append(newLine).toString());

            // matrix
            for (Entry<Object, Hashtable<Object, Double>> entry : items.entrySet()) {
                Object primaryID = entry.getKey();
                int count = entry.getValue().size();

                // 11170 7
                sb.setLength(0);
                sb.append(primaryID.toString()).append(space).append(count);
                writer.write(sb.append(newLine).toString());

                // 11440 11590 11140 11200 11650 11560 11680
                sb.setLength(0);
                for (Entry<Object, Double> second : entry.getValue().entrySet()) {
                    if (sb.length() > 0) {
                        sb.append(space);
                    }
                    sb.append(second.getKey().toString());
                }
                writer.write(sb.append(newLine).toString());
            }
            writer.flush();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
        } finally {
            closeQuietly(writer);
        }
    }

    private void writeDistance(File outputFile, Charset charset) throws IOException {
        BufferedWriter writer = null;
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            writer = new BufferedWriter(new OutputStreamWriter(fos, charset));

            StringBuffer sb = new StringBuffer();

            // header : 0 25 seoul_series sgg_cd
            sb.append("0").append(space).append(this.getFeatureCount()).append(space);
            sb.append(this.getTypeName()).append(space).append(this.getUniqueField());
            writer.write(sb.append(newLine).toString());

            // matrix
            for (Entry<Object, Hashtable<Object, Double>> entry : items.entrySet()) {
                Object primaryID = entry.getKey();
                for (Entry<Object, Double> second : entry.getValue().entrySet()) {
                    // 11545 11620 4029.25183
                    sb.setLength(0);
                    sb.append(primaryID.toString()).append(space);
                    sb.append(second.getKey().toString()).append("         ");
                    sb.append(second.getValue());
                    writer.write(sb.append(newLine).toString());
                }
            }
            writer.flush();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } finally {
            closeQuietly(writer);
        }
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
