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
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.geotools.process.impl.SingleProcessFactory;
import org.geotools.text.Text;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * Abstract SpatialStatistics Process Factory
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public abstract class SpatialStatisticsProcessFactory extends SingleProcessFactory {
    protected static final Logger LOGGER = Logging.getLogger(SpatialStatisticsProcessFactory.class);

    public final static String NAMESPACE = "statistics";

    public final static String VERSION = "1.0.0";

    static final ResourceBundle bundle;
    static {
        bundle = ResourceBundle.getBundle("resource", Locale.getDefault());
    }

    public static InternationalString getResource(String key) {
        return Text.text(bundle.getString(key));
    }

    public SpatialStatisticsProcessFactory(Name processName) {
        super(processName);
    }

    @Override
    protected String getVersion() {
        return VERSION;
    }

    @Override
    protected boolean supportsProgress() {
        return true;
    }
}
