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
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class GeometryGeoJSONPPIO extends CDataPPIO {
    protected static final Logger LOGGER = Logging.getLogger(GeometryGeoJSONPPIO.class);

    static final int DECIMALS = 10; // for EPSG:4326

    static final ObjectMapper MAPPER;

    static {
        MAPPER = JsonMapper.builder().addModule(new JtsModule(DECIMALS)).build();
    }

    protected GeometryGeoJSONPPIO() {
        super(Geometry.class, Geometry.class, "application/vnd.geo+json");
    }

    @Override
    public String getFileExtension() {
        return "json";
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
        return JsonMapper.builder().addModule(new JtsModule(decimals)).build();
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return MAPPER.readValue(input, Geometry.class);
    }

    @Override
    public Object decode(String input) throws Exception {
        return MAPPER.readValue(input, Geometry.class);
    }
}
