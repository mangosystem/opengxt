package org.geotools.process.spatialstatistics.transformation;

import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;

/**
 * FieldCalculation SimpleFeatureCollection Implementation
 * 
 * @author MapPlus, onspatial.com
 */
public class FieldCalculationFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging
            .getLogger(FieldCalculationFeatureCollection.class);

    private String fieldName;

    private Expression expression;

    private SimpleFeatureType schema;

    public FieldCalculationFeatureCollection(SimpleFeatureCollection delegate, String fieldName,
            Expression expression) {
        super(delegate);

        this.fieldName = fieldName;
        this.expression = expression;

        schema = FeatureTypes.build(delegate, delegate.getSchema().getTypeName());
        
        // Field type: String, Integer, Double
        schema = FeatureTypes.add(schema, fieldName, Double.class, 38);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new FieldCalculationFeatureIterator(delegate.features(), getSchema(), fieldName,
                expression);
    }

    static class FieldCalculationFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private String fieldName;

        private SimpleFeatureBuilder builder;

        private Expression expression;

        public FieldCalculationFeatureIterator(SimpleFeatureIterator delegate,
                SimpleFeatureType schema, String fieldName, Expression expression) {
            this.delegate = delegate;

            this.fieldName = fieldName;
            this.expression = expression;
            builder = new SimpleFeatureBuilder(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature sourceFeature = delegate.next();
            SimpleFeature nextFeature = builder.buildFeature(sourceFeature.getID());

            // transfer attributes
            transferAttribute(sourceFeature, nextFeature);

            Object value = expression.evaluate(sourceFeature);
            nextFeature.setAttribute(fieldName, value);

            return nextFeature;
        }
    }
}
