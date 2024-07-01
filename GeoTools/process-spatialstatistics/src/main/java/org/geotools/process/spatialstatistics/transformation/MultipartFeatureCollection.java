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
package org.geotools.process.spatialstatistics.transformation;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SubFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

/**
 * Singlepart to Multipart SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MultipartFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(MultipartFeatureCollection.class);

    private boolean dissolve;

    private String caseField;

    private SimpleFeatureType schema;

    private Hashtable<Object, List<SimpleFeature>> map;

    public MultipartFeatureCollection(SimpleFeatureCollection delegate, String caseField,
            boolean dissolve) {
        super(delegate);

        if (caseField == null || caseField.isEmpty()) {
            throw new NullPointerException("caseField is null");
        }

        this.caseField = FeatureTypes.validateProperty(delegate.getSchema(), caseField);
        this.dissolve = dissolve;
        this.schema = FeatureTypes.build(delegate, delegate.getSchema().getTypeName());
        this.init();
    }

    private void init() {
        map = new Hashtable<Object, List<SimpleFeature>>();

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(schema);
        Expression expression = ff.property(caseField);
        SimpleFeatureIterator featureIter = delegate.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();

                // create new feature
                SimpleFeature newFeature = builder.buildFeature(feature.getID());
                transferAttribute(feature, newFeature);

                Object value = expression.evaluate(feature);
                if (!map.containsKey(value)) {
                    map.put(value, new ArrayList<SimpleFeature>());
                }
                map.get(value).add(newFeature);
            }
        } finally {
            featureIter.close();
        }
    }

    @Override
    public SimpleFeatureIterator features() {
        return new MultipartFeatureIterator(map, dissolve);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        return new SubFeatureCollection(this, filter);
    }

    @Override
    public int size() {
        return map.size();
    }

    static class MultipartFeatureIterator implements SimpleFeatureIterator {
        private Iterator<Entry<Object, List<SimpleFeature>>> featureIter;

        private boolean dissolve = false;

        public MultipartFeatureIterator(Hashtable<Object, List<SimpleFeature>> map, boolean dissolve) {
            this.featureIter = map.entrySet().iterator();
            this.dissolve = dissolve;
        }

        public void close() {
            // nothing to do
        }

        public boolean hasNext() {
            return featureIter.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            Entry<Object, List<SimpleFeature>> entry = featureIter.next();
            List<SimpleFeature> featureList = entry.getValue();

            SimpleFeature nextFeature = featureList.get(0);

            List<Geometry> geometries = new ArrayList<Geometry>();
            for (SimpleFeature feature : featureList) {
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                geometries.add(geometry);
            }

            if (dissolve) {
                CascadedPolygonUnion unionOp = new CascadedPolygonUnion(geometries);
                nextFeature.setDefaultGeometry(unionOp.union());
            } else {
                nextFeature.setDefaultGeometry(geometries.get(0).getFactory()
                        .buildGeometry(geometries));
            }

            return nextFeature;
        }
    }
}
