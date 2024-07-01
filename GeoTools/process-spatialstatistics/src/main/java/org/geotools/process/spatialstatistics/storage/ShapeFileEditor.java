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
package org.geotools.process.spatialstatistics.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.data.DataStore;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.dbf.DbaseFileException;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.dbf.DbaseFileWriter;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;

/**
 * ShapeFileEditor
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ShapeFileEditor extends AbstractEditor {
    protected static final Logger LOGGER = Logging.getLogger(ShapeFileEditor.class);

    private Map<Integer, Integer> fieldMaps; // target, source

    private static final String[] SHP_EXTS = new String[] { "shp", "shx", "dbf", "prj", "sbn",
            "sbx", "qix", "fix", "lyr", "cpg", "qpj" };

    private Charset charset = Charset.defaultCharset();

    public ShapeFileEditor() {
    }

    public ShapeFileEditor(Charset charset) {
        this.charset = charset;
    }

    public boolean rename(DataStore dataStore, String layerName, String newName) {
        String folder = getFolder(dataStore);
        if (folder == null) {
            return false;
        }

        final int pos = layerName.lastIndexOf(".");
        if (pos > 0) {
            layerName = layerName.substring(0, pos);
        }

        for (String ext : SHP_EXTS) {
            File source = new File(folder, layerName + "." + ext);
            File dest = new File(folder, newName + "." + ext);
            if (source.exists() && !dest.exists()) {
                source.renameTo(dest);
            }
        }

        return true;
    }

    public boolean remove(DataStore dataStore, String layerName) {
        String folder = getFolder(dataStore);
        if (folder == null) {
            return false;
        }

        final int pos = layerName.lastIndexOf(".");
        if (pos > 0) {
            layerName = layerName.substring(0, pos);
        }

        for (String ext : SHP_EXTS) {
            File file = new File(folder, layerName + "." + ext);
            if (file.exists()) {
                file.delete();
            }
        }

        return true;
    }

    public boolean addField(DataStore dataStore, String layerName, String fieldName,
            Class<?> binding, int length) throws DbaseFileException, IOException {
        SimpleFeatureType schema = dataStore.getSchema(layerName);
        fieldName = FeatureTypes.validateProperty(schema, fieldName);
        if (schema.indexOf(fieldName) != -1) {
            return true;
        }

        String folder = getFolder(dataStore);
        if (folder == null) {
            return false;
        }

        // check file extension
        final int pos = layerName.lastIndexOf(".");
        if (pos > 0) {
            layerName = layerName.substring(0, pos);
        }

        // layername --> layername_20130218_130982_23
        File source = new File(folder, layerName + ".dbf");
        File target = new File(folder, getUniqueName(layerName) + ".dbf");

        if (source.renameTo(target)) {
            if (addField(target, source, fieldName, binding, length)) {
                target.delete();
                return true;
            } else {
                source.delete();
                target.renameTo(source);
            }
        }

        return false;
    }

    public boolean addField(File dbfSource, File dbfDest, String fieldName, Class<?> binding,
            int length) throws IOException, DbaseFileException {
        DbaseFileReader reader = getReader(dbfSource);
        DbaseFileHeader writerHeader = addDbaseHeader(reader.getHeader(), fieldName, binding,
                length);

        return transferAttributes(reader, dbfDest, writerHeader);
    }

    public boolean removeField(DataStore dataStore, String layerName, String[] fields)
            throws Exception {
        String folder = getFolder(dataStore);
        if (folder == null) {
            return false;
        }

        // check file extension
        final int pos = layerName.lastIndexOf(".");
        if (pos > 0) {
            layerName = layerName.substring(0, pos);
        }

        // layername --> layername_20130218_130982_23
        File source = new File(folder, layerName + ".dbf");
        File target = new File(folder, getUniqueName(layerName) + ".dbf");

        if (source.renameTo(target)) {
            if (removeField(target, source, fields)) {
                target.delete();
                return true;
            }
        } else {
            source.delete();
            target.renameTo(source);
        }

        return false;
    }

    public boolean removeField(File dbfSource, File dbfDest, String[] fields) throws IOException,
            DbaseFileException {
        DbaseFileReader reader = getReader(dbfSource);
        DbaseFileHeader writerHeader = modifyDbaseHeader(reader.getHeader(), fields);

        return transferAttributes(reader, dbfDest, writerHeader);
    }

    private String getUniqueName(String prefix) {
        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMdd_hhmmss_S");
        return prefix + "_" + dataFormat.format(Calendar.getInstance().getTime());
    }

    private String getFolder(DataStore dataStore) {
        String folder = null;
        final URI uri = dataStore.getInfo().getSource();

        try {
            if (dataStore instanceof DirectoryDataStore) {
                folder = URLs.urlToFile(uri.toURL()).getPath();
            } else if (dataStore instanceof ShapefileDataStore) {
                folder = URLs.urlToFile(uri.toURL()).getParent();
            }
        } catch (MalformedURLException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return folder;
    }

    private DbaseFileHeader addDbaseHeader(DbaseFileHeader readerHeader, String fieldName,
            Class<?> binding, int length) throws DbaseFileException {
        int fieldCount = 0;
        DbaseFileHeader writerHeader = new DbaseFileHeader();
        fieldMaps = new TreeMap<Integer, Integer>();

        // 1. clone
        for (int index = 0; index < readerHeader.getNumFields(); index++) {
            String colName = readerHeader.getFieldName(index);
            char type = readerHeader.getFieldType(index);
            int len = readerHeader.getFieldLength(index);
            int dec = readerHeader.getFieldDecimalCount(index);
            writerHeader.addColumn(colName, type, len, dec);

            // target, source
            fieldMaps.put(Integer.valueOf(fieldCount++), Integer.valueOf(index));
        }

        // 2. add field
        if (length == org.geotools.feature.FeatureTypes.ANY_LENGTH) {
            length = 255;
        }

        if ((binding == Integer.class) || (binding == Short.class) || (binding == Byte.class)) {
            writerHeader.addColumn(fieldName, 'N', Math.min(length, 9), 0);
        } else if (binding == Long.class) {
            writerHeader.addColumn(fieldName, 'N', Math.min(length, 19), 0);
        } else if (binding == BigInteger.class) {
            writerHeader.addColumn(fieldName, 'N', Math.min(length, 33), 0);
        } else if (Number.class.isAssignableFrom(binding)) {
            int l = Math.min(length, 33);
            int d = Math.max(l - 2, 0);
            writerHeader.addColumn(fieldName, 'N', l, d);
        } else if (java.util.Date.class.isAssignableFrom(binding)
                && Boolean.getBoolean("org.geotools.shapefile.datetime")) {
            writerHeader.addColumn(fieldName, '@', length, 0);
        } else if (java.util.Date.class.isAssignableFrom(binding)
                || Calendar.class.isAssignableFrom(binding)) {
            writerHeader.addColumn(fieldName, 'D', length, 0);
        } else if (binding == Boolean.class) {
            writerHeader.addColumn(fieldName, 'L', 1, 0);
        } else if (CharSequence.class.isAssignableFrom(binding) || binding == java.util.UUID.class) {
            writerHeader.addColumn(fieldName, 'C', Math.min(254, length), 0);
        } else {
            LOGGER.warning("Unable to write : " + binding.getName());
            return writerHeader;
        }

        // target, source
        fieldMaps.put(Integer.valueOf(fieldCount++), Integer.valueOf(-1));
        return writerHeader;
    }

    @SuppressWarnings("resource")
    private boolean transferAttributes(DbaseFileReader reader, File dest,
            DbaseFileHeader writerHeader) throws IOException {
        FileChannel writerChannel = new FileOutputStream(dest).getChannel();
        DbaseFileWriter writer = new DbaseFileWriter(writerHeader, writerChannel, charset);
        int inNumRecords = 0;

        try {
            Object[] transferCache = new Object[fieldMaps.size()];
            while (reader.hasNext()) {
                DbaseFileReader.Row row = reader.readRow();
                for (Entry<Integer, Integer> entrySet : fieldMaps.entrySet()) {
                    if (entrySet.getValue() == -1) {
                        transferCache[entrySet.getKey()] = null;
                    } else {
                        transferCache[entrySet.getKey()] = row.read(entrySet.getValue());
                    }
                }
                writer.write(transferCache);
                inNumRecords++;
            }
        } finally {
            reader.close();
            writerHeader.setNumRecords(inNumRecords);
            writerChannel.position(0);
            writerHeader.writeHeader(writerChannel);
            writer.close();
        }

        return true;
    }

    private DbaseFileHeader modifyDbaseHeader(DbaseFileHeader readerHeader, String[] dropFields)
            throws IOException, DbaseFileException {
        List<String> list = Arrays.asList(dropFields);

        int fieldCount = 0;
        DbaseFileHeader writerHeader = new DbaseFileHeader();
        fieldMaps = new TreeMap<Integer, Integer>();
        for (int index = 0; index < readerHeader.getNumFields(); index++) {
            String colName = readerHeader.getFieldName(index);
            if (list.indexOf(colName) == -1) {
                char type = readerHeader.getFieldType(index);
                int len = readerHeader.getFieldLength(index);
                int dec = readerHeader.getFieldDecimalCount(index);
                writerHeader.addColumn(colName, type, len, dec);

                // target, source
                fieldMaps.put(Integer.valueOf(fieldCount++), Integer.valueOf(index));
            }
        }

        return writerHeader;
    }

    @SuppressWarnings("resource")
    private DbaseFileReader getReader(File source) throws IOException {
        FileChannel readChannel = new FileInputStream(source).getChannel();
        return new DbaseFileReader(readChannel, false, charset);
    }
}
