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

import java.util.Iterator;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.process.gs.WrappingIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;

/**
 * A FeatureCollection which completely delegates to another FeatureCollection.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class GXTSimpleFeatureCollection extends DecoratingSimpleFeatureCollection {

    protected final static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    protected GXTSimpleFeatureCollection(SimpleFeatureCollection delegate) {
        super(delegate);
    }

    public Iterator<SimpleFeature> iterator() {
        return new WrappingIterator(features());
    }

    public void close(Iterator<SimpleFeature> close) {
        if (close instanceof WrappingIterator) {
            ((WrappingIterator) close).close();
        }
    }

    protected static String buildID(String typeName, int id) {
        return new StringBuilder().append(typeName).append(".").append(id).toString();
    }

    protected static void transferAttribute(SimpleFeature source, SimpleFeature target) {
        List<AttributeDescriptor> attributes = source.getFeatureType().getAttributeDescriptors();
        for (AttributeDescriptor attr : attributes) {
            String attributeName = attr.getLocalName();
            if (target.getFeatureType().indexOf(attributeName) == -1) {
                continue;
            }
            target.setAttribute(attributeName, source.getAttribute(attributeName));
        }
    }
}
