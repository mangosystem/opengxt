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

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.collection.TreeSetFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.filter.visitor.ExtractBoundsFilterVisitor;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Utility class for FeatureCollection
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class DataUtils {
    protected static final Logger LOGGER = Logging.getLogger(DataUtils.class);

    public static SimpleFeatureCollection toSpatialIndexFeatureCollection(
            SimpleFeatureCollection features) {
        SimpleFeatureCollection result = features;

        try {
            if (features instanceof DefaultFeatureCollection) {
                result = new SpatialIndexFeatureCollection2(features);
            } else if (features instanceof TreeSetFeatureCollection) {
                result = new SpatialIndexFeatureCollection2(features);
            } else if (features instanceof ListFeatureCollection) {
                result = new SpatialIndexFeatureCollection2(features);
            } else if (features instanceof DecoratingSimpleFeatureCollection) {
                result = new SpatialIndexFeatureCollection2(features);
            } else if (features instanceof ContentFeatureCollection) {
                // ShapefileFeatureStore, JDBCFeatureStore
                result = new SpatialIndexFeatureCollection2(features);
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return result;
    }

    public static class SpatialIndexFeatureCollection2 extends SpatialIndexFeatureCollection {
        static Logger LOGGER = Logging.getLogger(SpatialIndexFeatureCollection2.class);

        public SpatialIndexFeatureCollection2() {
            this.index = new STRtree();
        }

        public SpatialIndexFeatureCollection2(SimpleFeatureType schema) {
            this.index = new STRtree();
            this.schema = schema;
        }

        public SpatialIndexFeatureCollection2(SimpleFeatureCollection copy) throws IOException {
            this(copy.getSchema());

            addAll(copy);
        }

        public SimpleFeatureCollection sort(SortBy order) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        public SimpleFeatureCollection subCollection(Filter filter) {
            // split out the spatial part of the filter
            SpatialIndexFeatureCollection ret = new SpatialIndexFeatureCollection(schema);
            Envelope env = new Envelope();
            env = (Envelope) filter.accept(ExtractBoundsFilterVisitor.BOUNDS_VISITOR, env);
            for (Iterator<SimpleFeature> iter = (Iterator<SimpleFeature>) index.query(env)
                    .iterator(); iter.hasNext();) {
                SimpleFeature sample = iter.next();
                if (filter.evaluate(sample)) {
                    ret.add(sample);
                }
            }

            return ret;
        }
    }
}
