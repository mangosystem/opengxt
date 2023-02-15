/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.geoserver.wps.ppio.CDataPPIO;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Inputs and outputs feature collections in GeoJSON format using gt-geojson
 * 
 * @author Andrea Aime - OpenGeo
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class ApplicationGeoJSONPPIO extends CDataPPIO {
    protected static final Logger LOGGER = Logging.getLogger(ApplicationGeoJSONPPIO.class);

    static final int DECIMALS = 10; // for EPSG:4326

    private static JsonFactory JSON_FACTORY = new JsonFactory();;

    static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        JtsModule module = new JtsModule(DECIMALS);
        MAPPER.registerModule(module);
    }

    @SuppressWarnings("rawtypes")
    public ApplicationGeoJSONPPIO(Class clazz) {
        super(clazz, clazz, "application/vnd.geo+json");
    }

    @Override
    public abstract void encode(Object value, OutputStream os) throws IOException;

    @Override
    public abstract Object decode(InputStream input) throws Exception;

    @Override
    public abstract Object decode(String input) throws Exception;

    @Override
    public final String getFileExtension() {
        return "json";
    }

    public static class FeatureCollections2 extends ApplicationGeoJSONPPIO {

        public FeatureCollections2() {
            super(FeatureCollection.class);
        }

        @Override
        public void encode(Object value, OutputStream os) throws IOException {
            try (GeoJSONWriter writer = new GeoJSONWriter(os)) {
                writer.setMaxDecimals(DECIMALS);
                writer.writeFeatureCollection((SimpleFeatureCollection) value);
            }
        }

        @Override
        public Object decode(InputStream input) throws Exception {
            try (GeoJSONReader reader = new GeoJSONReader(input)) {
                return reader.getFeatures();
            }
        }

        @Override
        public Object decode(String input) throws Exception {
            try (GeoJSONReader reader = new GeoJSONReader(input)) {
                return reader.getFeatures();
            }
        }
    }

    public static class Geometries2 extends ApplicationGeoJSONPPIO {

        public Geometries2() {
            super(Geometry.class);
        }

        @Override
        public void encode(Object value, OutputStream os) throws IOException {
            ObjectMapper mapper = getMapper(DECIMALS);
            mapper.writeValue(os, value);
        }

        private ObjectMapper getMapper(int decimals) {
            if (decimals == DECIMALS) {
                return MAPPER;
            }
            ObjectMapper mapper = new ObjectMapper();
            JtsModule module = new JtsModule(decimals);
            mapper.registerModule(module);
            return mapper;
        }

        @Override
        public Object decode(InputStream input) throws Exception {
            try (JsonParser parser = JSON_FACTORY.createParser(input)) {
                return MAPPER.readValue(parser, Geometry.class);
            }
        }

        @Override
        public Object decode(String input) throws Exception {
            try (JsonParser parser = JSON_FACTORY.createParser(input)) {
                return MAPPER.readValue(parser, Geometry.class);
            }
        }
    }
}