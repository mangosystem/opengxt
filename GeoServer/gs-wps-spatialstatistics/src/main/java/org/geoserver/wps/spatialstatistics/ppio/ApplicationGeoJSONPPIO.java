/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geoserver.wps.ppio.CDataPPIO;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Inputs and outputs feature collections in GeoJSON format using gt-geojson
 * 
 * @author Andrea Aime - OpenGeo
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class ApplicationGeoJSONPPIO extends CDataPPIO {

    static final int DECIMALS = 10; // for EPSG:4326

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

        @SuppressWarnings("rawtypes")
        @Override
        public void encode(Object value, OutputStream os) throws IOException {
            FeatureJSON json = new FeatureJSON(new GeometryJSON(DECIMALS));
            // commented out due to GEOT-3209
            // json.setEncodeFeatureCRS(true);
            // json.setEncodeFeatureCollectionCRS(true);
            json.writeFeatureCollection((FeatureCollection) value, os);
        }

        @Override
        public Object decode(InputStream input) throws Exception {
            return new FeatureJSON(new GeometryJSON(DECIMALS)).readFeatureCollection(input);
        }

        @Override
        public Object decode(String input) throws Exception {
            return new FeatureJSON(new GeometryJSON(DECIMALS)).readFeatureCollection(input);
        }
    }

    public static class Geometries2 extends ApplicationGeoJSONPPIO {

        public Geometries2() {
            super(Geometry.class);
        }

        @Override
        public void encode(Object value, OutputStream os) throws IOException {
            GeometryJSON json = new GeometryJSON(DECIMALS);
            json.write((Geometry) value, os);
        }

        @Override
        public Object decode(InputStream input) throws Exception {
            return new GeometryJSON(DECIMALS).read(input);
        }

        @Override
        public Object decode(String input) throws Exception {
            return new GeometryJSON(DECIMALS).read(input);
        }
    }
}