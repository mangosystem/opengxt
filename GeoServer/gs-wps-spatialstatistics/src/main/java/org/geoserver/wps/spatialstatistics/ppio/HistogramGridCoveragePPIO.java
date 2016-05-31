/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2016 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import java.io.InputStream;

import javax.xml.namespace.QName;

import org.geoserver.config.util.SecureXStream;
import org.geoserver.wps.ppio.XStreamPPIO;
import org.geotools.process.spatialstatistics.core.HistogramProcessResult;
import org.geotools.process.spatialstatistics.core.HistogramProcessResult.HistogramItem;
import org.xml.sax.ContentHandler;

import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.SaxWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/**
 * A PPIO to generate good looking xml for the histogram gridcoverage process results
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class HistogramGridCoveragePPIO extends XStreamPPIO {
    final XmlFriendlyNameCoder nameCoder = new XmlFriendlyNameCoder("__", "_");

    static final QName PPIO_NAME = new QName("http://www.opengis.net/statistics",
            "HistogramGridCoverage");

    protected HistogramGridCoveragePPIO() {
        super(HistogramProcessResult.class, PPIO_NAME);
    }

    @Override
    protected SecureXStream buildXStream() {
        SecureXStream xstream = new SecureXStream(new DomDriver("UTF-8", nameCoder)) {
            protected boolean useXStream11XmlFriendlyMapper() {
                return true;
            }

            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new UppercaseTagMapper(next);
            };
        };

        xstream.processAnnotations(HistogramProcessResult.class);
        xstream.processAnnotations(HistogramItem.class);

        xstream.alias("Histogram", HistogramProcessResult.class);
        xstream.alias("HistogramItem", HistogramItem.class);
        xstream.addImplicitCollection(HistogramProcessResult.class, "histogramItems");

        return xstream;
    }

    @Override
    public void encode(Object object, ContentHandler handler) throws Exception {
        // bind with the content handler
        SaxWriter writer = new SaxWriter(nameCoder);
        writer.setContentHandler(handler);

        // write out xml
        buildXStream().marshal(object, writer);
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return buildXStream().fromXML(input);
    }
}
