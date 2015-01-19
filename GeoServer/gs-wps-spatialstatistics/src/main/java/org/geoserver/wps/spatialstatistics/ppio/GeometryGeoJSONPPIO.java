/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.wps.ppio.CDataPPIO;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.util.logging.Logging;

import com.vividsolutions.jts.geom.Geometry;

public class GeometryGeoJSONPPIO extends CDataPPIO {
    protected static final Logger LOGGER = Logging.getLogger(GeometryGeoJSONPPIO.class);

    protected GeometryGeoJSONPPIO() {
        super(Geometry.class, Geometry.class, "application/json");
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return new GeometryJSON().read(new InputStreamReader(input));
    }

    @Override
    public Object decode(String input) throws Exception {
        return new GeometryJSON().read(new StringReader(input));
    }

    @Override
    public void encode(Object value, OutputStream os) throws IOException {
        Writer writer = new OutputStreamWriter(os);
        try {
            GeometryJSON geometryJson = new GeometryJSON();
            geometryJson.write((Geometry) value, writer);
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } finally {
            writer.flush();
        }
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

}
