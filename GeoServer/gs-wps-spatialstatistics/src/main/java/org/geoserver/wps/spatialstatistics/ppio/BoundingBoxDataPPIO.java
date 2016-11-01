/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A PPIO to generate BoundingBoxData: format = minx, miny, maxx, maxy, srid
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class BoundingBoxDataPPIO extends LiteralPPIO {

    public BoundingBoxDataPPIO() {
        super(ReferencedEnvelope.class);
    }

    /**
     * Decodes the parameter (as a string) to its internal object implementation.
     */
    public Object decode(String value) throws Exception {
        if (value == null) {
            return null;
        }

        String[] coordinates = value.split(",");
        if (coordinates.length < 4) {
            return null;
        }

        double x1 = Double.parseDouble(coordinates[0]);
        double y1 = Double.parseDouble(coordinates[1]);
        double x2 = Double.parseDouble(coordinates[2]);
        double y2 = Double.parseDouble(coordinates[3]);

        CoordinateReferenceSystem crs = null;
        if (coordinates.length > 4) {
            crs = CRS.decode(coordinates[4]);
        }

        return new ReferencedEnvelope(x1, x2, y1, y2, crs);
    }

    /**
     * Encodes the internal object representation of a parameter as a string.
     */
    public String encode(Object value) throws Exception {
        if (value == null) {
            return null;
        }

        ReferencedEnvelope bbox = (ReferencedEnvelope) value;

        StringBuffer buffer = new StringBuffer();
        buffer.append(bbox.getMinX()).append(",");
        buffer.append(bbox.getMinY()).append(",");
        buffer.append(bbox.getMaxX()).append(",");
        buffer.append(bbox.getMaxY());

        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        if (crs != null) {
            try {
                Integer code = CRS.lookupEpsgCode(crs, false);
                if (code != null) {
                    buffer.append(",").append(code.toString());
                }
            } catch (Exception e) {
                throw new WPSException("Could not lookup epsg code for " + crs, e);
            }
        }
        return buffer.toString();
    }

}
