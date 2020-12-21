/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.web;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.util.MapModel;

import com.sun.media.imageioimpl.common.PackageUtil;

/**
 * General information about OpenGXT.
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class OpenGXTPage extends AbstractSecurityPage {
    /** serialVersionUID */
    private static final long serialVersionUID = 5599912424592834907L;

    private final Map<String, String> values;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OpenGXTPage() {
        values = new HashMap<String, String>();
        updateModel();

        // 1. status
        add(new ExternalLink("gxt.author", "http://www.mangosystem.com", "Mango System"));
        add(new ExternalLink("gxt.source",
                "https://github.com/mangosystem/opengxt/", "GitHub"));

        add(new ExternalLink("gxt.license",
                "https://docs.geoserver.org/latest/en/user/introduction/license.html",
                "GNU General Public License"));

        add(new Label("gxt.address", new MapModel(values, "gxt.address")));
        add(new Label("gxt.tel", new MapModel(values, "gxt.tel")));
        add(new Label("gxt.contact", new MapModel(values, "gxt.contact")));

        add(new Label("gxt.version", new MapModel(values, "gxt.version")));
        add(new Label("gxt.core", new MapModel(values, "gxt.core")));

        add(new Label("gxt.wps", new MapModel(values, "gxt.wps")));
        add(new Label("gxt.datadir", new MapModel(values, "gxt.datadir")));
        add(new Label("gxt.jai", new MapModel(values, "gxt.jai")));
        add(new Label("gxt.jai_imageio", new MapModel(values, "gxt.jai_imageio")));
    }

    private void updateModel() {
        values.put("gxt.address",
                "MangoSystem Inc. #2307, 126, Beolmal-ro, Dongan-gu, Anyang-si, Gyeonggi-do, 14057, Korea");
        values.put("gxt.tel", "Tel: 82-31-450-3411~3 | Fax: 82-31-450-3414");
        values.put("gxt.contact", "mango@mangosystem.com");

        Package versionInfo = lookupPackage("org.geotools.process.spatialstatistics");
        String version = versionInfo.getSpecificationVersion();
        values.put("gxt.version", String.format("%s", version));

        final String trueValue = "True";
        final String falseValue = "False";

        Boolean core = versionInfo != null;
        values.put("gxt.core", core ? trueValue : falseValue);

        // WPS enabled
        Boolean wpsEnabled = isInstalled("org.geoserver.wps.GetCapabilities");
        values.put("gxt.wps", wpsEnabled ? trueValue : falseValue);

        values.put("gxt.datadir", getDataDir());

        Boolean jai = isNativeJAIAvailable();
        if (jai) {
            values.put("gxt.jai", trueValue);
        } else {
            values.put("gxt.jai", "We recommend installing Native JAI!");
        }

        Boolean jaiCodec = PackageUtil.isCodecLibAvailable();
        if (jaiCodec) {
            values.put("gxt.jai_imageio", trueValue);
        } else {
            values.put("gxt.jai_imageio", "We recommend installing Native JAI ImageIO!");
        }
    }

    private Package lookupPackage(String packageName) {
        try {
            return Package.getPackage(packageName);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error looking up package:" + packageName, e);
        }
        return null;
    }

    private Boolean isInstalled(String classsName) {
        try {
            Class.forName(classsName);
            return Boolean.TRUE;
        } catch (Throwable e) {
            return Boolean.FALSE;
        }
    }

    private Boolean isNativeJAIAvailable() {
        try {
            Class<?> t = Class.forName("com.sun.medialib.mlib.Image");
            return (Boolean) t.getMethod("isAvailable").invoke(null);
        } catch (Throwable e) {
            return Boolean.FALSE;
        }
    }

    private String getDataDir() {
        GeoServerDataDirectory dd = getGeoServerApplication()
                .getBeanOfType(GeoServerDataDirectory.class);
        return dd.root().getAbsolutePath();
    }
}
