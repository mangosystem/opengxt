/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.wps.ppio.XMLPPIO;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.ContentHandler;

/**
 * A PPIO to generate good looking xml for the KML (formerly Keyhole Markup Language) results
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class FeatureCollectionKML21PPIO extends XMLPPIO {
    protected static final Logger LOGGER = Logging.getLogger(FeatureCollectionKML21PPIO.class);

    org.geotools.xml.Configuration configuration = new org.geotools.kml.KMLConfiguration();

    protected FeatureCollectionKML21PPIO() {
        super(FeatureCollection.class, FeatureCollection.class, "text/xml; subtype=kml/2.1",
                org.geotools.kml.KML.kml);
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        Parser p = new Parser(configuration);
        return p.parse(input);
    }

    @Override
    public void encode(Object object, ContentHandler handler) throws Exception {
        SimpleFeatureCollection features = (SimpleFeatureCollection) object;
        CoordinateReferenceSystem nativeCrs = features.getSchema().getCoordinateReferenceSystem();

        SimpleFeatureCollection wgs84Features = features;
        if (nativeCrs != null) {
            try {
                CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:4326", true);
                if (!CRS.equalsIgnoreMetadata(nativeCrs, targetCrs)) {
                    wgs84Features = new ReprojectingFeatureCollection(features, targetCrs);
                }
            } catch (FactoryException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }

        Encoder encoder = new Encoder(configuration);
        encoder.setIndenting(true);
        encoder.setIndentSize(2);
        encoder.encode(wgs84Features, element, handler);
    }
}
