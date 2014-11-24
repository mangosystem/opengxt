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

    public abstract int getFlushInterval();

    public abstract void setFlushInterval(int flushInterval);

    public abstract int getFeatureCount();

    public abstract SimpleFeature buildFeature(String id) throws IOException;

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