/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.geoserver.wps.ppio.BinaryPPIO;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.style.Style;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.MapToImageParam;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.util.logging.Logging;

/**
 * A PPIO to generate Image from featurecollection, gridcoverage2d
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class MapImagePPIO extends BinaryPPIO {
    protected static final Logger LOGGER = Logging.getLogger(MapImagePPIO.class);

    protected MapImagePPIO() {
        super(MapToImageParam.class, MapToImageParam.class, "image/png");
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        MapToImageParam info = (MapToImageParam) value;
        this.mimeType = info.getFormat();

        // prepare map context
        MapContent mapContent = new MapContent();
        mapContent.getViewport().setCoordinateReferenceSystem(info.getSrs());

        Style style = info.getStyle();
        if (info.getInputFeatures() == null) {
            mapContent.layers().add(new GridCoverageLayer(info.getInputCoverage(), style));
        } else {
            // convert string type to number type
            Filter filter = info.getFilter();
            SimpleFeatureCollection sfc = info.getInputFeatures();
            if (filter != Filter.INCLUDE && filter != null) {
                sfc = retypeCheck(info.getInputFeatures()).subCollection(filter);
            }
            mapContent.layers().add(new FeatureLayer(sfc, style));
        }
        mapContent.getViewport().setBounds(info.getMapExtent());

        // export map
        GTRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(mapContent);

        RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        renderer.setJava2DHints(hints);

        Map<Object, Object> rendererParams = new HashMap<Object, Object>();
        rendererParams.put("optimizedDataLoadingEnabled", Boolean.TRUE);
        renderer.setRendererHints(rendererParams);

        BufferedImage image = new BufferedImage(info.getWidth(), info.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        Rectangle paintArea = new Rectangle(0, 0, info.getWidth(), info.getHeight());
        ReferencedEnvelope mapArea = mapContent.getViewport().getBounds();
        if (info.getTransparent()) {
            renderer.paint(graphics, paintArea, mapArea);
        } else {
            graphics.setPaint(info.getBackgroundColor());
            graphics.fill(paintArea);
            renderer.paint(graphics, paintArea, mapArea);
        }

        // cleanup
        graphics.dispose();
        mapContent.dispose();

        // write image
        ImageIO.write(image, getFileExtension(), os);
    }

    private SimpleFeatureCollection retypeCheck(SimpleFeatureCollection sfc) {
        SimpleFeatureType schema = sfc.getSchema();

        // 1. check value
        Map<String, String> fieldMap = new HashMap<String, String>();
        SimpleFeatureIterator featureIter = sfc.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
                    if (descriptor instanceof GeometryDescriptor) {
                        continue;
                    } else {
                        final String propertyName = descriptor.getLocalName();
                        Object val = feature.getAttribute(propertyName);
                        try {
                            if (val != null) {
                                Double.parseDouble(val.toString().trim());
                                fieldMap.put(propertyName, propertyName);
                            }
                        } catch (NumberFormatException e) {
                            LOGGER.log(Level.FINEST, e.getLocalizedMessage(), e);
                        }
                    }
                }
                break;
            }
        } finally {
            featureIter.close();
        }

        // 2. if required, retype schema
        if (fieldMap.size() > 0) {
            SimpleFeatureTypeBuilder sftBuilder = new SimpleFeatureTypeBuilder();
            sftBuilder.setNamespaceURI(FeatureTypes.NAMESPACE_URL);
            sftBuilder.setName(schema.getName());
            sftBuilder.setCRS(schema.getCoordinateReferenceSystem());

            for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
                if (descriptor instanceof GeometryDescriptor) {
                    sftBuilder.add(descriptor);
                } else {
                    final String propertyName = descriptor.getLocalName();
                    if (fieldMap.containsKey(propertyName)) {
                        sftBuilder.add(propertyName, Number.class);
                    } else {
                        sftBuilder.add(descriptor);
                    }
                }
            }

            // return retyping features
            SimpleFeatureType retypeSchema = sftBuilder.buildFeatureType();
            ListFeatureCollection featureCollection = new ListFeatureCollection(retypeSchema);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(retypeSchema);

            featureIter = sfc.features();
            try {
                while (featureIter.hasNext()) {
                    SimpleFeature feature = featureIter.next();
                    SimpleFeature newFeature = builder.buildFeature(feature.getID());
                    for (AttributeDescriptor descriptor : retypeSchema.getAttributeDescriptors()) {
                        String name = descriptor.getLocalName();
                        if (descriptor instanceof GeometryDescriptor) {
                            newFeature.setDefaultGeometry(feature.getDefaultGeometry());
                        } else {
                            Object val = feature.getAttribute(name);
                            if (descriptor.getType().getBinding().isAssignableFrom(Number.class)) {
                                newFeature.setAttribute(name, Double.parseDouble(val.toString()));
                            } else {
                                newFeature.setAttribute(name, val);
                            }
                        }
                    }
                    featureCollection.add(newFeature);
                }
            } finally {
                featureIter.close();
            }
            return featureCollection;
        }
        return sfc;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        // nothing to do
        return null;
    }

    @Override
    public String getFileExtension() {
        int pos = mimeType.lastIndexOf("/");
        return mimeType.substring(pos + 1);
    }
}
