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
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;

/**
 * GraduatedSymbolStyleBuilder
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
@SuppressWarnings("nls")
public class GraduatedSymbolStyleBuilder extends AbstractFeatureStyleBuilder {
    protected static final Logger LOGGER = Logging.getLogger(GraduatedSymbolStyleBuilder.class);

    PolygonSymbolizer backgroundSymbol;

    Symbolizer templateSymbol;

    String propertyName;

    String normalProperty;

    String methodName = "EqualInterval";

    int numClasses = 5;

    float minSize = 2;

    float maxSize = 24;

    public PolygonSymbolizer getBackgroundSymbol() {
        return backgroundSymbol;
    }

    public void setBackgroundSymbol(PolygonSymbolizer backgroundSymbol) {
        this.backgroundSymbol = backgroundSymbol;
    }

    public Symbolizer getTemplateSymbol() {
        return templateSymbol;
    }

    public void setTemplateSymbol(Symbolizer templateSymbol) {
        this.templateSymbol = templateSymbol;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getNumClasses() {
        return numClasses;
    }

    public void setNumClasses(int numClasses) {
        this.numClasses = numClasses;
    }

    public float getMinSize() {
        return minSize;
    }

    public void setMinSize(float minSize) {
        this.minSize = minSize;
    }

    public float getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(float maxSize) {
        this.maxSize = maxSize;
    }

    public void setNormalProperty(String normalProperty) {
        this.normalProperty = normalProperty;
    }

    public String getNormalProperty() {
        return normalProperty;
    }

    private void setDefaultSymbolizer(SimpleFeatureCollection inputFeatures) {
        GeometryDescriptor geomDesc = inputFeatures.getSchema().getGeometryDescriptor();
        String geometryPropertyName = geomDesc.getLocalName();
        Class<?> geomBinding = geomDesc.getType().getBinding();
        SimpleShapeType shapeType = FeatureTypes.getSimpleShapeType(geomBinding);

        switch (shapeType) {
        case POINT:
        case POLYGON:
            // default Point Symbolizer
            Mark mark = sf.getCircleMark();
            // createStroke(Expression color, Expression width, Expression opacity)
            Stroke markStroke = sf.createStroke(ff.literal(Color.WHITE), ff.literal(outlineWidth),
                    ff.literal(outlineOpacity));
            mark.setStroke(markStroke);
            mark.setFill(sf.createFill(ff.literal(Color.RED), ff.literal(fillOpacity)));

            Graphic graphic = sf.createDefaultGraphic();
            graphic.graphicalSymbols().clear();
            graphic.graphicalSymbols().add(mark);
            graphic.setSize(ff.literal(minSize));

            this.templateSymbol = sf.createPointSymbolizer(graphic, null);
            break;
        case LINESTRING:
            // Default Line Symbolizer
            Stroke stroke = sf.createStroke(ff.literal(new Color(0, 112, 255)),
                    ff.literal(lineWidth), ff.literal(outlineOpacity));

            this.templateSymbol = sf.createLineSymbolizer(stroke, geometryPropertyName);
            break;
        }
    }

    public Style createStyle(SimpleFeatureCollection inputFeatures, String propertyName) {
        GeometryDescriptor geomDesc = inputFeatures.getSchema().getGeometryDescriptor();
        String geometryPropertyName = geomDesc.getLocalName();
        SimpleShapeType shapeType = FeatureTypes.getSimpleShapeType(inputFeatures);

        if (this.templateSymbol == null) {
            setDefaultSymbolizer(inputFeatures);
        }

        numClasses = numClasses < 3 ? 3 : numClasses;

        // get classifier
        RangedClassifier classifier = null;
        if (normalProperty == null || normalProperty.isEmpty()) {
            classifier = getClassifier(inputFeatures, propertyName, methodName, numClasses);
        } else {
            classifier = getClassifier(inputFeatures, propertyName, normalProperty, methodName,
                    numClasses);
        }

        double[] classBreaks = getClassBreaks(classifier);
        int step = (int) ((maxSize + minSize) / classBreaks.length) + 1;

        FeatureTypeStyle featureTypeStyle = sf.createFeatureTypeStyle();

        // add background symbol
        if (shapeType == SimpleShapeType.POLYGON) {
            if (backgroundSymbol == null) {
                Stroke stroke = sf.createStroke(ff.literal(outlineColor),
                        ff.literal(outlineOpacity));
                Fill fill = sf.createFill(ff.literal(new Color(190, 255, 232)),
                        ff.literal(fillOpacity));
                backgroundSymbol = sf.createPolygonSymbolizer(stroke, fill, geometryPropertyName);
            }

            Rule rule = sf.createRule();
            rule.setName("Background");
            rule.symbolizers().add(backgroundSymbol);
            featureTypeStyle.rules().add(rule);
        }

        DuplicatingStyleVisitor styleVisitor = new DuplicatingStyleVisitor();
        PropertyName property = ff.property(propertyName);
        for (int k = 0, length = classBreaks.length - 2; k <= length; k++) {
            float size = minSize + (step * k);

            Symbolizer symbolizer = null;
            if (templateSymbol instanceof PointSymbolizer) {
                PointSymbolizer pointSymbolizer = (PointSymbolizer) templateSymbol;

                Graphic graphic = pointSymbolizer.getGraphic();
                graphic.accept(styleVisitor);

                Graphic copy = (Graphic) styleVisitor.getCopy();
                copy.setSize(ff.literal(size));

                symbolizer = sf.createPointSymbolizer(copy, geometryPropertyName);
            } else {
                LineSymbolizer lineSymbolizer = (LineSymbolizer) templateSymbol;

                Stroke stroke = lineSymbolizer.getStroke();
                stroke.accept(styleVisitor);

                Stroke copy = (Stroke) styleVisitor.getCopy();
                stroke.setWidth(ff.literal(size));

                symbolizer = sf.createLineSymbolizer(copy, geometryPropertyName);
            }
            
            Filter lower = ff.greaterOrEqual(property, ff.literal(classBreaks[k]));
            Filter upper = k == length ? ff.lessOrEqual(property, ff.literal(classBreaks[k + 1]))
                    : ff.less(property, ff.literal(classBreaks[k + 1]));
            
            Rule rule = sf.createRule();
            rule.setName(classBreaks[k] + " - " + classBreaks[k + 1]);
            rule.setFilter(ff.and(lower, upper));
            rule.symbolizers().add(symbolizer);

            featureTypeStyle.rules().add(rule);
        }

        Style style = sf.createStyle();
        style.featureTypeStyles().add(featureTypeStyle);

        return style;
    }

}
