/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;

/**
 * DataStore Factory
 * 
 * @author Minpa Lee, MangoSystem  
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class DataStoreFactory {
    protected static final Logger LOGGER = Logging.getLogger(DataStoreFactory.class);

    public static final String CharacterSet = ToolboxPlugin.defaultCharset();

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
        params.put(ShapefileDataStoreFactory.URLP.key, DataUtilities.fileToURL(file));
        params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, createSpatialIndex);
        params.put(ShapefileDataStoreFactory.DBFCHARSET.key, CharacterSet);
        params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, Boolean.TRUE);

        return getDataStore(Collections.unmodifiableMap(params));
    }

    public static DataStore getDXFDataStore(String dxfFile, String epsgCode) {
        Map<String, Object> params = new HashMap<String, Object>();

        final File file = new File(dxfFile);
        params.put("url", DataUtilities.fileToURL(file));
        params.put("srs", epsgCode);
        params.put("charset", Charset.forName(CharacterSet));

        return getDataStore(Collections.unmodifiableMap(params));
    }

    @SuppressWarnings({ "rawtypes" })
    public static DataStore getDataStore(Map connectionParams) {
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
