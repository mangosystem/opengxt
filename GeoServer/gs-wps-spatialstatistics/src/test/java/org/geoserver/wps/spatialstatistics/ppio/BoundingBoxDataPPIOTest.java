/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;

public class BoundingBoxDataPPIOTest {

    @Test
    public void test() throws Exception {
        BoundingBoxDataPPIO ppio = new BoundingBoxDataPPIO();

        CoordinateReferenceSystem crs = null;
        ReferencedEnvelope envelope = new ReferencedEnvelope(1.0, 3.0, 1.0, 3.0, crs);

        // encode
        String encoded = ppio.encode(envelope);
        assertNotNull(encoded);

        // decode
        ReferencedEnvelope decoded = (ReferencedEnvelope) ppio.decode(encoded);
        assertNotNull(decoded);

        assertEquals(envelope, decoded);
    }
}
