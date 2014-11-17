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

import org.geotools.data.DataStore;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Abstract General Operation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class GeneralOperation {
    
    protected final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);
    
    protected final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    public GeneralOperation() {
        // TODO Auto-generated constructor stub
    }

    public boolean isShapefileDataStore(DataStore dataStore) {
        if (dataStore instanceof DirectoryDataStore) {
            return true;
        } else if (dataStore instanceof ShapefileDataStore) {
            return true;
        }
        return false;
    }
}
