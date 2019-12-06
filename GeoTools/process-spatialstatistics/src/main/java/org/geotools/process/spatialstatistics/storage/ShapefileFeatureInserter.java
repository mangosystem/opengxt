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
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.FeatureWriter;
import org.geotools.data.shapefile.dbf.DbaseFileException;
import org.geotools.data.shapefile.dbf.DbaseFileWriter;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.files.StorageFile;
import org.geotools.data.shapefile.shp.JTSUtilities;
import org.geotools.data.shapefile.shp.ShapeHandler;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileWriter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Shapefile Feature Inserter
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ShapefileFeatureInserter implements IFeatureInserter {
    protected static final Logger LOGGER = Logging.getLogger(ShapefileFeatureInserter.class);

    static final int DBF_LIMIT = 10;

    // 1. FeatureInserter
    boolean isSuccess = false;

    String dataStore;

    String outptuName;

    CoordinateReferenceSystem sourceCRS;

    SimpleFeatureBuilder sfBuilder;

    List<FieldMap> fieldMaps = new ArrayList<FieldMap>();

    // 2. Shapefile & dbf
    private Map<ShpFileType, StorageFile> storageFiles;;

    private ShapeType shapeType;

    private int shapefileLength = 100;

    private int numberOfFeatures = 0;

    private ShapeHandler handler;

    private ShapefileWriter shpWriter;

    private DbaseFileWriter dbfWriter;

    private FileChannel dbfChannel;

    private Charset dbfCharset = Charset.forName(DataStoreFactory.DEFAULT_CHARSET);

    private TimeZone dbfTimeZone = TimeZone.getDefault();;

    private DbaseFileHeader2 dbfHeader;

    private byte[] writeFlags;

    private Object[] transferCache;

    private Envelope bounds;

    public ShapefileFeatureInserter(String folder, SimpleFeatureType inputSchema) {
        this.sourceCRS = inputSchema.getCoordinateReferenceSystem();
        this.dataStore = folder;
        this.outptuName = inputSchema.getTypeName();
        this.fieldMaps.clear();
        this.sfBuilder = new SimpleFeatureBuilder(inputSchema);

        try {
            init(inputSchema, dataStore, outptuName);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter() {
        return null;
    }

    @Override
    public SimpleFeatureSource getFeatureSource() {
        try {
            if (isSuccess) {
                return DataStoreFactory.getShapefile(dataStore, outptuName);
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public SimpleFeatureCollection getFeatureCollection() throws IOException {
        SimpleFeatureSource featureSource = getFeatureSource();
        if (featureSource == null) {
            return null;
        } else {
            return featureSource.getFeatures();
        }
    }

    @Override
    public int getFlushInterval() {
        return 0;
    }

    @Override
    public void setFlushInterval(int flushInterval) {

    }

    @Override
    public int getFeatureCount() {
        return numberOfFeatures;
    }

    @Override
    public SimpleFeature buildFeature() throws IOException {
        return sfBuilder.buildFeature(null);
    }

    @Override
    public void write(SimpleFeatureCollection featureCollection) throws IOException {
        SimpleFeatureIterator iter = featureCollection.features();
        try {
            while (iter.hasNext()) {
                final SimpleFeature feature = iter.next();
                SimpleFeature newFeature = this.buildFeature();
                this.copyAttributes(feature, newFeature, true);
                this.write(newFeature);
            }
        } finally {
            iter.close();
        }
    }

    @Override
    public void write(SimpleFeature newFeature) throws IOException {
        Geometry geometry = (Geometry) newFeature.getDefaultGeometry();
        geometry = JTSUtilities.convertToCollection(geometry, shapeType);
        if (geometry == null || geometry.isEmpty()) {
            return;
        }

        shapefileLength += (handler.getLength(geometry) + 8);
        shpWriter.writeGeometry(geometry);
        bounds.expandToInclude(geometry.getEnvelopeInternal());

        // writing attributes
        int pos = 0;
        for (int index = 0; index < writeFlags.length; index++) {
            // skip geometries
            if (writeFlags[index] > 0) {
                switch (dbfHeader.getFieldType(pos)) {
                case 'C':
                case 'c':
                    final Object objValue = newFeature.getAttribute(index);
                    if (objValue == null) {
                        transferCache[pos] = objValue;
                    } else {
                        final int len = dbfHeader.getFieldLength(pos);
                        String strVal = objValue.toString();
                        if (strVal.getBytes().length > len) {
                            // GeoTools DbaseFileWriter는 한글문제를 고려하지 않음
                            transferCache[pos] = substringBytes(strVal, len);
                        } else {
                            transferCache[pos] = objValue;
                        }
                    }
                    break;
                default:
                    transferCache[pos] = newFeature.getAttribute(index);
                    break;
                }
                pos++;
            }
        }
        dbfWriter.write(transferCache);

        numberOfFeatures++;
    }

    @Override
    public void rollback() throws IOException {
        this.close();

        isSuccess = false;

        // delete shapefile
        final ShpFiles shpFiles = getShapeFiles(dataStore, outptuName);
        shpFiles.delete();
    }

    @Override
    public void rollback(Exception e) throws IOException {
        rollback();
        LOGGER.log(Level.WARNING, e.getMessage(), e);
    }

    @Override
    public void close() throws IOException {
        if (numberOfFeatures > 0) {
            // rewrite header
            shpWriter.writeHeaders(bounds, shapeType, numberOfFeatures, shapefileLength);
            dbfHeader.setNumRecords(numberOfFeatures);
            dbfChannel.position(0);
            dbfHeader.writeHeader(dbfChannel);
        }

        writePrj(sourceCRS);

        shpWriter.close();
        dbfWriter.close();

        shpWriter = null;
        dbfWriter = null;

        StorageFile.replaceOriginals(storageFiles.values().toArray(new StorageFile[0]));

        storageFiles.clear();

        isSuccess = true;
    }

    @Override
    public void close(SimpleFeatureIterator featureIter) throws IOException {
        close();

        if (featureIter != null) {
            featureIter.close();
        }
    }

    @Override
    public SimpleFeature copyAttributes(SimpleFeature source, SimpleFeature target,
            boolean copyGeometry) {
        if (this.fieldMaps.size() == 0) {
            fieldMaps = FieldMap.buildMap(source.getFeatureType(), target.getFeatureType());
        }

        for (FieldMap fieldMap : this.fieldMaps) {
            if (fieldMap.isGeometry) {
                if (copyGeometry) {
                    target.setDefaultGeometry(source.getDefaultGeometry());
                }
            } else {
                target.setAttribute(fieldMap.destID, source.getAttribute(fieldMap.soruceID));
            }
        }

        return target;
    }

    @Override
    public void clearFieldMaps() {
        this.fieldMaps.clear();
    }

    private ShpFiles getShapeFiles(String dataStore, String outptuName) throws IOException {
        if (!outptuName.toLowerCase().endsWith(".shp")) {
            outptuName = outptuName + ".shp";
        }

        String shapeFileName = dataStore + outptuName;
        if (!dataStore.endsWith(File.separator)) {
            shapeFileName = dataStore + File.separator + outptuName;
        }

        return new ShpFiles(shapeFileName);
    }

    private void init(SimpleFeatureType featureType, String dataStore, String outptuName)
            throws IOException {
        // initialize variables
        shapefileLength = 100;
        numberOfFeatures = 0;
        bounds = new Envelope();

        final ShpFiles shpFiles = getShapeFiles(dataStore, outptuName);
        shpFiles.delete();

        storageFiles = new HashMap<ShpFileType, StorageFile>();
        storageFiles.put(ShpFileType.SHP, shpFiles.getStorageFile(ShpFileType.SHP));
        storageFiles.put(ShpFileType.SHX, shpFiles.getStorageFile(ShpFileType.SHX));
        storageFiles.put(ShpFileType.DBF, shpFiles.getStorageFile(ShpFileType.DBF));
        storageFiles.put(ShpFileType.PRJ, shpFiles.getStorageFile(ShpFileType.PRJ));

        dbfHeader = createDbaseHeader(featureType);

        // open underlying writers
        FileChannel shpChannel = storageFiles.get(ShpFileType.SHP).getWriteChannel();
        FileChannel shxChannel = storageFiles.get(ShpFileType.SHX).getWriteChannel();
        shpWriter = new ShapefileWriter(shpChannel, shxChannel);

        dbfChannel = storageFiles.get(ShpFileType.DBF).getWriteChannel();
        dbfWriter = new DbaseFileWriter(dbfHeader, dbfChannel, dbfCharset, dbfTimeZone);

        shpWriter.writeHeaders(new Envelope(), shapeType, numberOfFeatures, shapefileLength);
    }

    private void writePrj(CoordinateReferenceSystem crs) throws IOException {
        if (crs == null) {
            LOGGER.warning("PRJ file not generated for null CoordinateReferenceSystem");
            return;
        }

        FileWriter prjWriter = new FileWriter(storageFiles.get(ShpFileType.PRJ).getFile());
        try {
            String wkt = crs.toWKT().replaceAll("\n", "").replaceAll("  ", "");
            prjWriter.write(wkt);
        } finally {
            prjWriter.close();
        }
    }

    private DbaseFileHeader2 createDbaseHeader(SimpleFeatureType featureType) throws IOException,
            DbaseFileException {
        DbaseFileHeader2 header = new DbaseFileHeader2();

        final int attributeCount = featureType.getAttributeCount();
        writeFlags = new byte[attributeCount];

        List<String> uniqueFields = new ArrayList<String>();
        int dbfFieldCount = 0;
        for (int index = 0; index < attributeCount; index++) {
            AttributeDescriptor descriptor = featureType.getDescriptor(index);
            Class<?> binding = descriptor.getType().getBinding();
            String columnName = descriptor.getLocalName();

            if (columnName.length() > DBF_LIMIT) {
                LOGGER.warning(columnName + " has more than 10 characters, and truncated");
                columnName = columnName.substring(0, 10);

                // must be a unique name
                if (uniqueFields.contains(columnName)) {
                    int increment = 0;
                    while (true) {
                        int subLen = increment >= 10 ? 7 : 8;
                        columnName = columnName.substring(0, subLen) + "_" + (++increment);
                        if (!uniqueFields.contains(columnName)) {
                            break;
                        }
                    }
                }
            }

            if (Geometry.class.isAssignableFrom(binding)) {
                shapeType = JTSUtilities.getShapeType(binding);
                handler = shapeType.getShapeHandler(new GeometryFactory());
            } else {
                int fieldLen = FeatureTypes.getFieldLength(descriptor);
                if (fieldLen == FeatureTypes.ANY_LENGTH) {
                    fieldLen = 255;
                }

                if ((binding == Integer.class) || (binding == Short.class)
                        || (binding == Byte.class)) {
                    header.addColumn(columnName, 'N', Math.min(fieldLen, 9), 0);
                } else if (binding == Long.class) {
                    header.addColumn(columnName, 'N', Math.min(fieldLen, 19), 0);
                } else if (binding == BigInteger.class) {
                    header.addColumn(columnName, 'N', Math.min(fieldLen, 33), 0);
                } else if (Number.class.isAssignableFrom(binding)) {
                    int l = Math.min(fieldLen, 33);
                    int d = Math.max(l - 2, 0);
                    header.addColumn(columnName, 'N', l, d);
                } else if (java.util.Date.class.isAssignableFrom(binding)
                        && Boolean.getBoolean("org.geotools.shapefile.datetime")) {
                    header.addColumn(columnName, '@', fieldLen, 0);
                } else if (java.util.Date.class.isAssignableFrom(binding)
                        || Calendar.class.isAssignableFrom(binding)) {
                    header.addColumn(columnName, 'D', fieldLen, 0);
                } else if (binding == Boolean.class) {
                    header.addColumn(columnName, 'L', 1, 0);
                } else if (CharSequence.class.isAssignableFrom(binding)
                        || binding == java.util.UUID.class) {
                    header.addColumn(columnName, 'C', Math.min(254, fieldLen), 0);
                } else {
                    LOGGER.warning("Unable to write : " + binding.getName());
                    continue;
                }

                dbfFieldCount++;
                writeFlags[index] = (byte) 1;
            }

            uniqueFields.add(columnName);
        }

        // dbf transfer buffer
        transferCache = new Object[dbfFieldCount];

        return header;
    }

    private String substringBytes(String value, int byte_len) {
        int retLength = 0;
        int tempSize = 0;
        for (int index = 1; index <= value.length(); index++) {
            int asc = value.charAt(index - 1);
            if (asc > 127) {
                if (byte_len >= tempSize + 2) {
                    tempSize += 2;
                    retLength++;
                } else {
                    return value.substring(0, retLength);
                }
            } else {
                if (byte_len > tempSize) {
                    tempSize++;
                    retLength++;
                }
            }
        }
        return value.substring(0, retLength);
    }
}
