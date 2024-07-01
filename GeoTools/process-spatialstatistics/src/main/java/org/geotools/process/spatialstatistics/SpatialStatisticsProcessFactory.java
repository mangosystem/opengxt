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
package org.geotools.process.spatialstatistics;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.util.InternationalString;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.process.impl.SingleProcessFactory;
import org.geotools.text.Text;
import org.geotools.util.logging.Logging;

/**
 * Abstract SpatialStatistics Process Factory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class SpatialStatisticsProcessFactory extends SingleProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(SpatialStatisticsProcessFactory.class);

    protected static final String NAMESPACE = "statistics";

    protected static final String VERSION = "1.0.0";

    protected static final FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    static final ResourceBundle bundle;
    static {
        bundle = ResourceBundle.getBundle("resource", Locale.getDefault());
    }

    protected static InternationalString getResource(String key) {
        try {
            return Text.text(bundle.getString(key));
        } catch (MissingResourceException mre) {
            return Text.text(key);
        }
    }

    public SpatialStatisticsProcessFactory(Name processName) {
        super(processName);
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public boolean supportsProgress() {
        return true;
    }
}
