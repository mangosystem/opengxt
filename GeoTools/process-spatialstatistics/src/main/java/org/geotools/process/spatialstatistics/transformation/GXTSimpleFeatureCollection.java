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

    protected static void transferAttribute(SimpleFeature source, SimpleFeature target) {
        List<AttributeDescriptor> attributes = source.getFeatureType().getAttributeDescriptors();
        for (AttributeDescriptor attr : attributes) {
            String attributeName = attr.getLocalName();
            target.setAttribute(attributeName, source.getAttribute(attributeName));
        }
    }
}
