/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox.tools;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Configuration;
import org.locationtech.udig.processingtoolbox.ToolboxPlugin;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Format Transformer
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class FormatTransformer {
    final static Logger LOGGER = Logging.getLogger(FormatTransformer.class);

    public enum EncodeType {
        GML212(0), GML311(1), GML32(2), GEOJSON(3), KML21(4), KML22(5), CSV(6), WKT(7);

        private final int value;

        EncodeType(int value) {
            this.value = value;
        }

        static final Map<Integer, EncodeType> map = new HashMap<Integer, EncodeType>();
        static {
            for (EncodeType type : EncodeType.values()) {
                map.put(type.value, type);
            }
        }

        public static EncodeType valueOf(int index) {
            EncodeType type = map.get(Integer.valueOf(index));
            if (type == null) {
                return EncodeType.GML311; // default
            }
            return type;
        }
    }

    private EncodeType encodeType = EncodeType.GML311;

    public FormatTransformer(EncodeType encodeType) {
        this.encodeType = encodeType;
    }

    public String getExtension() {
        switch (encodeType) {
        case GML212:
        case GML311:
        case GML32:
            return ".gml";
        case KML21:
        case KML22:
            return ".kml";
        case GEOJSON:
            return ".json";
        case CSV:
            return ".csv";
        case WKT:
            return ".wkt";
        default:
            return ".gml";
        }
    }

    public void encode(SimpleFeatureCollection features, File outputFile) throws IOException {
        switch (encodeType) {
        case CSV:
            encodeCSV(features, outputFile, Charset.defaultCharset(), "|");
            break;
        case WKT:
            encodeWKT(features, outputFile, Charset.defaultCharset(), "|");
            break;
        case GEOJSON:
            encodeGeoJSON(features, outputFile);
            break;
        case GML212:
        case GML311:
        case GML32:
            encodeGML(features, outputFile);
            break;
        case KML21:
        case KML22:
            encodeKML(features, outputFile);
            break;
        default:
            encodeGML(features, outputFile);
            break;
        }
    }

    public void encodeKML(SimpleFeatureCollection features, File outputFile) throws IOException {
        SimpleFeatureCollection wgs84 = features;
        CoordinateReferenceSystem sourceCRS = wgs84.getSchema().getCoordinateReferenceSystem();
        try {
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
            if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
                wgs84 = new ReprojectingFeatureCollection(features, targetCRS);
            }
        } catch (NoSuchAuthorityCodeException e) {
            ToolboxPlugin.log(e.getMessage());
        } catch (FactoryException e) {
            ToolboxPlugin.log(e.getMessage());
        }

        QName qName = org.geotools.kml.KML.kml;
        org.geotools.xml.Configuration config = new org.geotools.kml.KMLConfiguration();
        if (encodeType == EncodeType.KML22) {
            qName = org.geotools.kml.v22.KML.kml;
            config = new org.geotools.kml.v22.KMLConfiguration();
        }

        write(config, qName, wgs84, outputFile);
    }

    public void encodeGML(SimpleFeatureCollection features, File outputFile) throws IOException {
        QName qName;
        org.geotools.xml.Configuration config;

        switch (encodeType) {
        case GML212:
            qName = org.geotools.gml2.GML._FeatureCollection;
            config = new org.geotools.gml2.GMLConfiguration();
            // config = new org.geotools.wfs.v1_0.WFSConfiguration();
            break;
        case GML311:
            qName = org.geotools.gml3.GML.FeatureCollection;
            config = new org.geotools.gml3.GMLConfiguration();
            // config = new org.geotools.wfs.v1_1.WFSConfiguration();
            break;
        case GML32:
            qName = org.geotools.gml3.v3_2.GML.FeatureCollection;
            config = new org.geotools.gml3.v3_2.GMLConfiguration();
            // config = new org.geotools.wfs.v2_0.WFSConfiguration();
            break;
        default:
            qName = org.geotools.gml3.GML.FeatureCollection;
            config = new org.geotools.gml3.GMLConfiguration();
            break;
        }

        write(config, qName, features, outputFile);
    }

    private void write(Configuration config, QName qName, SimpleFeatureCollection features,
            File outputFile) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            org.geotools.xml.Encoder encoder = new org.geotools.xml.Encoder(config);
            encoder.setIndenting(true);
            encoder.setIndentSize(2);
            encoder.encode(features, qName, fos);
        } catch (FileNotFoundException e) {
            ToolboxPlugin.log(e.getMessage());
        } finally {
            closeQuietly(fos);
        }
    }

    public void encodeCSV(SimpleFeatureCollection features, File outputFile, Charset charset,
            String delimeter) throws IOException {
        BufferedWriter writer = null;
        try {
            String newLine = System.getProperty("line.separator"); //$NON-NLS-1$
            FileOutputStream fos = new FileOutputStream(outputFile);
            writer = new BufferedWriter(new OutputStreamWriter(fos, charset));

            // write fields
            SimpleFeatureType schema = features.getSchema();
            StringBuffer sb = new StringBuffer();
            for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
                if (descriptor instanceof GeometryDescriptor) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(delimeter);
                }
                sb.append(descriptor.getLocalName());
            }
            sb.append(delimeter).append("xcoord").append(delimeter).append("ycoord")
                    .append(newLine);
            writer.write(sb.toString());

            // write contents
            SimpleFeatureIterator featureIter = null;
            try {
                featureIter = features.features();
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    Geometry origGeom = (Geometry) feature.getDefaultGeometry();
                    Coordinate coordinate = origGeom.getCentroid().getCoordinate();

                    sb.setLength(0);
                    for (Object value : feature.getAttributes()) {
                        if (value instanceof Geometry) {
                            continue;
                        }
                        if (sb.length() > 0) {
                            sb.append(delimeter);
                        }
                        sb.append(value == null ? "" : value.toString());
                    }
                    sb.append(delimeter).append(coordinate.x);
                    sb.append(delimeter).append(coordinate.y).append(newLine);
                    writer.write(sb.toString());
                }
            } finally {
                featureIter.close();
            }
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            ToolboxPlugin.log(e.getMessage());
        } finally {
            closeQuietly(writer);
        }
    }

    public void encodeWKT(SimpleFeatureCollection features, File outputFile, Charset charset,
            String delimeter) throws IOException {
        BufferedWriter writer = null;
        try {
            String newLine = System.getProperty("line.separator"); //$NON-NLS-1$
            FileOutputStream fos = new FileOutputStream(outputFile);
            writer = new BufferedWriter(new OutputStreamWriter(fos, charset));

            boolean isComma = delimeter.equals(",");

            // write fields
            SimpleFeatureType schema = features.getSchema();
            StringBuffer sb = new StringBuffer();
            for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
                if (sb.length() > 0) {
                    sb.append(delimeter);
                }
                sb.append(descriptor.getLocalName());
            }
            writer.write(sb.append(newLine).toString());

            // write contents
            SimpleFeatureIterator featureIter = null;
            try {
                featureIter = features.features();
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    Geometry origGeom = (Geometry) feature.getDefaultGeometry();

                    sb.setLength(0);
                    for (Object value : feature.getAttributes()) {
                        if (sb.length() > 0) {
                            sb.append(delimeter);
                        }
                        if (value instanceof Geometry) {
                            if (isComma) {
                                sb.append("\"").append(origGeom.toText()).append("\"");
                            } else {
                                sb.append(origGeom.toText());
                            }
                        } else {
                            sb.append(value == null ? "" : value.toString());
                        }
                    }
                    writer.write(sb.append(newLine).toString());
                }
            } finally {
                featureIter.close();
            }
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            ToolboxPlugin.log(e.getMessage());
        } finally {
            closeQuietly(writer);
        }
    }

    public void encodeGeoJSON(SimpleFeatureCollection features, File outputFile) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            FeatureJSON featureJson = new FeatureJSON();
            featureJson.setEncodeFeatureCollectionCRS(true);
            featureJson.writeFeatureCollection(features, fos);
        } catch (FileNotFoundException e) {
            ToolboxPlugin.log(e.getMessage());
        } finally {
            closeQuietly(fos);
        }
    }

    private void closeQuietly(Closeable io) {
        try {
            if (io != null) {
                io.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }

}
