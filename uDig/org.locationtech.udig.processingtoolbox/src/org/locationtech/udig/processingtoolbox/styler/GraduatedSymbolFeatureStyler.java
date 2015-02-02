package org.locationtech.udig.processingtoolbox.styler;

import org.geotools.styling.Style;

public class GraduatedSymbolFeatureStyler extends OutputStyler {

    public GraduatedSymbolFeatureStyler(Object source, String functionName, String fieldName,
            int numClasses, String brewerPaletteName) {
        super(source);
    }

    @Override
    public Style getStyle() {
        return null;
    }

}
