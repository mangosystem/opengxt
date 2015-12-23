/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics.styler;

import java.awt.Color;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.TransformerException;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.UserLayer;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Style builder for spatial statistics
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class SSStyleBuilder {
    protected static final Logger LOGGER = Logging.getLogger(SSStyleBuilder.class);

    final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    final StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);

    static final Color LINE_COLOR = new Color(110, 110, 110);

    static final float LINE_WIDTH = 0.5f;

    private float opacity = 1.0f;

    private Stroke lineStroke;

    private SimpleFeatureType featureType = null;

    private String geometryField = null;

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public Stroke getLineStroke() {
        return lineStroke;
    }

    public void setLineStroke(Stroke lineStroke) {
        this.lineStroke = lineStroke;
    }

    public SSStyleBuilder(SimpleFeatureType featureType) {
        this.featureType = featureType;
        this.geometryField = featureType.getGeometryDescriptor().getLocalName();
        this.lineStroke = sf.createStroke(ff.literal(Color.WHITE), ff.literal(LINE_WIDTH));
    }

    public Style getZScoreStdDevStyle(String propertyName) {
        final String styleName = "Cluster / Outlier Analysis";

        final double[] classBreaks = { -2.58, -1.96, -1.65, 1.65, 1.96, 2.58 };

        final String[] classDescs = { "< -2.58 Std. Dev.", "-2.58 ~ -1.96 Std. Dev.",
                "-1.96 ~ -1.65 Std. Dev.", "-1.65 ~ 1.65 Std. Dev.", "1.65 ~ 1.96 Std. Dev.",
                "1.96 ~ 2.58 Std. Dev.", "> 2.58 Std. Dev." };

        final Color[] colorList = { new Color(69, 117, 181), new Color(132, 158, 186),
                new Color(192, 204, 190), new Color(255, 255, 191), new Color(250, 185, 132),
                new Color(237, 117, 81), new Color(214, 47, 39) };

        return buildStyle(propertyName, styleName, classBreaks, classDescs, colorList);
    }

    public Style getZScoreStyle(String propertyName) {
        final String styleName = "Cluster / Outlier Analysis";

        final double[] classBreaks = { -2.0, -1.0, 1.0, 2.0 };

        final String[] classDescs = { "< -2.0", "-2.0 ~ -1.0", "-1.0 ~ 1.0", "1.0 ~ 2.0", "> 2.0" };

        final Color[] colorList = { new Color(0, 92, 230), new Color(115, 255, 225),
                new Color(255, 255, 205), new Color(255, 215, 130), new Color(255, 0, 0) };

        return buildStyle(propertyName, styleName, classBreaks, classDescs, colorList);
    }

    public Style buildStyle(String propertyName, String styleName, double[] classBreaks,
            String[] classDescs, Color[] colorList) {
        FeatureTypeStyle featureTypeStyle = sf.createFeatureTypeStyle();
        featureTypeStyle.setName(styleName);

        for (int k = 0; k <= classBreaks.length; k++) {
            Fill fill = sf.createFill(ff.literal(colorList[k]), ff.literal(opacity));
            Symbolizer symbolizer = sf.createPolygonSymbolizer(lineStroke, fill, geometryField);
            if (classDescs != null && classDescs.length > k) {
                symbolizer.setName(classDescs[k]);
            }

            Filter filter = null;
            if (k == 0) {
                filter = ff.less(ff.property(propertyName), ff.literal(classBreaks[k]));
            } else if (k >= classBreaks.length) {
                filter = ff.greater(ff.property(propertyName), ff.literal(classBreaks[k - 1]));
            } else {
                filter = ff.between(ff.property(propertyName), ff.literal(classBreaks[k - 1]),
                        ff.literal(classBreaks[k]));
            }

            Rule rule = sf.createRule();
            rule.setName(classDescs[k]);
            rule.setFilter(filter);

            rule.symbolizers().add(symbolizer);
            featureTypeStyle.rules().add(rule);
        }

        Style style = sf.createStyle();
        style.featureTypeStyles().add(featureTypeStyle);
        style.setName(styleName);

        return style;
    }

    public Style getLISAStyle(String propertyName) {
        final String styleName = "LISA";
        final String[] classValues = { "HH", "LH", "LL", "HL", "" };
        final String[] classDescs = { "H-H", "L-H", "L-L", "H-L", "None" };

        // http://www.w3schools.com/tags/ref_color_tryit.asp
        // Color.Tomato, Color.LightGoldenrodYellow, Color.CornflowerBlue, Color.Thistle
        final Color[] colorList = { new Color(255, 99, 71), new Color(250, 250, 210),
                new Color(100, 149, 237), new Color(216, 191, 216), new Color(225, 225, 225) };

        FeatureTypeStyle featureTypeStyle = sf.createFeatureTypeStyle();
        featureTypeStyle.setName(styleName);

        for (int k = 0; k < classValues.length; k++) {
            Fill fill = sf.createFill(ff.literal(colorList[k]), ff.literal(opacity));

            Symbolizer symbolizer = sf.createPolygonSymbolizer(lineStroke, fill, geometryField);
            symbolizer.setName(classDescs[k]);

            Rule rule = sf.createRule();
            rule.setName(classDescs[k]);
            rule.setFilter(ff.equal(ff.property(propertyName), ff.literal(classValues[k]), false));

            rule.symbolizers().add(symbolizer);
            featureTypeStyle.rules().add(rule);
        }

        Style style = sf.createStyle();
        style.featureTypeStyles().add(featureTypeStyle);
        style.setName(styleName);

        return style;
    }

    public Style getDefaultFeatureStyle() {
        Style defaultStyle = null;

        Class<?> origBinding = featureType.getGeometryDescriptor().getType().getBinding();

        Random rnd = new Random();
        Color randomColor = Color.getHSBColor(rnd.nextFloat(), 1.0f, 1.0f);

        if (origBinding.isAssignableFrom(Point.class)) {
            defaultStyle = SLD.createPointStyle("Circle", Color.GRAY, randomColor, 1.0f, 4);
        } else if (origBinding.isAssignableFrom(MultiPoint.class)) {
            defaultStyle = SLD.createPointStyle("Circle", Color.GRAY, randomColor, 1.0f, 4);
        } else if (origBinding.isAssignableFrom(LineString.class)) {
            defaultStyle = SLD.createLineStyle(randomColor, 1f);
        } else if (origBinding.isAssignableFrom(MultiLineString.class)) {
            defaultStyle = SLD.createLineStyle(randomColor, 1f);
        } else if (origBinding.isAssignableFrom(Polygon.class)) {
            defaultStyle = SLD.createPolygonStyle(Color.GRAY, randomColor, 0.5f);
        } else if (origBinding.isAssignableFrom(MultiPolygon.class)) {
            defaultStyle = SLD.createPolygonStyle(Color.GRAY, randomColor, 0.5f);
        }

        return defaultStyle;
    }

    public String toXML(Style style) {
        if (style == null) {
            return null;
        }

        UserLayer layer = sf.createUserLayer();
        layer.setLayerFeatureConstraints(new FeatureTypeConstraint[] { null });
        layer.setName(featureType.getTypeName());
        layer.addUserStyle(style);

        StyledLayerDescriptor sld = sf.createStyledLayerDescriptor();
        sld.addStyledLayer(layer);
        try {
            SLDTransformer styleTransform = new SLDTransformer();
            return styleTransform.transform(sld);
        } catch (TransformerException te) {
            LOGGER.log(Level.FINE, te.getMessage(), te);
        }

        return null;
    }
}
