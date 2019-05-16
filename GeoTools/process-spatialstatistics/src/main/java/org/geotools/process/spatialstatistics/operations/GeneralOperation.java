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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.process.spatialstatistics.storage.FeatureInserter;
import org.geotools.process.spatialstatistics.storage.IFeatureInserter;
import org.geotools.process.spatialstatistics.storage.MemoryFeatureInserter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * Abstract General Operation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class GeneralOperation {
    protected static final Logger LOGGER = Logging.getLogger(GeneralOperation.class);

    protected final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);

    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    private DataStore outputDataStore = null;

    public void setOutputDataStore(DataStore outputDataStore) {
        this.outputDataStore = outputDataStore;
    }

    public DataStore getOutputDataStore() {
        return outputDataStore;
    }

    protected boolean isShapefileDataStore(DataStore dataStore) {
        if (dataStore instanceof DirectoryDataStore) {
            return true;
        } else if (dataStore instanceof ShapefileDataStore) {
            return true;
        }
        return false;
    }

    protected IFeatureInserter getFeatureWriter(SimpleFeatureType schema) throws IOException {
        if (getOutputDataStore() == null) {
            return new MemoryFeatureInserter(schema);
        } else {
            // create schema
            SimpleFeatureStore featureStore = null;
            getOutputDataStore().createSchema(schema);

            final String typeName = schema.getTypeName();
            SimpleFeatureSource featureSource = getOutputDataStore().getFeatureSource(typeName);
            if (featureSource instanceof SimpleFeatureStore) {
                featureStore = (SimpleFeatureStore) featureSource;
                featureStore.setTransaction(new DefaultTransaction(typeName));
            } else {
                LOGGER.log(Level.WARNING, typeName
                        + " does not support SimpleFeatureStore interface!");
                featureStore = (SimpleFeatureStore) featureSource;
            }

            return new FeatureInserter(featureStore);
        }
    }

    protected Filter getIntersectsFilter(String geomField, Geometry searchGeometry) {
        return ff.and(ff.bbox(ff.property(geomField), JTS.toEnvelope(searchGeometry)),
                ff.intersects(ff.property(geomField), ff.literal(searchGeometry)));
    }
}
