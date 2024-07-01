/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.expression.Expression;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

public class ExpressionPPIOTest {

    private WKTReader reader = new WKTReader();

    private SimpleFeature feature;

    @Before
    public void setup() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.add("geom", Polygon.class, "EPSG:4326");
        tb.add("area", Double.class);
        tb.setName("circle");
        SimpleFeatureType ft = tb.buildFeatureType();

        Geometry geom = reader.read("POINT(0 0)").buffer(10);

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(ft);
        fb.add(geom);
        fb.add(geom.getArea());
        feature = fb.buildFeature(null);
    }

    @Test
    public void test() throws Exception {
        ExpressionPPIO ppio = new ExpressionPPIO();

        String ecqlExpression = "[area] / 100";
        Expression expression = ECQL.toExpression(ecqlExpression);

        // encode
        String encoded = ppio.encode(expression);
        assertNotNull(encoded);

        // decode
        Expression decoded = (Expression) ppio.decode(encoded);
        assertEquals(expression, decoded);

        Geometry geom = (Geometry) feature.getDefaultGeometry();
        assertEquals(decoded.evaluate(feature, Double.class), Double.valueOf(geom.getArea() / 100));
    }

}
