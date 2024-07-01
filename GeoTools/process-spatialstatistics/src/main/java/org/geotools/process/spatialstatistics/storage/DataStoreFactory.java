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
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;

/**
 * DataStore Factory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DataStoreFactory {
    protected static final Logger LOGGER = Logging.getLogger(DataStoreFactory.class);

    // if debug mode, Charset = UTF-8
    public static String DEFAULT_CHARSET = Charset.defaultCharset().name();

    public static DataStoreFactory newInstance() {
        return new DataStoreFactory();
    }

    public static boolean isShapefileDataStore(DataStore dataStore) {
        if (dataStore instanceof DirectoryDataStore) {
            return true;
        } else if (dataStore instanceof ShapefileDataStore) {
            return true;
        }
        return false;
    }

    public static SimpleFeatureSource getShapefile(String folder, String typeName)
            throws IOException {
        File fileName = new File(folder, typeName);
        if (!typeName.toLowerCase().endsWith(".shp")) {
            fileName = new File(folder, typeName + ".shp");
        }

        // support for Linux, windows...
        DataStore dataStore = getShapefileDataStore(fileName.getPath(), Boolean.TRUE);
        return dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
    }

    public static DataStore getShapefileDataStore(String folder) {
        return getShapefileDataStore(folder, Boolean.FALSE);
    }

    public static DataStore getShapefileDataStore(String folder, Boolean createSpatialIndex) {
        Map<String, Object> params = new HashMap<String, Object>();

        final File file = new File(folder);
        params.put(ShapefileDataStoreFactory.URLP.key, URLs.fileToUrl(file));
        params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, createSpatialIndex);
        params.put(ShapefileDataStoreFactory.DBFCHARSET.key, DEFAULT_CHARSET);
        params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, Boolean.TRUE);

        return getDataStore(Collections.unmodifiableMap(params));
    }

    public static DataStore getDXFDataStore(String dxfFile, String epsgCode) {
        Map<String, Object> params = new HashMap<String, Object>();

        final File file = new File(dxfFile);
        params.put("url", URLs.fileToUrl(file));
        params.put("srs", epsgCode);
        params.put("charset", Charset.forName(DEFAULT_CHARSET));

        return getDataStore(Collections.unmodifiableMap(params));
    }

    public static DataStore getDataStore(Map<String, ?> connectionParams) {
        try {
            final DataStore dataStore = DataStoreFinder.getDataStore(connectionParams);
            return dataStore;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    public static DataStore getDataStore(String dbType, String host, int port, String schema,
            String database, String user, String passwd) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(JDBCDataStoreFactory.DBTYPE.key, dbType);

        params.put(JDBCDataStoreFactory.HOST.key, host);
        params.put(JDBCDataStoreFactory.PORT.key, port);

        params.put(JDBCDataStoreFactory.SCHEMA.key, schema);
        params.put(JDBCDataStoreFactory.DATABASE.key, database);

        params.put(JDBCDataStoreFactory.USER.key, user);
        params.put(JDBCDataStoreFactory.PASSWD.key, passwd);

        params.put(JDBCDataStoreFactory.EXPOSE_PK.key, Boolean.TRUE);
        params.put(JDBCDataStoreFactory.VALIDATECONN.key, Boolean.TRUE);
        params.put(JDBCDataStoreFactory.MAXWAIT.key, 120);

        // check connection validation
        params.put(JDBCDataStoreFactory.VALIDATECONN.key, Boolean.TRUE);

        return getDataStore(Collections.unmodifiableMap(params));
    }

}
