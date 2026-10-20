package org.geoserver.wps.spatialstatistics.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.geoserver.wps.ppio.CDataPPIO;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

public class GeoJSONPPIOTest {

    @Test
    public void geometryRoundTrip() throws Exception {
        assertGeometryRoundTrip(new GeometryGeoJSONPPIO());
    }

    @Test
    public void applicationGeometryRoundTrip() throws Exception {
        assertGeometryRoundTrip(new ApplicationGeoJSONPPIO.Geometries2());
    }

    private void assertGeometryRoundTrip(CDataPPIO ppio) throws Exception {
        Geometry expected = new GeometryFactory().createPoint(
                new Coordinate(127.1234567891, 37.1234567891));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ppio.encode(expected, output);

        Geometry fromStream = (Geometry) ppio.decode(
                new ByteArrayInputStream(output.toByteArray()));
        Geometry fromString = (Geometry) ppio.decode(
                output.toString(StandardCharsets.UTF_8));
        assertTrue(expected.equalsExact(fromStream, 1e-10));
        assertTrue(expected.equalsExact(fromString, 1e-10));
    }

    @Test
    public void featureCollectionRoundTrip() throws Exception {
        String json = "{\"type\":\"FeatureCollection\",\"features\":["
                + "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\","
                + "\"coordinates\":[127.1,37.2]},\"properties\":{\"name\":\"Seoul\"}}]}";
        ApplicationGeoJSONPPIO.FeatureCollections2 ppio =
                new ApplicationGeoJSONPPIO.FeatureCollections2();
        SimpleFeatureCollection features = (SimpleFeatureCollection) ppio.decode(json);
        assertEquals(1, features.size());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ppio.encode(features, output);
        SimpleFeatureCollection decoded = (SimpleFeatureCollection) ppio.decode(
                new ByteArrayInputStream(output.toByteArray()));
        assertEquals(1, decoded.size());
        try (var iterator = decoded.features()) {
            var feature = iterator.next();
            assertEquals("Seoul", feature.getAttribute("name"));
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            assertEquals(127.1, geometry.getCoordinate().x, 1e-10);
            assertEquals(37.2, geometry.getCoordinate().y, 1e-10);
        }
    }
}
