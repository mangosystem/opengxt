package org.locationtech.udig.processingtoolbox.styler;

import org.geotools.styling.Style;

public class GraduatedColorFeatureStyler extends OutputStyler {

    public GraduatedColorFeatureStyler(Object source, String functionName, String fieldName,
            int numClasses, String brewerPaletteName) {
        super(source);
    }

    @Override
    public Style getStyle() {
        return null;
    }

}
