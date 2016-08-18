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
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;

/**
 * GraduatedColorStyleBuilder
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class GraduatedColorStyleBuilder extends AbstractFeatureStyleBuilder {
    protected static final Logger LOGGER = Logging.getLogger(GraduatedColorStyleBuilder.class);

    String normalProperty;

    public void setNormalProperty(String normalProperty) {
        this.normalProperty = normalProperty;
    }

    public String getNormalProperty() {
        return normalProperty;
    }

    // Diverging: PuOr, BrBG, PRGn, PiYG, RdBu, RdGy, RdYlBu, Spectral, RdYlGn
    // Qualitative: Set1, Pastel1, Set2, Pastel2, Dark2, Set3, Paired, Accents,
    // Sequential: YlGn, YlGnBu, GnBu, BuGn, PuBuGn, PuBu, BuPu, RdPu, PuRd, OrRd, YlOrRd, YlOrBr,
    // Purples, Blues, Greens, Oranges, Reds, Grays,
    public Style createStyle(SimpleFeatureCollection inputFeatures, String propertyName,
            String methodName, int numClasses, String brewerPaletteName, boolean reverse) {
        numClasses = numClasses < 3 ? 3 : numClasses;
        if (numClasses > 12) {
            numClasses = numClasses > 12 ? 12 : numClasses;
            LOGGER.log(Level.WARNING, "maximum numClasses cannot exceed 12!");
        }

        // get classifier
        RangedClassifier classifier = null;
        if (normalProperty == null || normalProperty.isEmpty()) {
            classifier = getClassifier(inputFeatures, propertyName, methodName, numClasses);
        } else {
            classifier = getClassifier(inputFeatures, propertyName, normalProperty, methodName,
                    numClasses);
        }

        double[] classBreaks = getClassBreaks(classifier);

        if (brewerPaletteName == null || brewerPaletteName.isEmpty()) {
            brewerPaletteName = "OrRd"; // default
        }

        ColorBrewer brewer = ColorBrewer.instance();
        // brewer.loadPalettes();
        BrewerPalette brewerPalette = brewer.getPalette(brewerPaletteName);

        Color[] colors = brewerPalette.getColors(classBreaks.length - 1);
        if (reverse) {
            Collections.reverse(Arrays.asList(colors));
        }

        return createStyle(inputFeatures.getSchema(), propertyName, classBreaks, colors);
    }

    public Style createStyle(SimpleFeatureCollection inputFeatures, String propertyName,
            String methodName, int numClasses, String brewerPaletteName) {
        return createStyle(inputFeatures, propertyName, methodName, numClasses, brewerPaletteName,
                false);
    }

    public Style createStyle(SimpleFeatureType inputFeatureType, String propertyName,
            double[] classBreaks, Color[] colors) {
        if (classBreaks.length - 1 != colors.length) {
            LOGGER.log(Level.FINE, "classBreaks's length dose not equal colors's length");
            return null;
        }

        GeometryDescriptor geomDesc = inputFeatureType.getGeometryDescriptor();
        String geometryPropertyName = geomDesc.getLocalName();
        SimpleShapeType shapeType = FeatureTypes
                .getSimpleShapeType(geomDesc.getType().getBinding());

        FeatureTypeStyle fts = sf.createFeatureTypeStyle();
        for (int k = 0, length = classBreaks.length - 2; k <= length; k++) {
            final Color uvColor = colors[k];

            Symbolizer symbolizer = null;
            switch (shapeType) {
            case POINT:
                Mark mark = sf.getCircleMark();
                Stroke markStroke = sf.createStroke(ff.literal(Color.WHITE),
                        ff.literal(outlineWidth), ff.literal(outlineOpacity));
                mark.setStroke(markStroke);
                mark.setFill(sf.createFill(ff.literal(uvColor), ff.literal(fillOpacity)));

                Graphic graphic = sf.createDefaultGraphic();
                graphic.graphicalSymbols().clear();
                graphic.graphicalSymbols().add(mark);
                graphic.setSize(ff.literal(markerSize));

                symbolizer = sf.createPointSymbolizer(graphic, geometryPropertyName);
                break;
            case LINESTRING:
                Stroke lineStroke = sf.createStroke(ff.literal(uvColor), ff.literal(lineWidth),
                        ff.literal(lineOpacity));

                symbolizer = sf.createLineSymbolizer(lineStroke, geometryPropertyName);
                break;
            case POLYGON:
                Stroke outlineStroke = sf.createStroke(ff.literal(outlineColor),
                        ff.literal(outlineWidth), ff.literal(outlineOpacity));

                Fill fill = sf.createFill(ff.literal(uvColor), ff.literal(fillOpacity));
                symbolizer = sf.createPolygonSymbolizer(outlineStroke, fill, geometryPropertyName);
                break;
            }

            PropertyName property = ff.property(propertyName);
            Filter lower = ff.greaterOrEqual(property, ff.literal(classBreaks[k]));
            Filter upper = k == length ? ff.lessOrEqual(property, ff.literal(classBreaks[k + 1]))
                    : ff.less(property, ff.literal(classBreaks[k + 1]));
            
            Rule rule = sf.createRule();
            rule.setName(classBreaks[k] + " - " + classBreaks[k + 1]);
            rule.setFilter(ff.and(lower, upper));
            rule.symbolizers().add(symbolizer);

            fts.rules().add(rule);
        }

        Style style = sf.createStyle();
        style.featureTypeStyles().add(fts);

        return style;
    }

}
