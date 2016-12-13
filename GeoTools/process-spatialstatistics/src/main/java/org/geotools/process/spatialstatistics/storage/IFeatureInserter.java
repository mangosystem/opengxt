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

import java.io.IOException;

import org.geotools.data.FeatureWriter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * IFeatureInserter interface
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public interface IFeatureInserter {

    public abstract FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter();

    public abstract SimpleFeatureSource getFeatureSource();

    public abstract SimpleFeatureCollection getFeatureCollection() throws IOException;

    public abstract int getFlushInterval();

    public abstract void setFlushInterval(int flushInterval);

    public abstract int getFeatureCount();

    public abstract SimpleFeature buildFeature() throws IOException;

    public abstract void write(SimpleFeatureCollection featureCollection) throws IOException;

    public abstract void write(SimpleFeature newFeature) throws IOException;

    public abstract void rollback() throws IOException;

    public abstract void rollback(Exception e) throws IOException;

    public abstract void close() throws IOException;

    public abstract void close(SimpleFeatureIterator srcIter) throws IOException;

    public abstract SimpleFeature copyAttributes(SimpleFeature source, SimpleFeature target,
            boolean copyGeometry);

    public abstract void clearFieldMaps();

}