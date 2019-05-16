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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * TextColumn
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class TextColumn {
    protected static final Logger LOGGER = Logging.getLogger(TextColumn.class);

    // Unicode char represented by the UTF-8 byte order mark (EF BB BF)
    private static final String UTF8_BOM = "\uFEFF";

    private static final String prefix = "col_";

    public static final Map<String, String> reservedMap = new HashMap<String, String>();
    static {
        reservedMap.put("^", "\\^");
        reservedMap.put("$", "\\$");
        reservedMap.put("+", "\\+");
        reservedMap.put("*", "\\*");
        reservedMap.put(".", "\\.");
        reservedMap.put("|", "\\|");
    }

    private static final String[] attributesTypes = new String[] { "String", "Short", "Integer",
            "Long", "Float", "Double", "BigDecimal", "Date", "Boolean" };

    private static final String[] spatialTypes = new String[] { "String", "Short", "Integer",
            "Long", "Float", "Double", "BigDecimal", "Date", "Boolean", "X Coordinate",
            "Y Coordinate", "Point", "MultiPoint", "MultiLineString", "MultiPolygon" };

    public static String[] getFieldTypes(boolean isSpatial) {
        if (isSpatial) {
            return spatialTypes;
        } else {
            return attributesTypes;
        }
    }

    private String name;

    private String type;

    private int length = 254;

    private int columnIndex = -1;

    private List<String> sampleValues = new ArrayList<String>();

    public TextColumn() {
    }

    public TextColumn(String name, String type, int length) {
        setName(name);
        setType(type);
        setLength(length);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getType() {
        return type;
    }

    public Class<?> getBinding() {
        if (isX() || isY()) {
            return Double.class;
        } else {
            return findBestBinding(getType());
        }
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isX() {
        return type.equalsIgnoreCase("X Coordinate");
    }

    public boolean isY() {
        return type.equalsIgnoreCase("Y Coordinate");
    }
    
    public void setX() {
        this.setType("X Coordinate");
    }
    
    public void setY() {
        this.setType("Y Coordinate");
    }

    public boolean isGeometry() {
        return Geometry.class.isAssignableFrom(getBinding());
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    public List<String> getSampleValues() {
        return sampleValues;
    }

    public void setSampleValues(List<String> sampleValues) {
        this.sampleValues = sampleValues;
    }

    public String[] getItems() {
        String[] items = new String[3 + sampleValues.size()];
        items[0] = name;
        items[1] = type;
        items[2] = String.valueOf(length);

        for (int index = 0; index < sampleValues.size(); index++) {
            items[3 + index] = sampleValues.get(index);
        }

        return items;
    }

    public Class<?> findBestBinding(String typeName) {
        Class<?> binding = null;

        if (typeName.contains("AUTOINCREMENT") || typeName.contains("SERIAL")
                || typeName.contains("COUNTER")) {
            binding = Integer.class;
            return binding;
        }

        if ("GEOMETRY".equalsIgnoreCase(typeName)) {
            binding = Geometry.class;
        } else if ("MULTIPOLYGON".equalsIgnoreCase(typeName)) {
            binding = MultiPolygon.class;
        } else if ("MULTILINESTRING".equalsIgnoreCase(typeName)) {
            binding = MultiLineString.class;
        } else if ("MULTIPOINT".equalsIgnoreCase(typeName)) {
            binding = MultiPoint.class;
        } else if ("POLYGON".equalsIgnoreCase(typeName)) {
            binding = Polygon.class;
        } else if ("LINESTRING".equalsIgnoreCase(typeName)) {
            binding = LineString.class;
        } else if ("POINT".equalsIgnoreCase(typeName)) {
            binding = Point.class;
        } else if ("GEOMETRYCOLLECTION".equalsIgnoreCase(typeName)) {
            binding = GeometryCollection.class;
        } else if ("SMALLINT".equalsIgnoreCase(typeName)) {
            binding = Short.class;
        } else if ("SHORT".equalsIgnoreCase(typeName)) {
            binding = Short.class;
        } else if ("INTEGER".equalsIgnoreCase(typeName)) {
            binding = Integer.class;
        } else if ("INT4".equalsIgnoreCase(typeName)) {
            binding = Integer.class;
        } else if ("LONG".equalsIgnoreCase(typeName)) {
            binding = Long.class;
        } else if ("BIGINT".equalsIgnoreCase(typeName)) {
            binding = Long.class;
        } else if ("REAL".equalsIgnoreCase(typeName)) {
            binding = Float.class;
        } else if ("FLOAT".equalsIgnoreCase(typeName)) {
            binding = Double.class;
        } else if ("FLOAT8".equalsIgnoreCase(typeName)) {
            binding = Double.class;
        } else if ("INT8".equalsIgnoreCase(typeName)) {
            binding = Long.class;
        } else if ("DOUBLE".equalsIgnoreCase(typeName)) {
            binding = Double.class;
        } else if ("DECIMAL".equalsIgnoreCase(typeName)) {
            binding = Double.class;
        } else if ("BigDecimal".equalsIgnoreCase(typeName)) {
            binding = Double.class;
        } else if ("NUMERIC".equalsIgnoreCase(typeName)) {
            binding = Double.class;
        } else if (typeName.contains("CHAR")) {
            binding = String.class;
        } else if ("CLOB".equalsIgnoreCase(typeName)) {
            binding = String.class;
        } else if ("TEXT".equalsIgnoreCase(typeName)) {
            binding = String.class;
        } else if ("STRING".equalsIgnoreCase(typeName)) {
            binding = String.class;
        } else if ("DATE".equalsIgnoreCase(typeName)) {
            binding = Date.class;
        } else if ("DATETIME".equalsIgnoreCase(typeName)) {
            binding = Date.class;
        } else if ("TIMESTAMP".equalsIgnoreCase(typeName)) {
            binding = Date.class;
        } else if ("BLOB".equalsIgnoreCase(typeName)) {
            binding = byte[].class;
        } else if ("BINARY".equalsIgnoreCase(typeName)) {
            binding = byte[].class;
        } else if ("LONGBINARY".equalsIgnoreCase(typeName)) {
            binding = byte[].class;
        } else if ("LONGVARBINARY".equalsIgnoreCase(typeName)) {
            binding = byte[].class;
        } else if ("VARBINARY".equalsIgnoreCase(typeName)) {
            binding = byte[].class;
        }

        return binding;
    }

    public static TextColumn[] getColumns(File textFile, Charset charset, String splitter,
            boolean headerFirst) {
        if (reservedMap.containsKey(splitter)) {
            splitter = reservedMap.get(splitter);
        }

        TextColumn[] columns = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(textFile),
                    charset));

            int sampleSize = 1;
            String line = reader.readLine();
            if (line == null || line.isEmpty()) {
                return columns;
            }
            
            String[] values = line.split(splitter);

            // build header
            boolean isUTF8 = charset.equals(Charset.forName("UTF-8"));
            columns = new TextColumn[values.length];
            for (int index = 0; index < values.length; index++) {
                columns[index] = new TextColumn();
                columns[index].setType("String");
                columns[index].setColumnIndex(index);

                String value = removeDoubleQuote(values[index]).trim();
                if (index == 0 && isUTF8 && value.startsWith(UTF8_BOM)) {
                    value = value.substring(1);
                }

                if (headerFirst) {
                    columns[index].setName(value);
                } else {
                    columns[index].setName(prefix + index);
                    columns[index].getSampleValues().add(value);
                }
            }

            // build sample value
            int limitLength = columns.length - 1;
            while (line != null) {
                line = reader.readLine();
                if (line == null || line.isEmpty()) {
                    continue;
                }
                
                sampleSize++;
                values = line.split(splitter);

                for (int index = 0; index < values.length; index++) {
                    if (index > limitLength) {
                        break;
                    }
                    columns[index].getSampleValues().add(removeDoubleQuote(values[index]));
                }

                if (sampleSize >= 5) {
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } finally {
            closeQuietly(reader);
        }

        return columns;
    }

    private static final Pattern pattern = Pattern.compile("^\"|\"$");

    public static String removeDoubleQuote(String value) {
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            return matcher.replaceAll("").trim();
        } else {
            return value.trim();
        }
    }

    private static void closeQuietly(Closeable io) {
        try {
            if (io != null) {
                io.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }
}